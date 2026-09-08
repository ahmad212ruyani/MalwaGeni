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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.malwageni.app.model.TransactionType
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.accessibilityEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MalwaGeni",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Aplikasi MalwaGeni. Sistem Kasir, Stok, dan Keuangan Ramah Aksesibilitas"
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Accessible Network Status Indicator with LiveRegion
            AccessibleNetworkStatusBar(status = uiState.networkStatus)

            // Accessible Navigation Bar Tabs (Linear Focus & >=48dp touch targets)
            AccessibleTabBar(
                selectedTab = uiState.currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            Divider()

            // Dynamic Content based on selected Tab
            when (uiState.currentTab) {
                AppTab.POS -> PosTabContent(
                    uiState = uiState,
                    onAddToCart = { viewModel.addToCart(it) },
                    onClearCart = { viewModel.clearCart() },
                    onCheckout = { viewModel.checkoutCart() }
                )
                AppTab.INVENTORY -> InventoryTabContent(
                    uiState = uiState,
                    onRestock = { viewModel.restockProduct(it) }
                )
                AppTab.LEDGER -> LedgerTabContent(
                    uiState = uiState,
                    onAddSampleExpense = {
                        viewModel.addManualTransaction(
                            description = "Biaya Listrik Toko",
                            amount = 120000.0,
                            type = TransactionType.EXPENSE
                        )
                    }
                )
            }
        }
    }
}

/**
 * Tab Row designed for screen readers with explicit Tab semantics,
 * selected state, and minimum 48dp touch height.
 */
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

/**
 * POS (Kasir) Tab: Display cart overview and fast catalog buttons.
 */
@Composable
fun PosTabContent(
    uiState: MainUiState,
    onAddToCart: (com.malwageni.app.model.ProductItem) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit
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
        // Cart Summary Card (Grouped semantics for screen readers)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Ringkasan Kasir. Total keranjang: $cartCount item. Jumlah tagihan: $formattedTotal."
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
                            contentDescription = "Lanjutkan proses pembayaran kasir senilai $formattedTotal",
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

        items(uiState.products) { product ->
            AccessibleProductCard(
                product = product,
                onClick = { onAddToCart(product) }
            )
        }
    }
}

/**
 * Inventory (Stok & Restock) Tab.
 */
@Composable
fun InventoryTabContent(
    uiState: MainUiState,
    onRestock: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Katalog & Manajemen Persediaan",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Ketuk tombol restock untuk menambahkan 10 unit stok barang ke gudang.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(uiState.products) { product ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Produk: ${product.name}. Barcode: ${product.sku}. Stok saat ini: ${product.stockQuantity} unit."
                    },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Stok: ${product.stockQuantity} unit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (product.stockQuantity <= 5) ExpenseRed else IncomeGreen
                        )
                    }
                    AccessibleActionButton(
                        text = "+10 Stok",
                        contentDescription = "Tambah 10 stok untuk ${product.name}",
                        onClick = { onRestock(product.id) },
                        icon = Icons.Default.Add
                    )
                }
            }
        }
    }
}

/**
 * Dual-Ledger Financial Management Tab.
 */
@Composable
fun LedgerTabContent(
    uiState: MainUiState,
    onAddSampleExpense: () -> Unit
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
        // Financial Balance Card
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
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
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
                text = "+ Catat Pengeluaran Operasional",
                contentDescription = "Catat transaksi beban pengeluaran operasional toko sebesar Rp 120.000",
                onClick = onAddSampleExpense,
                backgroundColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Riwayat Transaksi Terakhir",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(uiState.transactions) { tx ->
            AccessibleTransactionCard(transaction = tx)
        }
    }
}
