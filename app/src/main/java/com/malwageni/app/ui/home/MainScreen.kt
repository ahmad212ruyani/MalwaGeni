package com.malwageni.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.malwageni.app.model.TransactionType
import com.malwageni.app.model.UserAccount
import com.malwageni.app.ui.components.AccessibleActionButton
import com.malwageni.app.ui.components.AccessibleNetworkStatusBar
import com.malwageni.app.ui.components.AccessibleProductCard
import com.malwageni.app.ui.components.AccessibleTransactionCard
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
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.accessibilityEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onSave = { name, sku, cost, sell, stock ->
                viewModel.addNewProduct(name, sku, cost, sell, stock)
                showAddProductDialog = false
            }
        )
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTransactionDialog = false },
            onSave = { desc, amount, type ->
                viewModel.addManualTransaction(desc, amount, type)
                showAddTransactionDialog = false
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
                                text = currentUser.displayName,
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
                    contentDescription = "Aplikasi MalwaGeni. $userDesc"
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
                    onDelete = { viewModel.deleteProduct(it) },
                    onOpenAddProduct = { showAddProductDialog = true }
                )
                AppTab.LEDGER -> LedgerTabContent(
                    uiState = uiState,
                    onOpenAddTransaction = { showAddTransactionDialog = true }
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
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AppTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            val activeColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                        contentDescription = if (isSelected) "${tab.title}, sedang aktif. ${tab.a11yDescription}"
                        else "${tab.title}. Ketuk dua kali untuk beralih."
                    }
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth(0.6f)
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
                            contentDescription = "Katalog kasir masih kosong. Silakan buka menu Stok Barang untuk menambahkan produk."
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
                            text = "Katalog Anda masih kosong. Buka tab Stok Barang untuk menambah produk toko Anda sendiri.",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Katalog & Manajemen Stok",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Total ${uiState.products.size} produk terdaftar di database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            AccessibleActionButton(
                text = "+ Tambah Produk Baru",
                contentDescription = "Tombol Tambah Produk Baru. Buka formulir untuk memasukkan barang dagangan Anda sendiri.",
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
                            contentDescription = "Gudang stok masih kosong. Ketuk tombol Tambah Produk Baru di atas untuk mulai mengisi barang Anda."
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
                            text = "Stok Gudang Bersih & Kosong",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tidak ada data tiruan (dummy). Anda dapat mengisi katalog toko Anda sendiri menggunakan tombol '+ Tambah Produk Baru' di atas.",
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
                            horizontalArrangement = Arrangement.End
                        ) {
                            AccessibleActionButton(
                                text = "+10 Stok",
                                contentDescription = "Tambah 10 unit stok untuk ${product.name}",
                                onClick = { onRestock(product.id) },
                                icon = Icons.Default.Add
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AccessibleActionButton(
                                text = "Hapus",
                                contentDescription = "Hapus produk ${product.name} dari database",
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

@Composable
fun LedgerTabContent(
    uiState: MainUiState,
    onOpenAddTransaction: () -> Unit
) {
    val formatRp = { amount: Double ->
        NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount)
    }

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
                        contentDescription = "Buku Keuangan. Total pemasukan: ${formatRp(uiState.totalRevenue)}. Total pengeluaran: ${formatRp(uiState.totalExpense)}. Saldo bersih: ${formatRp(uiState.netBalance)}."
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ringkasan Arus Kas",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pemasukan:", style = MaterialTheme.typography.bodyLarge)
                        Text(formatRp(uiState.totalRevenue), style = MaterialTheme.typography.bodyLarge, color = IncomeGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pengeluaran:", style = MaterialTheme.typography.bodyLarge)
                        Text(formatRp(uiState.totalExpense), style = MaterialTheme.typography.bodyLarge, color = ExpenseRed)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Saldo Bersih:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatRp(uiState.netBalance),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (uiState.netBalance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }

        item {
            AccessibleActionButton(
                text = "+ Catat Transaksi Keuangan",
                contentDescription = "Catat transaksi pemasukan atau pengeluaran keuangan baru Anda",
                onClick = onOpenAddTransaction,
                backgroundColor = MaterialTheme.colorScheme.secondary,
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Riwayat Catatan Transaksi",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (uiState.transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Belum ada transaksi yang dicatat. Ketuk Catat Transaksi Keuangan di atas untuk menambahkan."
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Catatan Transaksi",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Transaksi penjualan dari kasir maupun catatan manual Anda akan otomatis tersimpan di sini.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.transactions) { tx ->
                AccessibleTransactionCard(transaction = tx)
            }
        }
    }
}

/**
 * Accessible Dialog to Add a New Custom Product.
 */
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, sku: String, costPrice: Double, sellPrice: Double, initialStock: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var costPriceStr by remember { mutableStateOf("") }
    var sellPriceStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Produk Baru",
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
                    label = { Text("Jumlah Stok Awal") },
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
                Text("Simpan Produk", style = MaterialTheme.typography.labelLarge)
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
 * Accessible Dialog to Record a Manual Financial Transaction.
 */
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (description: String, amount: Double, type: TransactionType) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Catat Transaksi Keuangan",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { selectedType = TransactionType.EXPENSE }
                            .padding(4.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE }
                        )
                        Text("Pengeluaran")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { selectedType = TransactionType.INCOME }
                            .padding(4.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = TransactionType.INCOME }
                        )
                        Text("Pemasukan")
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Keterangan Transaksi") },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(description, amount, selectedType)
                },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Simpan Transaksi", style = MaterialTheme.typography.labelLarge)
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
