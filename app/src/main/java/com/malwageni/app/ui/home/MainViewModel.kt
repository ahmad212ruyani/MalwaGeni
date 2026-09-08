package com.malwageni.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malwageni.app.accessibility.AccessibilityUtils
import com.malwageni.app.data.CloudFirestoreRepository
import com.malwageni.app.data.LocalDataRepository
import com.malwageni.app.model.ProductItem
import com.malwageni.app.model.TransactionItem
import com.malwageni.app.model.TransactionType
import com.malwageni.app.network.ConnectivityObserver
import com.malwageni.app.network.ConnectivityStatus
import com.malwageni.app.network.NetworkConnectivityObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

enum class AppTab(val title: String, val a11yDescription: String) {
    POS("Kasir", "Tab 1 dari 4: Menu Kasir Penjualan Toko."),
    INVENTORY("Stok Barang", "Tab 2 dari 4: Menu Manajemen Persediaan dan Katalog Produk."),
    STORE_LEDGER("Keuangan Toko", "Tab 3 dari 4: Menu Laporan Keuangan Toko, Modal Usaha, Laba Kotor, dan Laba Bersih."),
    PERSONAL_LEDGER("Keuangan Pribadi", "Tab 4 dari 4: Menu Pencatatan Keuangan Pribadi Sehari-hari.")
}

data class CartItem(
    val product: ProductItem,
    val quantity: Int
)

