package com.malwageni.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malwageni.app.accessibility.AccessibilityUtils
import com.malwageni.app.data.LocalDataRepository
import com.malwageni.app.model.ProductItem
import com.malwageni.app.model.TransactionItem
import com.malwageni.app.model.TransactionType
import com.malwageni.app.network.ConnectivityObserver
import com.malwageni.app.network.ConnectivityStatus
import com.malwageni.app.network.NetworkConnectivityObserver
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
    POS("Kasir", "Menu Kasir. Kelola transaksi dan penjualan."),
    INVENTORY("Stok Barang", "Menu Stok Barang. Pantau persediaan dan tambah produk."),
    LEDGER("Buku Keuangan", "Menu Buku Keuangan Pribadi dan Usaha.")
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
    val totalRevenue: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalDataRepository(application)
    private val connectivityObserver: ConnectivityObserver = NetworkConnectivityObserver(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _accessibilityEvents = MutableSharedFlow<String>()
    val accessibilityEvents: SharedFlow<String> = _accessibilityEvents.asSharedFlow()

    init {
        loadPersistedData()
        observeNetwork()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                _uiState.update { it.copy(networkStatus = status) }
                val announcement = if (status.isConnected) {
                    "Koneksi internet aktif."
                } else {
                    "Koneksi internet terputus. Menggunakan database lokal."
                }
                announce(announcement)
            }
        }
    }

    /**
     * Loads clean data directly from the local database.
     * Starts with 0 products and 0 transactions if user hasn't added any yet!
     */
    private fun loadPersistedData() {
        val savedProducts = repository.loadProducts()
        val savedTransactions = repository.loadTransactions()

        _uiState.update { state ->
            state.copy(
                products = savedProducts,
                transactions = savedTransactions
            )
        }
        recalculateFinance()
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
        announce("Membuka ${tab.title}. ${tab.a11yDescription}")
    }

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
        repository.saveProducts(updatedProducts)
        _uiState.update { it.copy(products = updatedProducts) }

        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(sellPrice)
        announce("Produk ${newProduct.name} berhasil disimpan ke database. Harga $formatRp, stok $initialStock unit.")
    }

    fun deleteProduct(productId: String) {
        val target = _uiState.value.products.find { it.id == productId }
        val updatedProducts = _uiState.value.products.filterNot { it.id == productId }
        val updatedCart = _uiState.value.cart.filterNot { it.product.id == productId }

        repository.saveProducts(updatedProducts)
        _uiState.update { it.copy(products = updatedProducts, cart = updatedCart) }

        if (target != null) {
            announce("Produk ${target.name} telah dihapus dari database.")
        }
    }

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
        announce("${product.name} ditambahkan ke kasir. Keranjang berisi $totalItems item, total $formatRp.")
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
        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(cartTotal)

        // Decrease stock & record sale transaction
        val updatedProducts = _uiState.value.products.map { prod ->
            val bought = currentCart.find { it.product.id == prod.id }
            if (bought != null) prod.copy(stockQuantity = (prod.stockQuantity - bought.quantity).coerceAtLeast(0))
            else prod
        }

        val newTransaction = TransactionItem(
            id = "tx_" + System.currentTimeMillis(),
            description = "Penjualan Kasir (${currentCart.size} jenis barang)",
            amount = cartTotal,
            type = TransactionType.SALE
        )

        val updatedTransactions = listOf(newTransaction) + _uiState.value.transactions

        repository.saveProducts(updatedProducts)
        repository.saveTransactions(updatedTransactions)

        _uiState.update { state ->
            state.copy(
                products = updatedProducts,
                transactions = updatedTransactions,
                cart = emptyList()
            )
        }

        recalculateFinance()
        announce("Pembayaran berhasil senilai $formatRp. Data kasir dan stok otomatis disimpan ke database.")
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

        repository.saveProducts(updated)
        _uiState.update { it.copy(products = updated) }
        announce("Restock berhasil. Stok $productName kini menjadi $newStock unit.")
    }

    fun addManualTransaction(description: String, amount: Double, type: TransactionType) {
        if (description.isBlank() || amount <= 0.0) {
            announce("Keterangan dan nominal transaksi tidak boleh kosong.")
            return
        }

        val newTx = TransactionItem(
            id = "tx_" + System.currentTimeMillis(),
            description = description.trim(),
            amount = amount,
            type = type
        )

        val updatedTxs = listOf(newTx) + _uiState.value.transactions
        repository.saveTransactions(updatedTxs)

        _uiState.update { it.copy(transactions = updatedTxs) }
        recalculateFinance()

        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount)
        announce("Transaksi baru disimpan ke database: $description, senilai $formatRp.")
    }

    private fun recalculateFinance() {
        val txs = _uiState.value.transactions
        val revenue = txs.filter { it.type == TransactionType.INCOME || it.type == TransactionType.SALE }.sumOf { it.amount }
        val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val net = revenue - expense

        _uiState.update {
            it.copy(
                totalRevenue = revenue,
                totalExpense = expense,
                netBalance = net
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
