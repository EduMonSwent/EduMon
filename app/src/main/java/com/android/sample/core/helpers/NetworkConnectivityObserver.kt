package com.android.sample.core.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes network connectivity changes and provides a Flow of connection status. Used to determine
 * if the device is online/offline for features like Shop purchases.
 */
object NetworkConnectivityObserver {

  /**
   * Returns a Flow that emits true when network is available, false when unavailable. The flow
   * automatically updates when connectivity changes.
   */
  fun observe(context: Context): Flow<Boolean> =
      callbackFlow {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Send initial state
            trySend(isCurrentlyConnected(connectivityManager))

            val callback =
                object : ConnectivityManager.NetworkCallback() {
                  override fun onAvailable(network: Network) {
                    trySend(true)
                  }

                  override fun onLost(network: Network) {
                    trySend(false)
                  }

                  override fun onCapabilitiesChanged(
                      network: Network,
                      capabilities: NetworkCapabilities
                  ) {
                    val hasInternet =
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    trySend(hasInternet)
                  }
                }

            val request =
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

            connectivityManager.registerNetworkCallback(request, callback)

            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
          }
          .distinctUntilChanged()

  /**
   * Checks if the device is currently connected to the internet. Synchronous check for one-time
   * use.
   */
  fun isCurrentlyConnected(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return isCurrentlyConnected(connectivityManager)
  }

  private fun isCurrentlyConnected(connectivityManager: ConnectivityManager): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