data class MainUiState(
    val currentTab: AppTab = AppTab.POS,
    val networkStatus: ConnectivityStatus = ConnectivityStatus.AVAILABLE,
    val products: List<ProductItem> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val transactions: List<TransactionItem> = emptyList(),

    // Keuangan Toko (Store Finance & Profit/Loss)
    val storeInitialCapital: Double = 0.0,
    val storeRevenue: Double = 0.0,
    val storeExpense: Double = 0.0,
    val storeCogs: Double = 0.0,
    val storeGrossProfit: Double = 0.0,
    val storeNetProfit: Double = 0.0,
    val storeFinalBalance: Double = 0.0,
    val todayStoreRevenue: Double = 0.0,
    val todayStoreExpense: Double = 0.0,
    val todayStoreGrossProfit: Double = 0.0,
    val todayStoreNetProfit: Double = 0.0,

    // Keuangan Pribadi (Personal Finance)
    val personalInitialCapital: Double = 0.0,
    val personalIncome: Double = 0.0,
    val personalExpense: Double = 0.0,
    val personalBalance: Double = 0.0,
    val todayPersonalIncome: Double = 0.0,
    val todayPersonalExpense: Double = 0.0,
    val todayPersonalBalance: Double = 0.0,

    // Legacy backwards compatibility fields
    val totalRevenue: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val todayRevenue: Double = 0.0,
    val todayExpense: Double = 0.0,
    val todayNet: Double = 0.0,

    val isSyncing: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepo = LocalDataRepository(application)
    private val cloudRepo = CloudFirestoreRepository()
    private val connectivityObserver: ConnectivityObserver = NetworkConnectivityObserver(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _accessibilityEvents = MutableSharedFlow<String>()
    val accessibilityEvents: SharedFlow<String> = _accessibilityEvents.asSharedFlow()

    private var activeUserId: String = "guest_local"
    private var productSyncJob: Job? = null
    private var transactionSyncJob: Job? = null
    private var financeSettingsSyncJob: Job? = null

    init {
        // Load initial offline cached data
        val localProducts = localRepo.loadProducts()
        val localTransactions = localRepo.loadTransactions()
        val localStoreCapital = localRepo.loadStoreInitialCapital()
        val localPersonalCapital = localRepo.loadPersonalInitialCapital()
        _uiState.update {
            it.copy(
                products = localProducts,
                transactions = localTransactions,
                storeInitialCapital = localStoreCapital,
                personalInitialCapital = localPersonalCapital
            )
        }
        recalculateFinance()

        observeNetwork()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                _uiState.update { it.copy(networkStatus = status) }
                val announcement = if (status.isConnected) {
                    "Koneksi internet aktif. Terhubung ke Cloud Firestore Database."
                } else {
                    "Koneksi internet terputus. Mode offline diaktifkan, data disimpan di penyimpanan lokal."
                }
                announce(announcement)
            }
        }
    }

    /**
     * Initializes realtime cloud synchronization with Firebase Cloud Firestore for the logged-in user.
     */
    fun setUserSession(userId: String) {
        activeUserId = userId
        startRealtimeCloudSync(userId)
    }

    private fun startRealtimeCloudSync(userId: String) {
        productSyncJob?.cancel()
        transactionSyncJob?.cancel()
        financeSettingsSyncJob?.cancel()

        _uiState.update { it.copy(isSyncing = true) }

        // Realtime listener for Products in Firestore
        productSyncJob = viewModelScope.launch {
            try {
                cloudRepo.observeProducts(userId).collect { cloudProducts ->
                    _uiState.update { it.copy(products = cloudProducts, isSyncing = false) }
                    localRepo.saveProducts(cloudProducts)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }

        // Realtime listener for Transactions in Firestore
        transactionSyncJob = viewModelScope.launch {
            try {
                cloudRepo.observeTransactions(userId).collect { cloudTransactions ->
                    _uiState.update { it.copy(transactions = cloudTransactions) }
                    localRepo.saveTransactions(cloudTransactions)
                    recalculateFinance()
                }
            } catch (_: Exception) {}
        }

        // Realtime listener for Finance Settings (Modal/Saldo Awal) in Firestore
        financeSettingsSyncJob = viewModelScope.launch {
            try {
                cloudRepo.observeFinanceSettings(userId).collect { (storeCapital, personalCapital) ->
                    _uiState.update {
                        it.copy(
                            storeInitialCapital = storeCapital,
                            personalInitialCapital = personalCapital
                        )
                    }
                    localRepo.saveStoreInitialCapital(storeCapital)
                    localRepo.savePersonalInitialCapital(personalCapital)
                    recalculateFinance()
                }
            } catch (_: Exception) {}
        }
    }

    fun setStoreInitialCapital(amount: Double) {
        val nonNeg = amount.coerceAtLeast(0.0)
        _uiState.update { it.copy(storeInitialCapital = nonNeg) }
        localRepo.saveStoreInitialCapital(nonNeg)
        viewModelScope.launch {
            try {
                cloudRepo.updateStoreInitialCapital(activeUserId, nonNeg)
            } catch (_: Exception) {}
        }
        recalculateFinance()
        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(nonNeg)
        announce("Saldo awal modal usaha toko berhasil disimpan: $formatRp.")
    }

    fun setPersonalInitialCapital(amount: Double) {
        val nonNeg = amount.coerceAtLeast(0.0)
        _uiState.update { it.copy(personalInitialCapital = nonNeg) }
        localRepo.savePersonalInitialCapital(nonNeg)
        viewModelScope.launch {
            try {
                cloudRepo.updatePersonalInitialCapital(activeUserId, nonNeg)
            } catch (_: Exception) {}
        }
        recalculateFinance()
        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(nonNeg)
        announce("Saldo awal keuangan pribadi berhasil disimpan: $formatRp.")
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
        announce("Membuka ${tab.title}. ${tab.a11yDescription}")
    }

    // =========================================================================
    // CRUD: CREATE / TAMBAH PRODUK
    // =========================================================================

    fun addNewProduct(name: String, sku: String, costPrice: Double, sellPrice: Double, initialStock: Int) {
        if (name.isBlank()) {
            announce("Nama produk tidak boleh kosong.")
            return
        }

        val newProduct = ProductItem(
            id = "prod_" + System.currentTimeMillis(),
            name = name.trim(),
            sku = if (sku.isBlank()) "-" else sku.trim(),
            costPrice = costPrice.coerceAtLeast(0.0),
            sellPrice = sellPrice.coerceAtLeast(0.0),
            stockQuantity = initialStock.coerceAtLeast(0)
        )

        val updatedProducts = _uiState.value.products + newProduct
        _uiState.update { it.copy(products = updatedProducts) }
        localRepo.saveProducts(updatedProducts)

        viewModelScope.launch {
            try {
                cloudRepo.addProduct(activeUserId, newProduct)
            } catch (_: Exception) {}
        }

        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(sellPrice)
        announce("Produk ${newProduct.name} berhasil disimpan ke database cloud. Harga $formatRp, stok $initialStock unit.")
    }

    // =========================================================================
    // CRUD: UPDATE / EDIT PRODUK
    // =========================================================================

    fun editProduct(productId: String, name: String, sku: String, costPrice: Double, sellPrice: Double, stock: Int) {
        val updatedProduct = ProductItem(
            id = productId,
            name = name.trim(),
            sku = if (sku.isBlank()) "-" else sku.trim(),
            costPrice = costPrice.coerceAtLeast(0.0),
            sellPrice = sellPrice.coerceAtLeast(0.0),
            stockQuantity = stock.coerceAtLeast(0)
        )

        val updatedList = _uiState.value.products.map { if (it.id == productId) updatedProduct else it }
        _uiState.update { it.copy(products = updatedList) }
        localRepo.saveProducts(updatedList)

        viewModelScope.launch {
            try {
                cloudRepo.updateProduct(activeUserId, updatedProduct)
            } catch (_: Exception) {}
        }

        announce("Perubahan data produk ${updatedProduct.name} berhasil disimpan ke database cloud.")
    }

    fun restockProduct(productId: String, amountToAdd: Int = 10) {
        var productName = ""
        var newStock = 0

        val updated = _uiState.value.products.map { prod ->
            if (prod.id == productId) {
                productName = prod.name
                newStock = prod.stockQuantity + amountToAdd
                prod.copy(stockQuantity = newStock)
            } else {
                prod
            }
        }

        _uiState.update { it.copy(products = updated) }
        localRepo.saveProducts(updated)

        viewModelScope.launch {
            try {
                cloudRepo.updateProductStock(activeUserId, productId, newStock)
            } catch (_: Exception) {}
        }

        announce("Restock berhasil. Stok $productName bertambah $amountToAdd, kini menjadi $newStock unit di cloud database.")
    }

    // =========================================================================
    // CRUD: DELETE / HAPUS PRODUK
    // =========================================================================

    fun deleteProduct(productId: String) {
        val target = _uiState.value.products.find { it.id == productId }
        val updatedProducts = _uiState.value.products.filterNot { it.id == productId }
        val updatedCart = _uiState.value.cart.filterNot { it.product.id == productId }

        _uiState.update { it.copy(products = updatedProducts, cart = updatedCart) }
        localRepo.saveProducts(updatedProducts)

        viewModelScope.launch {
            try {
                cloudRepo.deleteProduct(activeUserId, productId)
            } catch (_: Exception) {}
        }

        if (target != null) {
            announce("Produk ${target.name} telah dihapus dari database cloud.")
        }
    }

    // =========================================================================
    // POS (KASIR) OPERATIONS
    // =========================================================================

    fun addToCart(product: ProductItem) {
        if (product.stockQuantity <= 0) {
            announce("Maaf, stok untuk ${product.name} telah habis.")
            return
        }

        _uiState.update { state ->
            val existingItem = state.cart.find { it.product.id == product.id }
            val updatedCart = if (existingItem != null) {
                state.cart.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                state.cart + CartItem(product = product, quantity = 1)
            }
            state.copy(cart = updatedCart)
        }

        val totalItems = _uiState.value.cart.sumOf { it.quantity }
        val cartTotal = _uiState.value.cart.sumOf { it.product.sellPrice * it.quantity }
        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(cartTotal)
        announce("${product.name} dimasukkan ke keranjang kasir. Total $totalItems item, $formatRp.")
    }

    fun clearCart() {
        _uiState.update { it.copy(cart = emptyList()) }
        announce("Keranjang belanja kasir telah dikosongkan.")
    }

    fun checkoutCart() {
        val currentCart = _uiState.value.cart
        if (currentCart.isEmpty()) {
            announce("Keranjang kasir masih kosong. Pilih produk terlebih dahulu.")
            return
        }

        val cartTotal = currentCart.sumOf { it.product.sellPrice * it.quantity }
        val cartCostTotal = currentCart.sumOf { it.product.costPrice * it.quantity }
        val grossProfit = cartTotal - cartCostTotal
        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(cartTotal)
        val formatProfit = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(grossProfit)

        val updatedProducts = _uiState.value.products.map { prod ->
            val bought = currentCart.find { it.product.id == prod.id }
            if (bought != null) prod.copy(stockQuantity = (prod.stockQuantity - bought.quantity).coerceAtLeast(0))
            else prod
        }

        val newTransaction = TransactionItem(
            id = "tx_" + System.currentTimeMillis(),
            description = "Penjualan Kasir (${currentCart.sumOf { it.quantity }} barang)",
            amount = cartTotal,
            type = TransactionType.SALE,
            category = "Penjualan Toko",
            wallet = "Kas Toko",
            costAmount = cartCostTotal,
            isPersonal = false
        )

        val updatedTransactions = listOf(newTransaction) + _uiState.value.transactions

        _uiState.update { state ->
            state.copy(
                products = updatedProducts,
                transactions = updatedTransactions,
                cart = emptyList()
            )
        }

        localRepo.saveProducts(updatedProducts)
        localRepo.saveTransactions(updatedTransactions)

        viewModelScope.launch {
            try {
                // Sync stock changes to Cloud Firestore
                currentCart.forEach { cartItem ->
                    val newStock = (cartItem.product.stockQuantity - cartItem.quantity).coerceAtLeast(0)
                    cloudRepo.updateProductStock(activeUserId, cartItem.product.id, newStock)
                }
                cloudRepo.addTransaction(activeUserId, newTransaction)
            } catch (_: Exception) {}
        }

        recalculateFinance()
        announce("Pembayaran kasir berhasil: $formatRp. Keuntungan kotor: $formatProfit. Data otomatis tersimpan ke cloud.")
    }

    // =========================================================================
    // CRUD: TRANSAKSI KEUANGAN TOKO & PRIBADI (CREATE, EDIT, DELETE)
    // =========================================================================

    fun addManualTransaction(
        description: String,
        amount: Double,
        type: TransactionType,
        category: String = "Umum",
        wallet: String = "Tunai",
        isPersonal: Boolean = false
    ) {
        if (description.isBlank() || amount <= 0.0) {
            announce("Keterangan dan nominal transaksi tidak boleh kosong.")
            return
        }

        val newTx = TransactionItem(
            id = "tx_" + System.currentTimeMillis(),
            description = description.trim(),
            amount = amount,
            type = type,
            category = category.trim(),
            wallet = wallet.trim(),
            costAmount = 0.0,
            isPersonal = isPersonal
        )

        val updatedTxs = listOf(newTx) + _uiState.value.transactions
        _uiState.update { it.copy(transactions = updatedTxs) }
        localRepo.saveTransactions(updatedTxs)

        viewModelScope.launch {
            try {
                cloudRepo.addTransaction(activeUserId, newTx)
            } catch (_: Exception) {}
        }

        recalculateFinance()

        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount)
        val ledgerName = if (isPersonal) "keuangan pribadi" else "keuangan toko"
        announce("Transaksi $ledgerName $description ($category) senilai $formatRp berhasil disimpan ke database cloud.")
    }

    fun editTransaction(
        transactionId: String,
        description: String,
        amount: Double,
        type: TransactionType,
        category: String,
        wallet: String,
        costAmount: Double = 0.0,
        isPersonal: Boolean = false
    ) {
        val target = _uiState.value.transactions.find { it.id == transactionId } ?: return
        val updatedTx = target.copy(
            description = description.trim(),
            amount = amount,
            type = type,
            category = category.trim(),
            wallet = wallet.trim(),
            costAmount = costAmount,
            isPersonal = isPersonal
        )

        val updatedList = _uiState.value.transactions.map { if (it.id == transactionId) updatedTx else it }
        _uiState.update { it.copy(transactions = updatedList) }
        localRepo.saveTransactions(updatedList)

        viewModelScope.launch {
            try {
                cloudRepo.updateTransaction(activeUserId, updatedTx)
            } catch (_: Exception) {}
        }

        recalculateFinance()
        announce("Perubahan catatan transaksi ${updatedTx.description} berhasil diperbarui di database cloud.")
    }

    fun deleteTransaction(transactionId: String) {
        val target = _uiState.value.transactions.find { it.id == transactionId }
        val updated = _uiState.value.transactions.filterNot { it.id == transactionId }

        _uiState.update { it.copy(transactions = updated) }
        localRepo.saveTransactions(updated)

        viewModelScope.launch {
            try {
                cloudRepo.deleteTransaction(activeUserId, transactionId)
            } catch (_: Exception) {}
        }

        recalculateFinance()

        if (target != null) {
            val ledgerName = if (target.isPersonal) "keuangan pribadi" else "keuangan toko"
            announce("Transaksi $ledgerName ${target.description} telah dihapus dari database cloud.")
        }
    }

    private fun recalculateFinance() {
        val txs = _uiState.value.transactions
        val storeCapital = _uiState.value.storeInitialCapital
        val personalCapital = _uiState.value.personalInitialCapital

        // --- Store Finance (isPersonal == false) ---
        val storeTxs = txs.filter { !it.isPersonal }
        val storeRev = storeTxs.filter { it.type == TransactionType.INCOME || it.type == TransactionType.SALE }.sumOf { it.amount }
        val storeExp = storeTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val storeCogs = storeTxs.filter { it.type == TransactionType.SALE }.sumOf { it.costAmount }
        val storeGross = storeRev - storeCogs
        val storeNet = storeGross - storeExp
        val storeBalance = storeCapital + storeRev - storeExp

        val todayStoreTxs = storeTxs.filter { it.isToday() }
        val todayStoreRev = todayStoreTxs.filter { it.type == TransactionType.INCOME || it.type == TransactionType.SALE }.sumOf { it.amount }
        val todayStoreExp = todayStoreTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val todayStoreCogs = todayStoreTxs.filter { it.type == TransactionType.SALE }.sumOf { it.costAmount }
        val todayStoreGross = todayStoreRev - todayStoreCogs
        val todayStoreNet = todayStoreGross - todayStoreExp

        // --- Personal Finance (isPersonal == true) ---
        val personalTxs = txs.filter { it.isPersonal }
        val personalInc = personalTxs.filter { it.type == TransactionType.INCOME || it.type == TransactionType.SALE }.sumOf { it.amount }
        val personalExp = personalTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val personalBal = personalCapital + personalInc - personalExp

        val todayPersonalTxs = personalTxs.filter { it.isToday() }
        val todayPersonalInc = todayPersonalTxs.filter { it.type == TransactionType.INCOME || it.type == TransactionType.SALE }.sumOf { it.amount }
        val todayPersonalExp = todayPersonalTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val todayPersonalBal = todayPersonalInc - todayPersonalExp

        _uiState.update {
            it.copy(
                storeRevenue = storeRev,
                storeExpense = storeExp,
                storeCogs = storeCogs,
                storeGrossProfit = storeGross,
                storeNetProfit = storeNet,
                storeFinalBalance = storeBalance,
                todayStoreRevenue = todayStoreRev,
                todayStoreExpense = todayStoreExp,
                todayStoreGrossProfit = todayStoreGross,
                todayStoreNetProfit = todayStoreNet,

                personalIncome = personalInc,
                personalExpense = personalExp,
                personalBalance = personalBal,
                todayPersonalIncome = todayPersonalInc,
                todayPersonalExpense = todayPersonalExp,
                todayPersonalBalance = todayPersonalBal,

                // Legacy backwards compatibility
                totalRevenue = storeRev,
                totalExpense = storeExp,
                netBalance = storeBalance,
                todayRevenue = todayStoreRev,
                todayExpense = todayStoreExp,
                todayNet = todayStoreNet
            )
        }
    }

    private fun announce(message: String) {
        viewModelScope.launch {
            _accessibilityEvents.emit(message)
            AccessibilityUtils.announceForAccessibility(getApplication(), message)
        }
    }
}
