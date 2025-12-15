package com.android.sample.core.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NetworkConnectivityObserver. Tests network connectivity observation and status
 * checking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkConnectivityObserverTest {

  @MockK private lateinit var mockContext: Context

  @MockK private lateinit var mockConnectivityManager: ConnectivityManager

  @MockK private lateinit var mockNetwork: Network

  @MockK private lateinit var mockNetworkCapabilities: NetworkCapabilities

  private lateinit var capturedCallback: ConnectivityManager.NetworkCallback

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)

    // Setup context to return connectivity manager
    every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns
        mockConnectivityManager

    // Capture the network callback when registered
    every {
      mockConnectivityManager.registerNetworkCallback(
          any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>())
    } answers { capturedCallback = secondArg() }

    every {
      mockConnectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>())
    } just Runs
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  // --- Test isCurrentlyConnected with Context parameter ---
  @Test
  fun isCurrentlyConnected_withContext_returnsTrue_whenNetworkAvailable() {
    // Setup
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns
        mockNetworkCapabilities
    every {
      mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } returns true

    // Execute
    val result = NetworkConnectivityObserver.isCurrentlyConnected(mockContext)

    // Verify
    assertTrue(result)
  }

  @Test
  fun isCurrentlyConnected_withContext_returnsFalse_whenNoActiveNetwork() {
    // Setup
    every { mockConnectivityManager.activeNetwork } returns null

    // Execute
    val result = NetworkConnectivityObserver.isCurrentlyConnected(mockContext)

    // Verify
    assertFalse(result)
  }

  @Test
  fun isCurrentlyConnected_withContext_returnsFalse_whenNoCapabilities() {
    // Setup
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns null

    // Execute
    val result = NetworkConnectivityObserver.isCurrentlyConnected(mockContext)

    // Verify
    assertFalse(result)
  }

  @Test
  fun isCurrentlyConnected_withContext_returnsFalse_whenNoInternetCapability() {
    // Setup
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns
        mockNetworkCapabilities
    every {
      mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } returns false

    // Execute
    val result = NetworkConnectivityObserver.isCurrentlyConnected(mockContext)

    // Verify
    assertFalse(result)
  }

  // --- Edge case tests ---
  @Test
  fun isCurrentlyConnected_handlesNullNetwork() {
    every { mockConnectivityManager.activeNetwork } returns null

    val result = NetworkConnectivityObserver.isCurrentlyConnected(mockContext)

    assertFalse(result)
  }

  @Test
  fun isCurrentlyConnected_handlesNullCapabilities() {
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns null

    val result = NetworkConnectivityObserver.isCurrentlyConnected(mockContext)

    assertFalse(result)
  }
}
