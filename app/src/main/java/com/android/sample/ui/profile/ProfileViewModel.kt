package com.android.sample.ui.profile

// This code has been written partially using A.I (LLM).

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.R
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessoryItem
import com.android.sample.data.AccessorySlot
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.profile.ProfileRepository
import com.android.sample.profile.ProfileRepositoryProvider
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.theme.AccentBlue
import com.android.sample.ui.theme.AccentMint
import com.android.sample.ui.theme.GlowGold
import com.android.sample.ui.theme.PurplePrimary
import com.android.sample.ui.theme.VioletSoft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository,
) : ViewModel() {

  // ----- Profile (name, email, avatar, accessories, settings) -----
  private val _userProfile = MutableStateFlow(profileRepository.profile.value.copy())
  val userProfile: StateFlow<UserProfile> = _userProfile

  // ----- Unified stats from Firestore (/users/{uid}/stats/stats) -----
  private val _userStats = MutableStateFlow(UserStats())
  val userStats: StateFlow<UserStats> = _userStats

  init {
    viewModelScope.launch {
      userStatsRepository.start()
      userStatsRepository.stats.collect { stats -> _userStats.value = stats }
    }
  }

  // Palette from theme
  val accentPalette: List<Color> =
      listOf(AccentViolet, AccentBlue, AccentMint, EventColorSports, AccentMagenta)

  // Accessories catalog
  val accessoryCatalog: List<AccessoryItem> =
  private val _userProfile = MutableStateFlow(repository.profile.value.copy())
  val userProfile: StateFlow<UserProfile> = _userProfile

  val accentPalette = listOf(PurplePrimary, AccentBlue, AccentMint, GlowGold, VioletSoft)

  private fun fullCatalog(): List<AccessoryItem> =
      listOf(
          AccessoryItem("none", AccessorySlot.HEAD, "None"),
          AccessoryItem("hat", AccessorySlot.HEAD, "Hat"),
          AccessoryItem("glasses", AccessorySlot.HEAD, "Glasses"),
          AccessoryItem("none", AccessorySlot.TORSO, "None"),
          AccessoryItem("scarf", AccessorySlot.TORSO, "Scarf"),
          AccessoryItem("cape", AccessorySlot.TORSO, "Cape"),
          AccessoryItem("none", AccessorySlot.BACK, "None"),
          AccessoryItem("wings", AccessorySlot.BACK, "Wings"),
          AccessoryItem("aura", AccessorySlot.BACK, "Aura"))

  val accessoryCatalog: List<AccessoryItem>
    get() {
      val owned = ownedIds()
      return fullCatalog().filter { item -> item.id == "none" || owned.contains(item.id) }
    }

  private val accentVariant = MutableStateFlow(AccentVariant.Base)
  val accentVariantFlow: StateFlow<AccentVariant> = accentVariant

  val accentEffective: StateFlow<Color> =
      combine(userProfile, accentVariantFlow) { user, variant ->
            applyAccentVariant(Color(user.avatarAccent.toInt()), variant)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Color(0xFF7C4DFF))

  fun accessoryResId(slot: AccessorySlot, id: String): Int {
    return when (slot) {
      AccessorySlot.HEAD ->
          when (id) {
            "hat" -> R.drawable.cosmetic_hat
            "glasses" -> R.drawable.cosmetic_glasses
            else -> 0
          }
      AccessorySlot.TORSO ->
          when (id) {
            "scarf" -> R.drawable.cosmetic_scarf
            "cape" -> R.drawable.cosmetic_cape
            else -> 0
          }
      AccessorySlot.BACK ->
          when (id) {
            "wings" -> R.drawable.cosmetic_wings
            "aura" -> R.drawable.cosmetic_aura
            else -> 0
          }
      else -> 0
    }
  }

  private fun ownedIds(): Set<String> {
    return _userProfile.value.accessories
        .filter { it.startsWith("owned:") }
        .map { it.removePrefix("owned:") }
        .toSet()
  }

  fun setAvatarAccent(color: Color) {
    val argb = color.toArgb().toLong()
    _userProfile.update { it.copy(avatarAccent = argb) }
    pushProfile()
  }

  fun setAccentVariant(variant: AccentVariant) {
    accentVariant.value = variant
  }

  fun toggleNotifications() = updateLocal {
    it.copy(notificationsEnabled = !it.notificationsEnabled)
  }

  fun toggleLocation() = updateLocal { it.copy(locationEnabled = !it.locationEnabled) }

  fun toggleFocusMode() = updateLocal { it.copy(focusModeEnabled = !it.focusModeEnabled) }

  fun equip(slot: AccessorySlot, id: String) {

    val owned = ownedIds()
    if (id != "none" && !owned.contains(id)) return

    val cur = _userProfile.value
    val prefix = slot.name.lowercase() + ":"
    val cleaned = cur.accessories.filterNot { it.startsWith(prefix) }

    val updatedAccessories = if (id == "none") cleaned else cleaned + (prefix + id)

    val updated = cur.copy(accessories = updatedAccessories)
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
    val entry = _userProfile.value.accessories.firstOrNull { it.startsWith(prefix) } ?: return null
    return entry.removePrefix(prefix)
  }

  private fun updateLocal(edit: (UserProfile) -> UserProfile) {
    _userProfile.update(edit)
    pushProfile()
  }

  // ---------- Color helpers ----------

  private fun applyAccentVariant(base: Color, v: AccentVariant): Color =
      when (v) {
        AccentVariant.Base -> base
        AccentVariant.Light ->
            Color(
                (base.red + (1f - base.red) * 0.25f),
                (base.green + (1f - base.green) * 0.25f),
                (base.blue + (1f - base.blue) * 0.25f),
                base.alpha)
        AccentVariant.Dark ->
            Color(base.red * 0.75f, base.green * 0.75f, base.blue * 0.75f, base.alpha)
        AccentVariant.Vibrant ->
            Color(
                (base.red * 1.1f).coerceIn(0f, 1f),
                (base.green * 1.1f).coerceIn(0f, 1f),
                (base.blue * 1.1f).coerceIn(0f, 1f),
                base.alpha)
      }

  fun addCoins(amount: Int) {
    if (amount <= 0) return
    viewModelScope.launch { userStatsRepository.updateCoins(amount) }
  }

  // --- Helpers ---

  private fun pushProfile(updated: UserProfile = _userProfile.value) {
    viewModelScope.launch { runCatching { profileRepository.updateProfile(updated) } }
  }
}
