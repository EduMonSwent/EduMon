package com.android.sample.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsUtilsTest {
  @Test
  fun `formatMinutes under one hour`() {
    assertEquals("45m", privateFormat(45))
  }

  @Test
  fun `formatMinutes over one hour`() {
    assertEquals("2h 5m", privateFormat(125))
  }

  private fun privateFormat(m: Int): String {
    val h = m / 60
    val mm = m % 60
    return if (h > 0) "${h}h ${mm}m" else "${mm}m"
  }
}
