package com.android.sample.ui.profile

sealed class StatGainEvent {
  data class PointsGained(val amount: Int) : StatGainEvent()

  data class CoinsGained(val amount: Int) : StatGainEvent()
}
