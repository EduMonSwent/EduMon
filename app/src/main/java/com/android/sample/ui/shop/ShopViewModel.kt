package com.android.sample.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.R
import com.android.sample.data.UserStatsRepository
import com.android.sample.repos_providors.AppRepositories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
class ShopViewModel(
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository
) : ViewModel() {

  // Expose coins from the single source of truth
  val userCoins: StateFlow<Int> =
      userStatsRepository.stats
          .map { it.coins }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  private val _items = MutableStateFlow(initialCosmetics())
  val items: StateFlow<List<CosmeticItem>> = _items

  init {
    userStatsRepository.start()
  }

  /**
   * Attempts to buy an item.
   *
   * @return true if the purchase succeeds, false otherwise.
   */
  fun buyItem(item: CosmeticItem): Boolean {
    val coins = userCoins.value
    return if (coins >= item.price && !item.owned) {
      viewModelScope.launch { userStatsRepository.updateCoins(-item.price) }
      _items.value =
          _items.value.map { currentItem ->
            if (currentItem.id == item.id) currentItem.copy(owned = true) else currentItem
          }
      true
    } else {
      false
    }
  }
}

// Initial cosmetics data
private fun initialCosmetics() =
    listOf(
        CosmeticItem("1", "Cool Shades", 500, R.drawable.cosmetic_glasses),
        CosmeticItem("2", "Wizard Hat", 800, R.drawable.cosmetic_hat),
        CosmeticItem("3", "Red Scarf", 300, R.drawable.cosmetic_scarf),
        CosmeticItem("4", "Cyber Wings", 1200, R.drawable.cosmetic_wings),
        CosmeticItem("5", "Epic Aura", 1000, R.drawable.cosmetic_aura),
        CosmeticItem("6", "Hero Cape", 700, R.drawable.cosmetic_cape))
