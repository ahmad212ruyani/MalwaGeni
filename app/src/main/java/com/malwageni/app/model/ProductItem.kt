package com.malwageni.app.model

import java.text.NumberFormat
import java.util.Locale

data class ProductItem(
    val id: String,
    val name: String,
    val sku: String,
    val costPrice: Double,
    val sellPrice: Double,
    val stockQuantity: Int
) {
    /**
     * Formats price into Indonesian Rupiah format.
     */
    fun formattedSellPrice(): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(sellPrice)
    }

    /**
     * Generates a coherent, screen reader optimized sentence.
     * Prevents screen readers from fragmenting data into multiple disconnected swipes.
     */
    fun getAccessibilityDescription(): String {
        return "Produk: $name. Kode barcode: $sku. Harga jual: ${formattedSellPrice()}. Sisa stok: $stockQuantity unit."
    }
}
