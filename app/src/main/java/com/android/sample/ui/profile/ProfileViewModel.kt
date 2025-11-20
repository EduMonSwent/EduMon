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
import com.android.sample.feature.rewards.LevelRewardEngine
import com.android.sample.profile.ProfileRepository
import com.android.sample.profile.ProfileRepositoryProvider
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.theme.AccentBlue
import com.android.sample.ui.theme.AccentMagenta
import com.android.sample.ui.theme.AccentMint
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.EventColorSports
import com.android.sample.ui.theme.GlowGold
import com.android.sample.ui.theme.PurplePrimary
import com.android.sample.ui.theme.VioletSoft
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
      listOf(
          AccentViolet,
          AccentBlue,
          AccentMint,
          EventColorSports,
          AccentMagenta,
          PurplePrimary,
          AccentBlue,
          AccentMint,
          GlowGold,
          VioletSoft)

  // Accessories catalog
  companion object {
    const val POINTS_PER_LEVEL: Int = 300
  }

  // ----- Profil LOCAL uniquement -----
  // reward engine instance
  private val rewardEngine = LevelRewardEngine()

  // one-shot events for UI (snackbar/dialog/etc.)
  private val _rewardEvents = MutableSharedFlow<LevelUpRewardUiEvent>()
  val rewardEvents: SharedFlow<LevelUpRewardUiEvent> = _rewardEvents

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
    viewModelScope.launch { runCatching { profileRepository.updateProfile(updated) } }
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
    return 1 + (points / POINTS_PER_LEVEL)
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
