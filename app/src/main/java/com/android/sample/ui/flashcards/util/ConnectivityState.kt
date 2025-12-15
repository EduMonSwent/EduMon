package com.android.sample.ui.flashcards.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** This code has been written partially using A.I (LLM). */
internal fun interface DisposableHandle {
  fun dispose()
}

internal fun interface OnlineChecker {
  fun isOnline(context: Context): Boolean
}

internal fun interface ConnectivityObserver {
  fun observe(context: Context, onOnlineChanged: (Boolean) -> Unit): DisposableHandle
}

internal object ConnectivityDeps {
  // Default production implementations live in AndroidConnectivityDeps.kt
  var onlineChecker: OnlineChecker = AndroidOnlineChecker()
  var observer: ConnectivityObserver = AndroidConnectivityObserver()
}

object ConnectivityUtils {
  fun isOnline(context: Context): Boolean = ConnectivityDeps.onlineChecker.isOnline(context)
}

@Composable
fun rememberIsOnline(): State<Boolean> {
  val appContext = LocalContext.current.applicationContext

  val onlineState: MutableState<Boolean> = remember {
    mutableStateOf(ConnectivityUtils.isOnline(appContext))
  }

  DisposableEffect(appContext) {
    val handle =
        ConnectivityDeps.observer.observe(appContext) { isOnline -> onlineState.value = isOnline }

    onDispose { handle.dispose() }
  }

  return onlineState
}
