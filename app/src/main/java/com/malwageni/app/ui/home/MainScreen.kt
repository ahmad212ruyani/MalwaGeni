package com.malwageni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.malwageni.app.model.ProductItem
import com.malwageni.app.model.TransactionItem
import com.malwageni.app.model.TransactionType
import com.malwageni.app.model.UserAccount
import com.malwageni.app.ui.components.AccessibleActionButton
import com.malwageni.app.ui.components.AccessibleNetworkStatusBar
import com.malwageni.app.ui.components.AccessibleProductCard
import com.malwageni.app.ui.theme.ExpenseRed
import com.malwageni.app.ui.theme.IncomeGreen
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    currentUser: UserAccount?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductItem?>(null) }

    var showStoreCapitalDialog by remember { mutableStateOf(false) }
    var showPersonalCapitalDialog by remember { mutableStateOf(false) }

    var showAddStoreTxDialog by remember { mutableStateOf(false) }
    var showAddPersonalTxDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.accessibilityEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Modal Create Product
    if (showAddProductDialog) {
        ProductFormDialog(
            title = "Tambah Produk Baru",
            initialName = "",
            initialSku = "",
            initialCostPrice = 0.0,
            initialSellPrice = 0.0,
            initialStock = 0,
            onDismiss = { showAddProductDialog = false },
            onSave = { name, sku, cost, sell, stock ->
                viewModel.addNewProduct(name, sku, cost, sell, stock)
                showAddProductDialog = false
            }
        )
    }

    // Modal Update (Edit) Product
    if (productToEdit != null) {
        val p = productToEdit!!
        ProductFormDialog(
            title = "Edit Produk: ${p.name}",
            initialName = p.name,
            initialSku = p.sku,
            initialCostPrice = p.costPrice,
            initialSellPrice = p.sellPrice,
            initialStock = p.stockQuantity,
            onDismiss = { productToEdit = null },
            onSave = { name, sku, cost, sell, stock ->
                viewModel.editProduct(p.id, name, sku, cost, sell, stock)
                productToEdit = null
            }
        )
    }

    // Modal Saldo Awal / Modal Toko
    if (showStoreCapitalDialog) {
        CapitalFormDialog(
            title = "Atur Modal Awal Toko",
            initialAmount = uiState.storeInitialCapital,
            onDismiss = { showStoreCapitalDialog = false },
            onSave = { amount ->
                viewModel.setStoreInitialCapital(amount)
                showStoreCapitalDialog = false
            }
        )
    }

    // Modal Saldo Awal Keuangan Pribadi
    if (showPersonalCapitalDialog) {
        CapitalFormDialog(
            title = "Atur Saldo Awal Pribadi",
            initialAmount = uiState.personalInitialCapital,
            onDismiss = { showPersonalCapitalDialog = false },
            onSave = { amount ->
                viewModel.setPersonalInitialCapital(amount)
                showPersonalCapitalDialog = false
            }
        )
    }

    // Modal Create Store Transaction (Beban / Pemasukan Toko)
    if (showAddStoreTxDialog) {
        DailyTransactionFormDialog(
            title = "Catat Transaksi Toko",
            initialDescription = "",
            initialAmount = 0.0,
            initialType = TransactionType.EXPENSE,
            initialCategory = "Operasional Toko",
            initialWallet = "Kas Toko",
            isPersonal = false,
            onDismiss = { showAddStoreTxDialog = false },
            onSave = { desc, amount, type, category, wallet ->
                viewModel.addManualTransaction(desc, amount, type, category, wallet, isPersonal = false)
                showAddStoreTxDialog = false
            }
        )
    }

    // Modal Create Personal Transaction (Pemasukan / Pengeluaran Pribadi)
    if (showAddPersonalTxDialog) {
        DailyTransactionFormDialog(
            title = "Catat Keuangan Pribadi",
            initialDescription = "",
            initialAmount = 0.0,
            initialType = TransactionType.EXPENSE,
            initialCategory = "Makanan & Minuman",
            initialWallet = "Tunai",
            isPersonal = true,
            onDismiss = { showAddPersonalTxDialog = false },
            onSave = { desc, amount, type, category, wallet ->
                viewModel.addManualTransaction(desc, amount, type, category, wallet, isPersonal = true)
                showAddPersonalTxDialog = false
            }
        )
    }

    // Modal Edit Transaction (Toko atau Pribadi)
    if (transactionToEdit != null) {
        val tx = transactionToEdit!!
        val isPersonalTx = tx.isPersonal
        val dialogTitle = if (isPersonalTx) "Edit Catatan Pribadi: ${tx.description}" else "Edit Catatan Toko: ${tx.description}"
        DailyTransactionFormDialog(
            title = dialogTitle,
            initialDescription = tx.description,
            initialAmount = tx.amount,
            initialType = tx.type,
            initialCategory = tx.category,
            initialWallet = tx.wallet,
            isPersonal = isPersonalTx,
            onDismiss = { transactionToEdit = null },
            onSave = { desc, amount, type, category, wallet ->
                viewModel.editTransaction(tx.id, desc, amount, type, category, wallet, tx.costAmount, isPersonal = isPersonalTx)
                transactionToEdit = null
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MalwaGeni",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (currentUser != null) {
                            Text(
                                text = "${currentUser.displayName} (Online Cloud)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    if (currentUser != null) {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Tombol Keluar dari akun ${currentUser.displayName}"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.semantics {
                    val userDesc = currentUser?.getAccessibilityDescription() ?: ""
                    contentDescription = "Aplikasi MalwaGeni. Terhubung ke Cloud Firestore. $userDesc"
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AccessibleNetworkStatusBar(status = uiState.networkStatus)

            AccessibleTabBar(
                selectedTab = uiState.currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            HorizontalDivider()

            when (uiState.currentTab) {
                AppTab.POS -> PosTabContent(
                    uiState = uiState,
                    onAddToCart = { viewModel.addToCart(it) },
                    onClearCart = { viewModel.clearCart() },
                    onCheckout = { viewModel.checkoutCart() },
                    onNavigateToInventory = { viewModel.selectTab(AppTab.INVENTORY) }
                )
                AppTab.INVENTORY -> InventoryTabContent(
                    uiState = uiState,
                    onRestock = { viewModel.restockProduct(it) },
                    onEdit = { productToEdit = it },
                    onDelete = { viewModel.deleteProduct(it) },
                    onOpenAddProduct = { showAddProductDialog = true }
                )
                AppTab.STORE_LEDGER -> StoreLedgerTabContent(
                    uiState = uiState,
                    onOpenAddTransaction = { showAddStoreTxDialog = true },
                    onOpenSetCapital = { showStoreCapitalDialog = true },
                    onEditTransaction = { transactionToEdit = it },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                )
                AppTab.PERSONAL_LEDGER -> PersonalLedgerTabContent(
                    uiState = uiState,
                    onOpenAddTransaction = { showAddPersonalTxDialog = true },
                    onOpenSetCapital = { showPersonalCapitalDialog = true },
                    onEditTransaction = { transactionToEdit = it },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                )
            }
        }
    }
}

@Composable
fun AccessibleTabBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Start
    ) {
        AppTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            val activeColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp, minWidth = 84.dp)
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                        contentDescription = if (isSelected) "${tab.title}, sedang aktif. ${tab.a11yDescription}"
                        else "${tab.title}. Ketuk dua kali untuk beralih."
                    }
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(36.dp)
                            .background(activeColor)
                    )
                }
            }
        }
    }
}

