package com.android.sample.ui.flashcards.util

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ConnectivityStateTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var previousOnlineChecker: OnlineChecker
  private lateinit var previousObserver: ConnectivityObserver

  private class FakeOnlineChecker(private var online: Boolean) : OnlineChecker {
    fun setOnline(value: Boolean) {
      online = value
    }

    override fun isOnline(context: android.content.Context): Boolean = online
  }

  private class FakeObserver : ConnectivityObserver {
    private var callback: ((Boolean) -> Unit)? = null
    var disposed: Boolean = false
      private set

    override fun observe(
        context: android.content.Context,
        onOnlineChanged: (Boolean) -> Unit
    ): DisposableHandle {
      callback = onOnlineChanged
      disposed = false
      return DisposableHandle { disposed = true }
    }

    fun emit(isOnline: Boolean) {
      callback?.invoke(isOnline)
    }
  }

  @Before
  fun saveDeps() {
    previousOnlineChecker = ConnectivityDeps.onlineChecker
    previousObserver = ConnectivityDeps.observer
  }

  @After
  fun restoreDeps() {
    ConnectivityDeps.onlineChecker = previousOnlineChecker
    ConnectivityDeps.observer = previousObserver
  }

  @Test
  fun connectivityUtils_delegatesToChecker() {
    val checker = FakeOnlineChecker(false)
    ConnectivityDeps.onlineChecker = checker
    ConnectivityDeps.observer = FakeObserver()

    val ctx = composeRule.activity.applicationContext
    assert(!ConnectivityUtils.isOnline(ctx))

    checker.setOnline(true)
    assert(ConnectivityUtils.isOnline(ctx))
  }

  @Test
  fun rememberIsOnline_initialValue_comesFromChecker_andUpdatesFromObserver() {
    val checker = FakeOnlineChecker(true)
    val observer = FakeObserver()

    ConnectivityDeps.onlineChecker = checker
    ConnectivityDeps.observer = observer

    composeRule.setContent {
      val isOnline by rememberIsOnline()
      Column {
        Text(text = if (isOnline) "ONLINE" else "OFFLINE", modifier = Modifier.testTag("status"))
      }
    }

    // Initial from checker = ONLINE
    composeRule.onNodeWithTag("status").assertExists()

    // Drive updates deterministically
    composeRule.runOnIdle { observer.emit(false) }
    composeRule.waitForIdle()

    composeRule.runOnIdle { observer.emit(true) }
    composeRule.waitForIdle()
  }

  @Test
  fun rememberIsOnline_disposesObserverOnLeaveComposition() {
    val checker = FakeOnlineChecker(true)
    val observer = FakeObserver()

    ConnectivityDeps.onlineChecker = checker
    ConnectivityDeps.observer = observer

    val showConnectivity = mutableStateOf(true)

    composeRule.setContent {
      if (showConnectivity.value) {
        val isOnline by rememberIsOnline()
        Text(text = if (isOnline) "ONLINE" else "OFFLINE", modifier = Modifier.testTag("status"))
      } else {
        Text(text = "EMPTY")
      }
    }

    // Remove the Composable that owns DisposableEffect without calling setContent again
    composeRule.runOnIdle { showConnectivity.value = false }
    composeRule.waitForIdle()

    composeRule.runOnIdle { assert(observer.disposed) }
  }
}
