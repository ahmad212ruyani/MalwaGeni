package com.malwageni.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malwageni.app.accessibility.AccessibilityUtils
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

    private val connectivityObserver: ConnectivityObserver = NetworkConnectivityObserver(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _accessibilityEvents = MutableSharedFlow<String>()
    val accessibilityEvents: SharedFlow<String> = _accessibilityEvents.asSharedFlow()

    init {
        loadInitialData()
        observeNetwork()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                _uiState.update { it.copy(networkStatus = status) }
                val announcement = if (status.isConnected) {
                    "Koneksi internet aktif. Sinkronisasi data cloud siap."
                } else {
                    "Koneksi internet terputus. Mode offline diaktifkan."
                }
                announce(announcement)
            }
        }
    }

    private fun loadInitialData() {
        val initialProducts = listOf(
            ProductItem("1", "Kopi Robusta 250g", "8991001", 18000.0, 25000.0, 15),
            ProductItem("2", "Gula Pasir 1kg", "8991002", 14000.0, 17500.0, 24),
            ProductItem("3", "Susu Segar UHT 1L", "8991003", 16000.0, 21000.0, 4), // Low stock
            ProductItem("4", "Teh Celup Kotak", "8991004", 6000.0, 9000.0, 30)
        )

        val initialTransactions = listOf(
            TransactionItem("t1", "Modal Kas Awal Toko", 500000.0, TransactionType.INCOME),
            TransactionItem("t2", "Beli ATK & Nota Kasir", 35000.0, TransactionType.EXPENSE),
            TransactionItem("t3", "Penjualan 2 Kopi Robusta", 50000.0, TransactionType.SALE)
        )

        _uiState.update { state ->
            state.copy(
                products = initialProducts,
                transactions = initialTransactions
            )
        }
        recalculateFinance()
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
        announce("Membuka ${tab.title}. ${tab.a11yDescription}")
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
        announce("Keranjang belanja telah dikosongkan.")
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
        _uiState.update { state ->
            val updatedProducts = state.products.map { prod ->
                val bought = currentCart.find { it.product.id == prod.id }
                if (bought != null) prod.copy(stockQuantity = (prod.stockQuantity - bought.quantity).coerceAtLeast(0))
                else prod
            }

            val newTransaction = TransactionItem(
                id = System.currentTimeMillis().toString(),
                description = "Penjualan POS (${currentCart.size} jenis produk)",
                amount = cartTotal,
                type = TransactionType.SALE
            )

            state.copy(
                products = updatedProducts,
                transactions = listOf(newTransaction) + state.transactions,
                cart = emptyList()
            )
        }

        recalculateFinance()
        announce("Pembayaran berhasil diselesaikan senilai $formatRp. Stok otomatis diperbarui dan transaksi dicatat.")
    }

    fun restockProduct(productId: String, amountToAdd: Int = 10) {
        var productName = ""
        var newStock = 0
        _uiState.update { state ->
            val updated = state.products.map { prod ->
                if (prod.id == productId) {
                    productName = prod.name
                    newStock = prod.stockQuantity + amountToAdd
                    prod.copy(stockQuantity = newStock)
                } else {
                    prod
                }
            }
            state.copy(products = updated)
        }
        announce("Restock berhasil. Stok $productName bertambah $amountToAdd, kini menjadi $newStock unit.")
    }

    fun addManualTransaction(description: String, amount: Double, type: TransactionType) {
        val newTx = TransactionItem(
            id = System.currentTimeMillis().toString(),
            description = description,
            amount = amount,
            type = type
        )
        _uiState.update { it.copy(transactions = listOf(newTx) + it.transactions) }
        recalculateFinance()
        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount)
        announce("Catatan keuangan baru berhasil disimpan: $description, senilai $formatRp.")
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
