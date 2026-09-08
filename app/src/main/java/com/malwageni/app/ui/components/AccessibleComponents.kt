package com.malwageni.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.malwageni.app.model.ProductItem
import com.malwageni.app.model.TransactionItem
import com.malwageni.app.model.TransactionType
import com.malwageni.app.network.ConnectivityStatus
import com.malwageni.app.ui.theme.ExpenseRed
import com.malwageni.app.ui.theme.IncomeGreen
import com.malwageni.app.ui.theme.StatusOfflineRed
import com.malwageni.app.ui.theme.StatusOnlineGreen

/**
 * Accessible button strictly meeting the 48dp x 48dp touch target standard
 * and providing explicit contentDescription and semantics role.
 */
@Composable
fun AccessibleActionButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // decorative within the accessible button
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Network status indicator with LiveRegion semantics.
 * When status changes, TalkBack automatically announces the change to the user.
 */
@Composable
fun AccessibleNetworkStatusBar(
    status: ConnectivityStatus,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (status.isConnected) StatusOnlineGreen else StatusOfflineRed
    val statusText = if (status.isConnected) {
        "Online: Terhubung ke Cloud Database"
    } else {
        "Offline: Berjalan di memori lokal"
    }

    Surface(
        color = backgroundColor,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = statusText
            }
    ) {
        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusText,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Product Item Card for Inventory and POS.
 * Uses mergeDescendants = true so TalkBack reads the whole item coherently
 * in a single swipe without confusing fragmented sub-elements.
 */
@Composable
fun AccessibleProductCard(
    product: ProductItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val a11ySummary = product.getAccessibilityDescription()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = a11ySummary
                role = Role.Button
            }
            .clickable(
                onClickLabel = "Pilih produk ${product.name} untuk kasir atau ubah stok",
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = product.formattedSellPrice(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Barcode: ${product.sku}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Stok: ${product.stockQuantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (product.stockQuantity <= 5) ExpenseRed else IncomeGreen
                )
            }
        }
    }
}

/**
 * Transaction Item Card for Dual-Ledger personal finance.
 * Merges descendants for smooth TalkBack experience.
 */
@Composable
fun AccessibleTransactionCard(
    transaction: TransactionItem,
    modifier: Modifier = Modifier
) {
    val a11ySummary = transaction.getAccessibilityDescription()
    val amountColor = if (transaction.type.isCredit) IncomeGreen else ExpenseRed
    val prefix = if (transaction.type.isCredit) "+ " else "- "

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = a11ySummary
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.type.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$prefix${transaction.formattedAmount()}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor
            )
        }
    }
}
