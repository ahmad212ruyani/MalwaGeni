package com.malwageni.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.malwageni.app.model.ProductItem
import com.malwageni.app.model.TransactionItem
import com.malwageni.app.model.TransactionType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Cloud Firestore Database Repository for online cloud synchronization
 * with full Create, Read, Update, and Delete (CRUD) operations.
 */
class CloudFirestoreRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Enable persistent disk cache so offline changes automatically sync to cloud when connected
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
    }

    private fun productsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("products")

    private fun transactionsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("transactions")

    /**
     * Realtime listener Flow for Products collection in Cloud Firestore.
     */
    fun observeProducts(userId: String): Flow<List<ProductItem>> = callbackFlow {
        val listener: ListenerRegistration = productsCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // In case of error (e.g. permission rules), don't crash
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        try {
                            ProductItem(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                sku = doc.getString("sku") ?: "-",
                                costPrice = doc.getDouble("costPrice") ?: 0.0,
                                sellPrice = doc.getDouble("sellPrice") ?: 0.0,
                                stockQuantity = doc.getLong("stockQuantity")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(products)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Realtime listener Flow for Transactions collection in Cloud Firestore.
     */
    fun observeTransactions(userId: String): Flow<List<TransactionItem>> = callbackFlow {
        val listener: ListenerRegistration = transactionsCollection(userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        try {
                            val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                            val type = try {
                                TransactionType.valueOf(typeStr)
                            } catch (_: Exception) {
                                TransactionType.EXPENSE
                            }
                            val costAmount = doc.getDouble("costAmount") ?: 0.0
                            val isPersonal = doc.getBoolean("isPersonal") ?: false
                            TransactionItem(
                                id = doc.id,
                                description = doc.getString("description") ?: "",
                                amount = doc.getDouble("amount") ?: 0.0,
                                type = type,
                                category = doc.getString("category") ?: "Umum",
                                wallet = doc.getString("wallet") ?: "Tunai",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                costAmount = costAmount,
                                isPersonal = isPersonal
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(transactions)
                }
            }

        awaitClose { listener.remove() }
    }

    private fun financeSettingsDoc(userId: String) =
        firestore.collection("users").document(userId).collection("settings").document("finance")

    fun observeFinanceSettings(userId: String): Flow<Pair<Double, Double>> = callbackFlow {
        val listener: ListenerRegistration = financeSettingsDoc(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val storeCapital = snapshot.getDouble("storeInitialCapital") ?: 0.0
                    val personalCapital = snapshot.getDouble("personalInitialCapital") ?: 0.0
                    trySend(Pair(storeCapital, personalCapital))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateStoreInitialCapital(userId: String, capital: Double) {
        val data = hashMapOf<String, Any>(
            "storeInitialCapital" to capital,
            "updatedAt" to System.currentTimeMillis()
        )
        financeSettingsDoc(userId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun updatePersonalInitialCapital(userId: String, capital: Double) {
        val data = hashMapOf<String, Any>(
            "personalInitialCapital" to capital,
            "updatedAt" to System.currentTimeMillis()
        )
        financeSettingsDoc(userId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    // =========================================================================
    // CRUD: CREATE
    // =========================================================================

    suspend fun addProduct(userId: String, product: ProductItem) {
        val data = hashMapOf(
            "name" to product.name,
            "sku" to product.sku,
            "costPrice" to product.costPrice,
            "sellPrice" to product.sellPrice,
            "stockQuantity" to product.stockQuantity,
            "updatedAt" to System.currentTimeMillis()
        )
        productsCollection(userId).document(product.id).set(data).await()
    }

    suspend fun addTransaction(userId: String, transaction: TransactionItem) {
        val data = hashMapOf(
            "description" to transaction.description,
            "amount" to transaction.amount,
            "type" to transaction.type.name,
            "category" to transaction.category,
            "wallet" to transaction.wallet,
            "timestamp" to transaction.timestamp,
            "costAmount" to transaction.costAmount,
            "isPersonal" to transaction.isPersonal
        )
        transactionsCollection(userId).document(transaction.id).set(data).await()
    }

    // =========================================================================
    // CRUD: UPDATE (EDIT)
    // =========================================================================

    suspend fun updateProduct(userId: String, product: ProductItem) {
        val updates = hashMapOf<String, Any>(
            "name" to product.name,
            "sku" to product.sku,
            "costPrice" to product.costPrice,
            "sellPrice" to product.sellPrice,
            "stockQuantity" to product.stockQuantity,
            "updatedAt" to System.currentTimeMillis()
        )
        productsCollection(userId).document(product.id).update(updates).await()
    }

    suspend fun updateProductStock(userId: String, productId: String, newStock: Int) {
        productsCollection(userId).document(productId).update("stockQuantity", newStock).await()
    }

    suspend fun updateTransaction(userId: String, transaction: TransactionItem) {
        val updates = hashMapOf<String, Any>(
            "description" to transaction.description,
            "amount" to transaction.amount,
            "type" to transaction.type.name,
            "category" to transaction.category,
            "wallet" to transaction.wallet,
            "costAmount" to transaction.costAmount,
            "isPersonal" to transaction.isPersonal
        )
        transactionsCollection(userId).document(transaction.id).update(updates).await()
    }

    // =========================================================================
    // CRUD: DELETE (HAPUS)
    // =========================================================================

    suspend fun deleteProduct(userId: String, productId: String) {
        productsCollection(userId).document(productId).delete().await()
    }

    suspend fun deleteTransaction(userId: String, transactionId: String) {
        transactionsCollection(userId).document(transactionId).delete().await()
    }
}
