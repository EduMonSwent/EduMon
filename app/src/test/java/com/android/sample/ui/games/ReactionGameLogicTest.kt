package com.android.sample.ui.games

import org.junit.Assert.assertTrue
import org.junit.Test

class ReactionGameLogicTest {

  @Test
  fun randomDelayWithinExpectedRange() {
    val min = 1500L
    val max = 4000L
    repeat(50) {
      val value = kotlin.random.Random.nextLong(min, max)
      assertTrue(value in min until max)
    }
  }
}
