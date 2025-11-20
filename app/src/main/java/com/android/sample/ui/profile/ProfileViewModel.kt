package com.android.sample.ui.profile

// This code has been written partially using A.I (LLM).

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessoryItem
import com.android.sample.data.AccessorySlot
import com.android.sample.data.Rarity
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Profile screen. UserProfile handles cosmetic and preference data.
 * UserStatsRepository is the single source of truth for study time, streak, goals and coins.
 */
class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository,
) : ViewModel() {

  // ----- Local profile state (cosmetics + preferences) -----
  private val _userProfile = MutableStateFlow(repository.profile.value.copy())
  val userProfile: StateFlow<UserProfile> = _userProfile

  // ----- Shared user stats (single source of truth) -----
  private val _userStats = MutableStateFlow(UserStats())
  val userStats: StateFlow<UserStats> = _userStats

  init {
    // Start realtime stats listener and keep coins in profile in sync for UI that still reads from
    // UserProfile
    userStatsRepository.start()
    viewModelScope.launch {
      userStatsRepository.stats.collect { stats ->
        _userStats.value = stats
        _userProfile.update {
          it.copy(
              coins = stats.coins,
              points = stats.points,
          )
        }
      }
    }
  }

  // Palette from theme
  val accentPalette: List<Color> =
      listOf(AccentViolet, AccentBlue, AccentMint, EventColorSports, AccentMagenta)

  // Catalog with rarities and "None" for each slot
  val accessoryCatalog: List<AccessoryItem> =
      listOf(
          // HEAD
          AccessoryItem("none", AccessorySlot.HEAD, "None"),
          AccessoryItem("halo", AccessorySlot.HEAD, "Halo", rarity = Rarity.EPIC),
          AccessoryItem("crown", AccessorySlot.HEAD, "Crown", rarity = Rarity.RARE),
          AccessoryItem("antenna", AccessorySlot.HEAD, "Antenna", rarity = Rarity.COMMON),
          // TORSO
          AccessoryItem("none", AccessorySlot.TORSO, "None"),
          AccessoryItem("badge", AccessorySlot.TORSO, "Badge", rarity = Rarity.COMMON),
          AccessoryItem("scarf", AccessorySlot.TORSO, "Scarf", rarity = Rarity.RARE),
          AccessoryItem("armor", AccessorySlot.TORSO, "Armor", rarity = Rarity.EPIC),
          // LEGS
          AccessoryItem("none", AccessorySlot.LEGS, "None"),
          AccessoryItem("boots", AccessorySlot.LEGS, "Boots", rarity = Rarity.COMMON),
          AccessoryItem("rocket", AccessorySlot.LEGS, "Rocket", rarity = Rarity.LEGENDARY),
          AccessoryItem("skates", AccessorySlot.LEGS, "Skates", rarity = Rarity.RARE),
      )

  // Accent variant (local, not yet persisted)
  private val accentVariant = MutableStateFlow(AccentVariant.Base)
  val accentVariantFlow: StateFlow<AccentVariant> = accentVariant

  // Effective accent color = base (from profile) + local variant
  val accentEffective: StateFlow<Color> =
      combine(userProfile, accentVariantFlow) { user, variant ->
            applyAccentVariant(Color(user.avatarAccent.toInt()), variant)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentViolet)

  // ---------- Intents (update local profile and push to repository) ----------

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
    val current = _userProfile.value
    val prefixesToClean: List<String> =
        when (slot) {
          AccessorySlot.LEGS -> listOf("legs:", "leg:") // legacy cleanup
          else -> listOf(slot.name.lowercase() + ":")
        }

    val cleaned =
        current.accessories.filterNot { entry ->
          prefixesToClean.any { prefix -> entry.startsWith(prefix) }
        }

    val currentId = equippedId(slot)
    val nextAccessories =
        when {
          id == "none" -> cleaned
          currentId == id -> cleaned
          else -> cleaned + (slot.name.lowercase() + ":" + id)
        }

    val updated = current.copy(accessories = nextAccessories)
    _userProfile.value = updated
    pushProfile(updated)
  }

  fun unequip(slot: AccessorySlot) {
    val current = _userProfile.value
    val prefix = slot.name.lowercase() + ":"
    val updated =
        current.copy(accessories = current.accessories.filterNot { it.startsWith(prefix) })
    _userProfile.value = updated
    pushProfile(updated)
  }

  fun equippedId(slot: AccessorySlot): String? {
    val prefixes: List<String> =
        when (slot) {
          AccessorySlot.LEGS -> listOf("legs:", "leg:")
          else -> listOf(slot.name.lowercase() + ":")
        }

    val entry =
        _userProfile.value.accessories.firstOrNull { value ->
          prefixes.any { prefix -> value.startsWith(prefix) }
        } ?: return null

    return entry.substringAfter(':')
  }

  private fun updateLocal(edit: (UserProfile) -> UserProfile) {
    _userProfile.update(edit)
    pushProfile()
  }

  // ---------- Color helpers ----------

  private fun applyAccentVariant(base: Color, variant: AccentVariant): Color =
      when (variant) {
        AccentVariant.Base -> base
        AccentVariant.Light -> base.blend(Color.White, 0.25f)
        AccentVariant.Dark -> base.blend(Color.Black, 0.25f)
        AccentVariant.Vibrant -> base.boostSaturation(1.2f).boostValue(1.1f)
      }

  private fun Color.blend(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    fun channel(a: Float, b: Float) = a + (b - a) * t
    return Color(
        channel(red, other.red),
        channel(green, other.green),
        channel(blue, other.blue),
        channel(alpha, other.alpha),
    )
  }

  private fun Color.boostSaturation(factor: Float): Color {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val value = max
    val saturation = if (max == 0f) 0f else (max - min) / max
    val newSaturation = (saturation * factor).coerceIn(0f, 1f)
    val scale = if (max == 0f) 1f else (newSaturation / (saturation.takeIf { it > 0f } ?: 1f))
    val nr = ((red - value) * scale) + value
    val ng = ((green - value) * scale) + value
    val nb = ((blue - value) * scale) + value
    return Color(nr, ng, nb, alpha)
  }

  private fun Color.boostValue(factor: Float): Color {
    fun channel(x: Float) = (x * factor).coerceIn(0f, 1f)
    return Color(channel(red), channel(green), channel(blue), alpha)
  }

  /**
   * Adds coins using the shared UserStatsRepository. Coins are no longer directly mutated on
   * UserProfile; profile is kept in sync from stats flow.
   */
  fun addCoins(amount: Int) {
    if (amount <= 0) return
    viewModelScope.launch { userStatsRepository.updateCoins(amount) }
  }

  // --- Repository sync helper ---
  /** Pushes the current or provided profile to the repository. */
  private fun pushProfile(updated: UserProfile = _userProfile.value) {
    viewModelScope.launch { runCatching { repository.updateProfile(updated) } }
  }
}
