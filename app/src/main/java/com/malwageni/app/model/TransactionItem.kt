package com.malwageni.app.model

import java.text.NumberFormat
import java.util.Locale

enum class TransactionType(val label: String, val isCredit: Boolean) {
    INCOME("Pemasukan Pribadi", true),
    EXPENSE("Pengeluaran Pribadi", false),
    SALE("Penjualan Kasir", true)
}

data class TransactionItem(
    val id: String,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formattedAmount(): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(amount)
    }

    /**
     * Clear TalkBack description specifying whether funds are incoming or outgoing.
     */
    fun getAccessibilityDescription(): String {
        val flowDescription = if (type.isCredit) "Dana Masuk" else "Dana Keluar"
        return "Transaksi ${type.label}: $description. Jumlah: ${formattedAmount()}, status: $flowDescription."
    }
}