@Composable
fun PosTabContent(
    uiState: MainUiState,
    onAddToCart: (ProductItem) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    onNavigateToInventory: () -> Unit
) {
    val cartCount = uiState.cart.sumOf { it.quantity }
    val cartTotal = uiState.cart.sumOf { it.product.sellPrice * it.quantity }
    val formattedTotal = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(cartTotal)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Ringkasan Kasir. Total keranjang: $cartCount item. Tagihan: $formattedTotal."
                        liveRegion = LiveRegionMode.Polite
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Keranjang Belanja Kasir",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$cartCount Barang",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formattedTotal,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccessibleActionButton(
                            text = "Kosongkan",
                            contentDescription = "Kosongkan keranjang kasir",
                            onClick = onClearCart,
                            icon = Icons.Default.Delete,
                            backgroundColor = ExpenseRed,
                            modifier = Modifier.weight(1f)
                        )
                        AccessibleActionButton(
                            text = "Bayar",
                            contentDescription = "Lanjutkan proses pembayaran senilai $formattedTotal",
                            onClick = onCheckout,
                            icon = Icons.Default.Check,
                            backgroundColor = IncomeGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Pilih Produk untuk Dijual",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (uiState.products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Katalog kasir masih kosong. Silakan buka menu Stok Barang untuk menambahkan produk pertama Anda."
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Produk di Kasir",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Katalog cloud Anda masih kosong. Buka tab Stok Barang untuk menambah produk toko Anda sendiri.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AccessibleActionButton(
                            text = "Buka Menu Stok Barang",
                            contentDescription = "Buka menu Stok Barang untuk menambah produk baru",
                            onClick = onNavigateToInventory
                        )
                    }
                }
            }
        } else {
            items(uiState.products) { product ->
                AccessibleProductCard(
                    product = product,
                    onClick = { onAddToCart(product) }
                )
            }
        }
    }
}

