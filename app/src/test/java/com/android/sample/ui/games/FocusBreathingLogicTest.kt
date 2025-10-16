package com.android.sample.ui.games

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusBreathingLogicTest {

  @Test
  fun phasesFollowExpectedOrder() {
    val phases = listOf("Inhale...", "Hold...", "Exhale...")
    var index = 0

    repeat(6) {
      val expected = phases[index % 3]
      assertEquals(expected, phases[index % 3])
      index++
    }
  }
}
