package com.android.sample.data.notifications

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit

object StreakPrefs {
  private const val FILE = "edumon_prefs"
  private const val KEY_STREAK = "streak_days"

  fun get(ctx: Context): Int = ctx.getSharedPreferences(FILE, MODE_PRIVATE).getInt(KEY_STREAK, 0)

  fun set(ctx: Context, days: Int) {
    ctx.getSharedPreferences(FILE, MODE_PRIVATE).edit { putInt(KEY_STREAK, days.coerceAtLeast(0)) }
  }
}
