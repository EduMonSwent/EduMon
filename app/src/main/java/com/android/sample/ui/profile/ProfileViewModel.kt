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
import com.android.sample.feature.rewards.LevelRewardEngine
import com.android.sample.profile.ProfileRepository
import com.android.sample.profile.ProfileRepositoryProvider
import com.android.sample.ui.theme.AccentBlue
import com.android.sample.ui.theme.AccentMagenta
import com.android.sample.ui.theme.AccentMint
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.EventColorSports
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

  // ----- Profil LOCAL uniquement -----
  private val _userProfile = MutableStateFlow(repository.profile.value.copy())
  val userProfile: StateFlow<UserProfile> = _userProfile

  // reward engine instance
  private val rewardEngine = LevelRewardEngine()

  // one-shot events for UI (snackbar/dialog/etc.)
  private val _rewardEvents = MutableSharedFlow<LevelUpRewardUiEvent>()
  val rewardEvents: SharedFlow<LevelUpRewardUiEvent> = _rewardEvents

  // Palette issue de ton thème
  val accentPalette: List<Color> =
      listOf(AccentViolet, AccentBlue, AccentMint, EventColorSports, AccentMagenta)

  // Catalogue avec rareté + "None" pour chaque slot (déséquiper)
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

  // Variation non persistée (Firestore plus tard)
  private val accentVariant = MutableStateFlow(AccentVariant.Base)
  val accentVariantFlow: StateFlow<AccentVariant> = accentVariant

  // Couleur d’accent effective = base (profil) + variation (LOCAL)
  val accentEffective: StateFlow<Color> =
      combine(userProfile, accentVariantFlow) { user, variant ->
            applyAccentVariant(Color(user.avatarAccent.toInt()), variant)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentViolet)

  // ---------- Intents (modifient le local, puis tentent de sync repo) ----------

  fun setAvatarAccent(color: Color) {
    val argb = color.toArgb().toLong()
    _userProfile.update { it.copy(avatarAccent = argb) }
    pushProfile() // sync remote
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
    val cur = _userProfile.value
    val prefixesToClean: List<String> =
        when (slot) {
          AccessorySlot.LEGS -> listOf("legs:", "leg:") // legacy clean
          else -> listOf(slot.name.lowercase() + ":")
        }
    val cleaned = cur.accessories.filterNot { s -> prefixesToClean.any { p -> s.startsWith(p) } }
    val currentId = equippedId(slot)
    val next =
        when {
          id == "none" -> cleaned
          currentId == id -> cleaned
          else -> cleaned + (slot.name.lowercase() + ":" + id)
        }
    val updated = cur.copy(accessories = next)
    _userProfile.value = updated
    pushProfile(updated) // sync remote
  }

  fun unequip(slot: AccessorySlot) {
    val cur = _userProfile.value
    val prefix = slot.name.lowercase() + ":"
    val updated = cur.copy(accessories = cur.accessories.filterNot { it.startsWith(prefix) })
    _userProfile.value = updated
    pushProfile(updated) // sync remote
  }

  fun equippedId(slot: AccessorySlot): String? {
    val prefixes: List<String> =
        when (slot) {
          AccessorySlot.LEGS -> listOf("legs:", "leg:")
          else -> listOf(slot.name.lowercase() + ":")
        }
    val entry =
        _userProfile.value.accessories.firstOrNull { s -> prefixes.any { p -> s.startsWith(p) } }
            ?: return null
    return entry.substringAfter(':')
  }

  private fun updateLocal(edit: (UserProfile) -> UserProfile) {
    _userProfile.update(edit)
    pushProfile() // sync remote
  }

  // ---------- Color utils (privées) ----------
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
        ch(red, other.red), ch(green, other.green), ch(blue, other.blue), ch(alpha, other.alpha))
  }

  private fun Color.boostSaturation(f: Float): Color {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val v = max
    val s = if (max == 0f) 0f else (max - min) / max
    val newS = (s * f).coerceIn(0f, 1f)
    val scale = if (max == 0f) 1f else (newS / (s.takeIf { it > 0f } ?: 1f))
    val nr = ((red - v) * scale) + v
    val ng = ((green - v) * scale) + v
    val nb = ((blue - v) * scale) + v
    return Color(nr, ng, nb, alpha)
  }

  private fun Color.boostValue(f: Float): Color {
    fun ch(x: Float) = (x * f).coerceIn(0f, 1f)
    return Color(ch(red), ch(green), ch(blue), alpha)
  }

  fun addCoins(amount: Int) {
    if (amount <= 0) return
    val current = _userProfile.value
    val updated = current.copy(coins = current.coins + amount)
    _userProfile.value = updated
    pushProfile(updated) // sync remote
  }

  /**
   * Adds points to the user, recomputes the level based on total points, and routes the change
   * through the reward engine.
   *
   * If the new level is higher than the old one:
   * - LevelRewardEngine applies rewards
   * - lastRewardedLevel is updated
   * - UI receives a LevelUpRewardUiEvent
   */
  fun addPoints(amount: Int) {
    if (amount <= 0) return

    applyProfileWithPotentialRewards { current ->
      val newPoints = (current.points + amount).coerceAtLeast(0)
      val newLevel = computeLevelFromPoints(newPoints)
      current.copy(points = newPoints, level = newLevel)
    }
  }

  // --- Helpers ---
  /**
   * Pushes the current or provided profile to the repository. Centralizes the fire-and-forget
   * update with error safety.
   */
  private fun pushProfile(updated: UserProfile = _userProfile.value) {
    viewModelScope.launch { runCatching { repository.updateProfile(updated) } }
  }

  /**
   * Applies a change to the profile (via [edit]) and, if that change includes a level increase,
   * routes the old/new profiles through the reward engine.
   * - If level didn't increase → just update and push as usual.
   * - If level increased → apply rewards, update profile, push, and emit UI event.
   */
  private fun applyProfileWithPotentialRewards(edit: (UserProfile) -> UserProfile) {
    val oldProfile = _userProfile.value
    val candidate = edit(oldProfile)

    // No level up → regular path
    if (candidate.level <= oldProfile.level) {
      _userProfile.value = candidate
      pushProfile(candidate)
      return
    }

    // Level increased → let the reward engine do its job
    val result = rewardEngine.applyLevelUpRewards(oldProfile, candidate)
    val updated = result.updatedProfile

    _userProfile.value = updated
    pushProfile(updated)

    // Emit UI event only if something was actually rewarded
    if (!result.summary.isEmpty) {
      viewModelScope.launch {
        _rewardEvents.emit(
            LevelUpRewardUiEvent.RewardsGranted(newLevel = updated.level, summary = result.summary))
      }
    }
  }

  /**
   * Computes the level for a given amount of points.
   *
   * Simple rule:
   * - Every 300 points = +1 level
   * - Minimum level is 1
   */
  private fun computeLevelFromPoints(points: Int): Int {
    if (points <= 0) return 1
    return 1 + (points / 300)
  }
  /**
   * DEBUG / TEST-ONLY helper.
   *
   * This function exists purely to simplify unit testing of the reward system. Not used in
   * production UI. It's kept to make reward-related tests shorter, more deterministic, and easier
   * to maintain (if we later decide to change the level up calculation)
   */
  fun debugLevelUpForTests() {
    applyProfileWithPotentialRewards { current -> current.copy(level = current.level + 1) }
  }
  /**
   * DEBUG / TEST-ONLY helper.
   *
   * This function exists purely to simplify unit testing of the reward system. Not used in
   * production UI. It's kept to make reward-related tests shorter, more deterministic, and easier
   * to maintain (if we later decide to change the level up calculation)
   */
  fun debugNoLevelChangeForTests() {
    applyProfileWithPotentialRewards { current ->
      // Change points, keep level the same
      current.copy(points = current.points + 10)
    }
  }
}
