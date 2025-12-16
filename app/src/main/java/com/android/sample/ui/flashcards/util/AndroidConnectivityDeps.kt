package com.android.sample.ui.flashcards.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/** This code has been written partially using A.I (LLM). */
internal class AndroidOnlineChecker : OnlineChecker {
  override fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false

    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }
}

internal class AndroidConnectivityObserver : ConnectivityObserver {
  override fun observe(context: Context, onOnlineChanged: (Boolean) -> Unit): DisposableHandle {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback =
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            onOnlineChanged(AndroidOnlineChecker().isOnline(context))
          }

          override fun onLost(network: Network) {
            onOnlineChanged(false)
          }

          override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val online =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            onOnlineChanged(online)
          }
        }

    runCatching { cm.registerDefaultNetworkCallback(callback) }
        .getOrElse {
          val request =
              NetworkRequest.Builder()
                  .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                  .build()
          cm.registerNetworkCallback(request, callback)
        }

    return DisposableHandle { runCatching { cm.unregisterNetworkCallback(callback) } }
  }
}
