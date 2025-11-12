package com.android.sample.ui.focus

import android.Manifest
import android.content.Context
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FocusModeManager {

  /**
   * Activate Focus Mode:
   * - Cancels all scheduled notifications (tag = "notifications")
   * - Gives short haptic feedback
   */
  fun activate(context: Context) {
    CoroutineScope(Dispatchers.Default).launch {
      try {
        WorkManager.getInstance(context).cancelAllWorkByTag("notifications")
        giveHapticFeedback(context)
      } catch (e: Exception) {
        // Log silently
      }
    }
  }

  /**
   * Deactivate Focus Mode:
   * - Plays a small completion sound
   */
  fun deactivate(context: Context) {
    CoroutineScope(Dispatchers.Default).launch {
      try {
        playCompletionSound(context)
      } catch (e: Exception) {
        // Log silently
      }
    }
  }

  /** Optional haptic feedback when activating Focus Mode */
  @RequiresPermission(Manifest.permission.VIBRATE)
  private fun giveHapticFeedback(context: Context) {
    try {
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
      val effect = VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
      vibrator.vibrate(effect)
    } catch (_: Exception) {
      // Ignore if device doesn’t support vibration
    }
  }

  /** Plays a short sound when focus session ends */
  private fun playCompletionSound(context: Context) {
    try {
      val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val ringtone = RingtoneManager.getRingtone(context, notification)
      ringtone.play()
    } catch (_: Exception) {
      // silently ignore
    }
  }
}
