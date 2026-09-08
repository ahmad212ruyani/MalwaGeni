package com.malwageni.app.data

import android.content.Context
import com.malwageni.app.model.ProductItem
import com.malwageni.app.model.TransactionItem
import com.malwageni.app.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local file-based JSON persistence engine.
 * Stores products and financial transactions directly in app's internal storage
 * so data is permanently saved offline and never lost on app restart.
 */
class LocalDataRepository(private val context: Context) {

    private val dbFile = File(context.filesDir, "malwageni_database.json")

    fun loadProducts(): List<ProductItem> {
        return try {
            if (!dbFile.exists()) return emptyList()
            val content = dbFile.readText()
            if (content.isBlank()) return emptyList()
            val root = JSONObject(content)
            val jsonArray = root.optJSONArray("products") ?: return emptyList()

            val list = mutableListOf<ProductItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ProductItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        sku = obj.optString("sku", "-"),
                        costPrice = obj.optDouble("costPrice", 0.0),
                        sellPrice = obj.optDouble("sellPrice", 0.0),
                        stockQuantity = obj.optInt("stockQuantity", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProducts(products: List<ProductItem>) {
        try {
            val root = if (dbFile.exists() && dbFile.readText().isNotBlank()) {
                JSONObject(dbFile.readText())
            } else {
                JSONObject()
            }

            val jsonArray = JSONArray()
            for (p in products) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("sku", p.sku)
                    put("costPrice", p.costPrice)
                    put("sellPrice", p.sellPrice)
                    put("stockQuantity", p.stockQuantity)
                }
                jsonArray.put(obj)
            }
            root.put("products", jsonArray)
            dbFile.writeText(root.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadTransactions(): List<TransactionItem> {
        return try {
            if (!dbFile.exists()) return emptyList()
            val content = dbFile.readText()
            if (content.isBlank()) return emptyList()
            val root = JSONObject(content)
            val jsonArray = root.optJSONArray("transactions") ?: return emptyList()

            val list = mutableListOf<TransactionItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.optString("type", TransactionType.EXPENSE.name)
                val type = try {
                    TransactionType.valueOf(typeStr)
                } catch (e: Exception) {
                    TransactionType.EXPENSE
                }

                list.add(
                    TransactionItem(
                        id = obj.getString("id"),
                        description = obj.getString("description"),
                        amount = obj.optDouble("amount", 0.0),
                        type = type,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTransactions(transactions: List<TransactionItem>) {
        try {
            val root = if (dbFile.exists() && dbFile.readText().isNotBlank()) {
                JSONObject(dbFile.readText())
            } else {
                JSONObject()
            }

            val jsonArray = JSONArray()
            for (t in transactions) {
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("description", t.description)
                    put("amount", t.amount)
                    put("type", t.type.name)
                    put("timestamp", t.timestamp)
                }
                jsonArray.put(obj)
            }
            root.put("transactions", jsonArray)
            dbFile.writeText(root.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
