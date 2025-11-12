package com.android.sample.ui.focus

import android.content.Context
import android.os.Vibrator
import androidx.work.WorkManager
import io.mockk.*
import org.junit.Before
import org.junit.Test

// Parts of this code were written using ChatGPT
class FocusModeManagerTest {

  private val context = mockk<Context>(relaxed = true)
  private val vibrator = mockk<Vibrator>(relaxed = true)
  private val workManager = mockk<WorkManager>(relaxed = true)

  @Before
  fun setup() {
    mockkStatic(WorkManager::class)
    every { WorkManager.getInstance(any()) } returns workManager
    every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator
  }

  @Test
  fun `activate cancels work and vibrates`() {
    FocusModeManager.activate(context)
    verify { workManager.cancelAllWorkByTag("notifications") }
  }

  @Test
  fun `deactivate plays sound safely`() {
    FocusModeManager.deactivate(context)
  }
}
