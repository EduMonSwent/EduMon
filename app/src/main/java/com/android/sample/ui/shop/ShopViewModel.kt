package com.android.sample.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.R
import com.android.sample.profile.ProfileRepository
import com.android.sample.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

    val userCoins: StateFlow<Int> =
        profileRepository.profile.map { it.coins }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    private val _items = kotlinx.coroutines.flow.MutableStateFlow(initialCosmetics())
    val items: StateFlow<List<CosmeticItem>> = _items

    fun buyItem(item: CosmeticItem): Boolean {
        val profile = profileRepository.profile.value

        if (profile.coins < item.price || item.owned) return false

        val updatedProfile = profile.copy(
            coins = profile.coins - item.price,
            accessories = profile.accessories + "owned:${item.id}"
        )

        viewModelScope.launch {
            profileRepository.updateProfile(updatedProfile)
        }

        _items.value = _items.value.map { current ->
            if (current.id == item.id) current.copy(owned = true) else current
        }

        return true
    }
}

private fun initialCosmetics() =
    listOf(
        CosmeticItem("glasses", "Cool Shades", 200, R.drawable.cosmetic_glasses),
        CosmeticItem("hat", "Wizard Hat", 200, R.drawable.cosmetic_hat),
        CosmeticItem("scarf", "Red Scarf", 200, R.drawable.cosmetic_scarf),
        CosmeticItem("wings", "Cyber Wings", 200, R.drawable.cosmetic_wings),
        CosmeticItem("aura", "Epic Aura", 1500, R.drawable.cosmetic_aura),
        CosmeticItem("cape", "Hero Cape", 200, R.drawable.cosmetic_cape)
    )