@Composable
fun InventoryTabContent(
    uiState: MainUiState,
    onRestock: (String) -> Unit,
    onEdit: (ProductItem) -> Unit,
    onDelete: (String) -> Unit,
    onOpenAddProduct: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Katalog & Manajemen Stok (Cloud Database)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Total ${uiState.products.size} produk tersinkronisasi di server online.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            AccessibleActionButton(
                text = "+ Tambah Produk Baru",
                contentDescription = "Tombol Tambah Produk Baru ke Cloud Database.",
                onClick = onOpenAddProduct,
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Gudang stok online masih kosong. Ketuk tombol Tambah Produk Baru di atas untuk membuat produk Anda."
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Database Online Bersih & Kosong",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tidak ada produk dummy. Anda dapat membuat, mengubah (edit), dan menghapus produk Anda sendiri.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.products) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = product.getAccessibilityDescription()
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Barcode: ${product.sku} | Harga Jual: ${product.formattedSellPrice()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Sisa Stok: ${product.stockQuantity} unit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (product.stockQuantity <= 5) ExpenseRed else IncomeGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AccessibleActionButton(
                                text = "Edit",
                                contentDescription = "Edit data produk ${product.name}",
                                onClick = { onEdit(product) },
                                icon = Icons.Default.Edit,
                                backgroundColor = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AccessibleActionButton(
                                text = "+10",
                                contentDescription = "Tambah 10 unit stok untuk ${product.name}",
                                onClick = { onRestock(product.id) },
                                icon = Icons.Default.Add
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AccessibleActionButton(
                                text = "Hapus",
                                contentDescription = "Hapus produk ${product.name} dari database cloud",
                                onClick = { onDelete(product.id) },
                                icon = Icons.Default.Delete,
                                backgroundColor = ExpenseRed
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Store Financial Management Tab (Keuangan Toko).
 * Displays Saldo Awal/Modal Toko, Pemasukan Toko, Pengeluaran Toko, Keuntungan Kotor, Keuntungan Bersih, and Daily Cashflow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreLedgerTabContent(
    uiState: MainUiState,
    onOpenAddTransaction: () -> Unit,
    onOpenSetCapital: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    val formatRp = { amount: Double ->
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount)
    }

    var selectedFilter by remember { mutableStateOf("Semua") }

    val storeTransactions = uiState.transactions.filter { !it.isPersonal }
    val filteredTransactions = when (selectedFilter) {
        "Hari Ini" -> storeTransactions.filter { it.isToday() }
        "Penjualan" -> storeTransactions.filter { it.type == TransactionType.SALE }
        "Pengeluaran" -> storeTransactions.filter { it.type == TransactionType.EXPENSE }
        "Pemasukan" -> storeTransactions.filter { it.type == TransactionType.INCOME }
        else -> storeTransactions
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Saldo Awal / Modal Toko Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Modal atau Saldo Awal Toko: ${formatRp(uiState.storeInitialCapital)}. Ketuk tombol atur untuk mengubah."
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modal Awal Toko",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatRp(uiState.storeInitialCapital),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    TextButton(
                        onClick = onOpenSetCapital,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Tombol atur modal awal toko"
                            }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atur Modal")
                    }
                }
            }
        }

        // 2. HIGHLIGHT UTAMA: Keuntungan Kotor & Keuntungan Bersih Toko
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Laporan Laba Usaha Toko. Keuntungan kotor: ${formatRp(uiState.storeGrossProfit)}, dari omset penjualan ${formatRp(uiState.storeRevenue)} dikurangi modal pokok barang terjual ${formatRp(uiState.storeCogs)}. Keuntungan bersih: ${formatRp(uiState.storeNetProfit)}, setelah dipotong biaya operasional toko ${formatRp(uiState.storeExpense)}."
                        liveRegion = LiveRegionMode.Polite
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.storeNetProfit >= 0) IncomeGreen.copy(alpha = 0.14f) else ExpenseRed.copy(alpha = 0.14f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Laporan Keuntungan Usaha Toko",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (uiState.storeNetProfit >= 0) IncomeGreen else ExpenseRed,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (uiState.storeNetProfit >= 0) "Profit" else "Defisit",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Keuntungan Kotor (Gross Profit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Keuntungan Kotor (Gross Profit)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Omset ${formatRp(uiState.storeRevenue)} - Modal Barang ${formatRp(uiState.storeCogs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatRp(uiState.storeGrossProfit),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (uiState.storeGrossProfit >= 0) IncomeGreen else ExpenseRed
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Keuntungan Bersih (Net Profit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Keuntungan Bersih (Net Profit)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Laba Kotor - Biaya Operasional ${formatRp(uiState.storeExpense)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatRp(uiState.storeNetProfit),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (uiState.storeNetProfit >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }

        // 3. Arus Kas Toko & Saldo Kas Akhir
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Arus Kas Toko. Total pemasukan kas: ${formatRp(uiState.storeRevenue)}. Total pengeluaran toko: ${formatRp(uiState.storeExpense)}. Saldo kas toko saat ini: ${formatRp(uiState.storeFinalBalance)}."
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Arus Kas & Saldo Toko",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pemasukan Toko:", style = MaterialTheme.typography.bodyMedium)
                        Text(formatRp(uiState.storeRevenue), style = MaterialTheme.typography.titleSmall, color = IncomeGreen)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pengeluaran Toko:", style = MaterialTheme.typography.bodyMedium)
                        Text(formatRp(uiState.storeExpense), style = MaterialTheme.typography.titleSmall, color = ExpenseRed)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Saldo Kas Toko Saat Ini:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatRp(uiState.storeFinalBalance),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (uiState.storeFinalBalance >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
                        )
                    }
                }
            }
        }

        // 4. Hari Ini (Daily Performance Toko)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Performa Toko Hari Ini. Penjualan: ${formatRp(uiState.todayStoreRevenue)}. Beban toko: ${formatRp(uiState.todayStoreExpense)}. Keuntungan bersih hari ini: ${formatRp(uiState.todayStoreNetProfit)}."
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Performa Toko Hari Ini", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Omset Hari Ini: ${formatRp(uiState.todayStoreRevenue)}", style = MaterialTheme.typography.bodySmall)
                        Text("Beban Hari Ini: ${formatRp(uiState.todayStoreExpense)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Laba Kotor Hari Ini:", style = MaterialTheme.typography.bodySmall)
                        Text(formatRp(uiState.todayStoreGrossProfit), style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Laba Bersih Hari Ini:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatRp(uiState.todayStoreNetProfit),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (uiState.todayStoreNetProfit >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }

        // 5. Action Button: Catat Transaksi Toko
        item {
            AccessibleActionButton(
                text = "+ Catat Transaksi / Beban Toko",
                contentDescription = "Buka formulir untuk mencatat pengeluaran operasional atau pemasukan toko",
                onClick = onOpenAddTransaction,
                backgroundColor = MaterialTheme.colorScheme.secondary,
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 6. Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf("Semua", "Hari Ini", "Penjualan", "Pengeluaran", "Pemasukan")
                items(filters) { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }
        }

        // 7. Transaction List Header
        item {
            Text(
                text = "Riwayat Transaksi Toko (${filteredTransactions.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 8. List Items
        if (filteredTransactions.isEmpty()) {
            item {
                Text(
                    text = "Belum ada catatan transaksi toko.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    onEdit = { onEditTransaction(tx) },
                    onDelete = { onDeleteTransaction(tx.id) }
                )
            }
        }
    }
}

/**
 * Personal Financial Management Tab (Keuangan Pribadi).
 * Tracks daily personal income, personal expenses, balance, and daily cashflow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalLedgerTabContent(
    uiState: MainUiState,
    onOpenAddTransaction: () -> Unit,
    onOpenSetCapital: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    val formatRp = { amount: Double ->
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount)
    }

    var selectedFilter by remember { mutableStateOf("Semua") }

    val personalTransactions = uiState.transactions.filter { it.isPersonal }
    val filteredTransactions = when (selectedFilter) {
        "Hari Ini" -> personalTransactions.filter { it.isToday() }
        "Pengeluaran" -> personalTransactions.filter { it.type == TransactionType.EXPENSE }
        "Pemasukan" -> personalTransactions.filter { it.type == TransactionType.INCOME }
        else -> personalTransactions
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Saldo Pribadi Saat Ini
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Saldo Keuangan Pribadi saat ini: ${formatRp(uiState.personalBalance)}. Saldo awal pribadi: ${formatRp(uiState.personalInitialCapital)}."
                        liveRegion = LiveRegionMode.Polite
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sisa Saldo Pribadi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatRp(uiState.personalBalance),
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (uiState.personalBalance >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
                            )
                        }
                        TextButton(
                            onClick = onOpenSetCapital,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Tombol atur saldo awal keuangan pribadi"
                                }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Saldo Awal")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Saldo Awal: ${formatRp(uiState.personalInitialCapital)}", style = MaterialTheme.typography.bodySmall)
                        Text("Pemasukan: ${formatRp(uiState.personalIncome)}", style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
                        Text("Pengeluaran: ${formatRp(uiState.personalExpense)}", style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                    }
                }
            }
        }

        // 2. Aktivitas Hari Ini (Daily Personal Cashflow)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Aktivitas Keuangan Pribadi Hari Ini. Pemasukan hari ini: ${formatRp(uiState.todayPersonalIncome)}. Pengeluaran hari ini: ${formatRp(uiState.todayPersonalExpense)}. Sisa hari ini: ${formatRp(uiState.todayPersonalBalance)}."
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Arus Pribadi Hari Ini", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (uiState.todayPersonalBalance >= 0) "+${formatRp(uiState.todayPersonalBalance)}" else formatRp(uiState.todayPersonalBalance),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (uiState.todayPersonalBalance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Masuk: ${formatRp(uiState.todayPersonalIncome)}", style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
                        Text("Keluar: ${formatRp(uiState.todayPersonalExpense)}", style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                    }
                }
            }
        }

        // 3. Action Button: Catat Keuangan Pribadi
        item {
            AccessibleActionButton(
                text = "+ Catat Keuangan Pribadi",
                contentDescription = "Buka formulir untuk mencatat pengeluaran atau pemasukan pribadi harian",
                onClick = onOpenAddTransaction,
                backgroundColor = MaterialTheme.colorScheme.secondary,
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf("Semua", "Hari Ini", "Pengeluaran", "Pemasukan")
                items(filters) { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }
        }

        // 5. Section Title
        item {
            Text(
                text = "Riwayat Catatan Pribadi (${filteredTransactions.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 6. List Items
        if (filteredTransactions.isEmpty()) {
            item {
                Text(
                    text = "Belum ada catatan keuangan pribadi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    onEdit = { onEditTransaction(tx) },
                    onDelete = { onDeleteTransaction(tx.id) }
                )
            }
        }
    }
}

/**
 * Universal Card for displaying individual transactions (Store or Personal).
 */
@Composable
fun TransactionRowCard(
    transaction: TransactionItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = transaction.getAccessibilityDescription()
            },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.wallet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (transaction.type == TransactionType.SALE && transaction.costAmount > 0) {
                            Box(
                                modifier = Modifier
                                    .background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Laba: +${transaction.formattedGrossProfit()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IncomeGreen
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.formattedDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val prefix = if (transaction.type.isCredit) "+ " else "- "
                val amountColor = if (transaction.type.isCredit) IncomeGreen else ExpenseRed

                Text(
                    text = "$prefix${transaction.formattedAmount()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Edit and Delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AccessibleActionButton(
                    text = "Edit",
                    contentDescription = "Edit catatan transaksi ${transaction.description}",
                    onClick = onEdit,
                    icon = Icons.Default.Edit,
                    backgroundColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                AccessibleActionButton(
                    text = "Hapus",
                    contentDescription = "Hapus catatan transaksi ${transaction.description}",
                    onClick = onDelete,
                    icon = Icons.Default.Delete,
                    backgroundColor = ExpenseRed
                )
            }
        }
    }
}

/**
 * Universal Dialog for Creating and Editing Products (Full CRUD).
 */
@Composable
fun ProductFormDialog(
    title: String,
    initialName: String,
    initialSku: String,
    initialCostPrice: Double,
    initialSellPrice: Double,
    initialStock: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, sku: String, costPrice: Double, sellPrice: Double, initialStock: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var sku by remember { mutableStateOf(initialSku) }
    var costPriceStr by remember { mutableStateOf(if (initialCostPrice > 0) initialCostPrice.toInt().toString() else "") }
    var sellPriceStr by remember { mutableStateOf(if (initialSellPrice > 0) initialSellPrice.toInt().toString() else "") }
    var stockStr by remember { mutableStateOf(if (initialStock > 0) initialStock.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk (Wajib)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("Kode Barcode / SKU (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it },
                    label = { Text("Harga Modal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sellPriceStr,
                    onValueChange = { sellPriceStr = it },
                    label = { Text("Harga Jual (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("Jumlah Stok") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cost = costPriceStr.toDoubleOrNull() ?: 0.0
                    val sell = sellPriceStr.toDoubleOrNull() ?: 0.0
                    val stock = stockStr.toIntOrNull() ?: 0
                    onSave(name, sku, cost, sell, stock)
                },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Simpan", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Batal", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/**
 * Accessible Dialog for setting Initial Capital (Modal Awal Toko / Saldo Awal Pribadi).
 */
@Composable
fun CapitalFormDialog(
    title: String,
    initialAmount: Double,
    onDismiss: () -> Unit,
    onSave: (amount: Double) -> Unit
) {
    var amountStr by remember {
        mutableStateOf(if (initialAmount > 0) initialAmount.toInt().toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Masukkan saldo awal atau modal awal kas. Nominal ini akan menjadi dasar perhitungan saldo berjalan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Nominal Saldo Awal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(amount)
                },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Simpan", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Batal", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/**
 * Dialog for Creating and Editing Daily Financial Transactions.
 * Supports Categories, Wallets, and Type Selection for Store vs Personal.
 */
@Composable
fun DailyTransactionFormDialog(
    title: String,
    initialDescription: String,
    initialAmount: Double,
    initialType: TransactionType,
    initialCategory: String,
    initialWallet: String,
    isPersonal: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (description: String, amount: Double, type: TransactionType, category: String, wallet: String) -> Unit
) {
    var description by remember { mutableStateOf(initialDescription) }
    var amountStr by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toInt().toString() else "") }
    var selectedType by remember {
        mutableStateOf(if (initialType == TransactionType.SALE) TransactionType.INCOME else initialType)
    }

    val storeExpenseCategories = listOf(
        "Operasional Toko",
        "Sewa Tempat",
        "Listrik & Air Toko",
        "Gaji Karyawan",
        "Belanja Modal / Stok",
        "Ongkir & Pengiriman",
        "Lain-lain Toko"
    )
    val storeIncomeCategories = listOf(
        "Penjualan Toko",
        "Pendapatan Lain Toko",
        "Tambahan Modal Pemilik",
        "Lain-lain Toko"
    )
    val storeWallets = listOf("Kas Toko (Tunai)", "Rekening Bank Toko", "QRIS / E-Wallet Toko")

    val personalExpenseCategories = listOf(
        "Makanan & Minuman",
        "Transport & Bensin",
        "Kebutuhan Rumah",
        "Pulsa & Listrik",
        "Tagihan Pribadi",
        "Hiburan & Jajan",
        "Lain-lain Pribadi"
    )
    val personalIncomeCategories = listOf(
        "Gaji & Upah",
        "Bagi Hasil Toko",
        "Uang Saku",
        "Hadiah & Bonus",
        "Lain-lain Pribadi"
    )
    val personalWallets = listOf("Tunai", "Rekening Bank Pribadi", "E-Wallet (Gopay/OVO/Dana)")

    val currentCategories = if (isPersonal) {
        if (selectedType == TransactionType.EXPENSE) personalExpenseCategories else personalIncomeCategories
    } else {
        if (selectedType == TransactionType.EXPENSE) storeExpenseCategories else storeIncomeCategories
    }

    val currentWallets = if (isPersonal) personalWallets else storeWallets

    var selectedCategory by remember {
        mutableStateOf(
            if (initialCategory.isNotBlank() && initialCategory in currentCategories) {
                initialCategory
            } else {
                currentCategories.firstOrNull() ?: "Lain-lain"
            }
        )
    }
    var selectedWallet by remember {
        mutableStateOf(
            if (initialWallet.isNotBlank() && initialWallet in currentWallets) {
                initialWallet
            } else {
                currentWallets.firstOrNull() ?: "Tunai"
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Selector: Pengeluaran vs Pemasukan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                selectedType = TransactionType.EXPENSE
                                selectedCategory = if (isPersonal) "Makanan & Minuman" else "Operasional Toko"
                            }
                            .padding(4.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = {
                                selectedType = TransactionType.EXPENSE
                                selectedCategory = if (isPersonal) "Makanan & Minuman" else "Operasional Toko"
                            }
                        )
                        Text("Pengeluaran")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                selectedType = TransactionType.INCOME
                                selectedCategory = if (isPersonal) "Gaji & Upah" else "Penjualan Toko"
                            }
                            .padding(4.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = {
                                selectedType = TransactionType.INCOME
                                selectedCategory = if (isPersonal) "Gaji & Upah" else "Penjualan Toko"
                            }
                        )
                        Text("Pemasukan")
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = {
                        Text(if (isPersonal) "Keterangan (misal: Beli makan siang)" else "Keterangan (misal: Beli plastik & lakban)")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection
                Text("Pilih Kategori:", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    currentCategories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat }
                            )
                            Text(cat, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Wallet Selection
                Text("Sumber Dana / Dompet Kas:", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    currentWallets.forEach { wal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedWallet = wal }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedWallet == wal,
                                onClick = { selectedWallet = wal }
                            )
                            Text(wal, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(description, amount, selectedType, selectedCategory, selectedWallet)
                },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Simpan", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Batal", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
