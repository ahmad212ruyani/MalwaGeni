package com.malwageni.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class ConnectivityStatus(val isConnected: Boolean, val messageResKey: String) {
    AVAILABLE(true, "Terhubung ke jaringan internet"),
    LOST(false, "Koneksi internet terputus, beralih ke mode offline"),
    UNAVAILABLE(false, "Tidak ada jaringan internet")
}

interface ConnectivityObserver {
    fun observe(): Flow<ConnectivityStatus>
}

class NetworkConnectivityObserver(context: Context) : ConnectivityObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<ConnectivityStatus> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    trySend(ConnectivityStatus.AVAILABLE)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    trySend(ConnectivityStatus.LOST)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    trySend(ConnectivityStatus.UNAVAILABLE)
                }
            }

            // Check initial state
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val isInitiallyConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            trySend(if (isInitiallyConnected) ConnectivityStatus.AVAILABLE else ConnectivityStatus.UNAVAILABLE)

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, callback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
    }
}
