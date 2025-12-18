package com.android.sample.ui.schedule

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.util.isOnline
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectivityUtilsTest {

  @Test
  fun isOnline_returnsFalse_whenNoNetworkAvailable() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Robolectric default: no active network
    assertFalse(context.isOnline())
  }
}
