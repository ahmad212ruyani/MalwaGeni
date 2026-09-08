package com.malwageni.app.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransactionType(val label: String, val isCredit: Boolean) {
    INCOME("Pemasukan", true),
    EXPENSE("Pengeluaran", false),
    SALE("Penjualan Toko", true)
}

data class TransactionItem(
    val id: String,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val category: String = "Umum",
    val wallet: String = "Tunai",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formattedAmount(): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(amount)
    }

    fun formattedDate(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("in", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun formattedDayOnly(): String {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun isToday(): Boolean {
        val todaySdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return todaySdf.format(Date(timestamp)) == todaySdf.format(Date())
    }

    /**
     * Clear TalkBack description with category, wallet, and flow for screen readers.
     */
    fun getAccessibilityDescription(): String {
        val flowDescription = if (type.isCredit) "Pemasukan Dana" else "Pengeluaran Dana"
        return "Transaksi $flowDescription: $description. Kategori: $category. Sumber dana: $wallet. Jumlah: ${formattedAmount()}. Waktu: ${formattedDate()}."
    }
}
