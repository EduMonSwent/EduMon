package com.android.sample.ui.focus

import android.Manifest
import android.content.Context
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FocusModeManager {

  private const val TAG = "FocusModeManager"

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

        Log.e(TAG, "Failed to activate focus mode", e)
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

        Log.e(TAG, "Failed to play completion sound", e)
      }
    }
  }

  @RequiresPermission(Manifest.permission.VIBRATE)
  private fun giveHapticFeedback(context: Context) {
    try {
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
      val effect = VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
      vibrator.vibrate(effect)
    } catch (e: Exception) {

      Log.w(TAG, "Haptic feedback unavailable", e)
    }
  }

  /** Plays a short sound when focus session ends */
  private fun playCompletionSound(context: Context) {
    try {
      val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val ringtone = RingtoneManager.getRingtone(context, notification)
      ringtone.play()
    } catch (e: Exception) {

      Log.w(TAG, "Unable to play completion sound", e)
    }
  }
}
