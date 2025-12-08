package com.android.sample.ui.theme

import androidx.annotation.DrawableRes
import com.android.sample.R

/** Default sprite per starter. Used for avatar, games, headers, etc. */
@DrawableRes
fun spriteForStarter(starterId: String?): Int =
    when (starterId) {
      EduMonStarterIds.SECOND -> R.drawable.edumon2
      EduMonStarterIds.THIRD -> R.drawable.edumon1
      else -> R.drawable.edumon // default + first starter
    }

/**
 * Default environment/background per starter. You can reuse these wherever you want a "world"
 * behind EduMon (profile pet section, games, etc.).
 */
@DrawableRes
fun environmentForStarter(starterId: String?): Int =
    when (starterId) {
      EduMonStarterIds.SECOND -> R.drawable.bg_aquamon
      EduMonStarterIds.THIRD -> R.drawable.bg_floramon
      else -> R.drawable.bg_pyrmon
    }
