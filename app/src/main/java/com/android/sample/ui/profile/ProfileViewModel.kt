package com.android.sample.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessoryItem
import com.android.sample.data.AccessorySlot
import com.android.sample.data.Rarity
import com.android.sample.data.UserProfile
import com.android.sample.pet.model.PetState
import com.android.sample.profile.ProfileRepository
import com.android.sample.profile.ProfileRepositoryProvider
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.theme.AccentBlue
import com.android.sample.ui.theme.AccentMagenta
import com.android.sample.ui.theme.AccentMint
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.EventColorSports
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

    val petState: StateFlow<PetState> = AppRepositories.petRepository.state

    private val _userProfile = MutableStateFlow(repository.profile.value)
    val userProfile: StateFlow<UserProfile> = _userProfile

    init {
        viewModelScope.launch {
            repository.profile.collect { remote ->
                _userProfile.value = remote
            }
        }
    }

    val ownedIds: StateFlow<Set<String>> =
        repository.profile.map { it.owned.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val accentPalette: List<Color> =
        listOf(AccentViolet, AccentBlue, AccentMint, EventColorSports, AccentMagenta)

    val accessoryCatalog: List<AccessoryItem> =
        listOf(
            AccessoryItem("none", AccessorySlot.HEAD, "None"),
            AccessoryItem("halo", AccessorySlot.HEAD, "Halo", rarity = Rarity.EPIC),
            AccessoryItem("crown", AccessorySlot.HEAD, "Crown", rarity = Rarity.RARE),
            AccessoryItem("antenna", AccessorySlot.HEAD, "Antenna", rarity = Rarity.COMMON),
            AccessoryItem("none", AccessorySlot.TORSO, "None"),
            AccessoryItem("badge", AccessorySlot.TORSO, "Badge", rarity = Rarity.COMMON),
            AccessoryItem("scarf", AccessorySlot.TORSO, "Scarf", rarity = Rarity.RARE),
            AccessoryItem("armor", AccessorySlot.TORSO, "Armor", rarity = Rarity.EPIC),
            AccessoryItem("none", AccessorySlot.LEGS, "None"),
            AccessoryItem("boots", AccessorySlot.LEGS, "Boots", rarity = Rarity.COMMON),
            AccessoryItem("rocket", AccessorySlot.LEGS, "Rocket", rarity = Rarity.LEGENDARY),
            AccessoryItem("skates", AccessorySlot.LEGS, "Skates", rarity = Rarity.RARE),
        )

    private val accentVariant = MutableStateFlow(AccentVariant.Base)
    val accentVariantFlow: StateFlow<AccentVariant> = accentVariant

    val accentEffective: StateFlow<Color> =
        combine(userProfile, accentVariantFlow) { user, variant ->
            applyAccentVariant(Color(user.avatarAccent.toInt()), variant)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentViolet)

    fun setAvatarAccent(color: Color) {
        val argb: Long = color.toArgb().toLong()
        val updated = _userProfile.value.copy(avatarAccent = argb)
        _userProfile.value = updated
        pushProfile(updated)
    }

    fun setAccentVariant(v: AccentVariant) { accentVariant.value = v }

    fun toggleNotifications() = updateLocal { it.copy(notificationsEnabled = !it.notificationsEnabled) }
    fun toggleLocation() = updateLocal { it.copy(locationEnabled = !it.locationEnabled) }
    fun toggleFocusMode() = updateLocal { it.copy(focusModeEnabled = !it.focusModeEnabled) }

    fun equip(slot: AccessorySlot, id: String) {
        val cur = _userProfile.value
        val prefix = slot.name.lowercase() + ":"
        val cleaned = cur.accessories.filterNot { it.startsWith(prefix) }
        val next = if (id == "none") cleaned else cleaned + (prefix + id)
        val updated = cur.copy(accessories = next)
        _userProfile.value = updated
        pushProfile(updated)
    }

    fun unequip(slot: AccessorySlot) {
        val cur = _userProfile.value
        val prefix = slot.name.lowercase() + ":"
        val updated = cur.copy(accessories = cur.accessories.filterNot { it.startsWith(prefix) })
        _userProfile.value = updated
        pushProfile(updated)
    }

    fun equippedId(slot: AccessorySlot): String? {
        val prefix = slot.name.lowercase() + ":"
        val s = _userProfile.value.accessories.firstOrNull { it.startsWith(prefix) }
        return s?.substringAfter(':')
    }

    fun addCoins(amount: Int) = updateLocal { it.copy(coins = it.coins + amount) }

    private fun updateLocal(block: (UserProfile) -> UserProfile) {
        val updated = block(_userProfile.value)
        _userProfile.value = updated
        pushProfile(updated)
    }

    private fun pushProfile(updated: UserProfile = _userProfile.value) {
        viewModelScope.launch { runCatching { repository.updateProfile(updated) } }
    }

    private fun applyAccentVariant(base: Color, v: AccentVariant): Color =
        when (v) {
            AccentVariant.Base -> base
            AccentVariant.Light -> base.blend(Color.White, 0.25f)
            AccentVariant.Dark -> base.blend(Color.Black, 0.25f)
            AccentVariant.Vibrant -> base.boostSaturation(1.2f).boostValue(1.1f)
        }

    private fun Color.blend(other: Color, amt: Float): Color {
        val t = amt.coerceIn(0f, 1f)
        fun ch(a: Float, b: Float) = a + (b - a) * t
        return Color(
            ch(this.red, other.red),
            ch(this.green, other.green),
            ch(this.blue, other.blue),
            ch(this.alpha, other.alpha)
        )
    }

    private fun Color.boostSaturation(f: Float): Color {
        val r = this.red
        val g = this.green
        val b = this.blue
        val p = kotlin.math.sqrt((r * r * 0.299f) + (g * g * 0.587f) + (b * b * 0.114f))
        fun mix(c: Float) = p + (c - p) * f
        return Color(mix(r), mix(g), mix(b), this.alpha)
    }

    private fun Color.boostValue(f: Float): Color =
        Color(
            (this.red * f).coerceIn(0f, 1f),
            (this.green * f).coerceIn(0f, 1f),
            (this.blue * f).coerceIn(0f, 1f),
            this.alpha
        )
}
