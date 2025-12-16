// This code was written with the assistance of an AI (LLM).
package com.android.sample.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.profile.ProfileRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.shop.repository.ShopRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(
    private val profileRepository: ProfileRepository = AppRepositories.profileRepository,
    private val shopRepository: ShopRepository = AppRepositories.shopRepository
) : ViewModel() {

    private object Constants {
        const val FLOW_TIMEOUT_MS = 5000L
        const val INITIAL_COINS = 0
        const val OWNED_PREFIX = "owned:"
    }

    val userCoins: StateFlow<Int> =
        profileRepository.profile
            .map { it.coins }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(Constants.FLOW_TIMEOUT_MS),
                Constants.INITIAL_COINS
            )

    val items: StateFlow<List<CosmeticItem>> = shopRepository.items

    init {
        viewModelScope.launch {
            shopRepository.getItems()
            shopRepository.refreshOwnedStatus()
        }
    }

    fun buyItem(item: CosmeticItem): Boolean {
        val profile = profileRepository.profile.value

        if (profile.coins < item.price || item.owned) return false

        val ownedEntry = Constants.OWNED_PREFIX + item.id
        val alreadyOwned = profile.accessories.contains(ownedEntry)
        if (alreadyOwned) return false

        val updatedAccessories = profile.accessories + ownedEntry
        val updatedProfile = profile.copy(
            coins = profile.coins - item.price,
            accessories = updatedAccessories
        )

        viewModelScope.launch {
            profileRepository.updateProfile(updatedProfile)
            shopRepository.refreshOwnedStatus()
        }

        return true
    }
}