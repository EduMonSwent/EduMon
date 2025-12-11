package com.android.sample.ui.profile

// This code has been written partially using A.I (LLM).

import LevelingConfig.levelForPoints
import android.content.Context
import android.net.Uri
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
import com.android.sample.feature.schedule.repository.schedule.IcsImporter
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
      userStatsRepository.stats.collect { stats ->
        _userStats.value = stats
        syncProfileWithStats(stats)
      }
    }
  }

  // Palette from theme
  val accentPalette: List<Color> =
      listOf(
          AccentViolet,
          AccentBlue,
          EventColorSports,
          AccentMagenta,
          PurplePrimary,
          AccentMint,
          GlowGold,
          VioletSoft)

  // ----- Profil LOCAL uniquement -----
  // reward engine instance
  private val rewardEngine = LevelRewardEngine()

  private val _rewardEvents = MutableSharedFlow<LevelUpRewardUiEvent>()
  val rewardEvents: SharedFlow<LevelUpRewardUiEvent> = _rewardEvents
  // Accessories catalog

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

  open val accentEffective: StateFlow<Color> =
      combine(userProfile, accentVariantFlow) { user, variant ->
            applyAccentVariant(Color(user.avatarAccent.toInt()), variant)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Color(0xFF7C4DFF))

  open fun accessoryResId(slot: AccessorySlot, id: String): Int {
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

  fun addPoints(amount: Int) {
    if (amount <= 0) return

    viewModelScope.launch {
      // update STATS (source of truth)
      userStatsRepository.addPoints(amount)

      // Profile will update automatically via syncProfileWithStats()
    }
  }

  private fun pushProfile(updated: UserProfile = _userProfile.value) {
    viewModelScope.launch { runCatching { profileRepository.updateProfile(updated) } }
  }

  private fun applyProfileWithPotentialRewards(edit: (UserProfile) -> UserProfile) {
    val oldProfile = _userProfile.value
    val candidate = edit(oldProfile)

    if (candidate.level <= oldProfile.level) {
      _userProfile.value = candidate
      pushProfile(candidate)
      return
    }

    val result = rewardEngine.applyLevelUpRewards(oldProfile, candidate)
    val updated = result.updatedProfile

    _userProfile.value = updated
    pushProfile(updated)

    if (!result.summary.isEmpty) {
      viewModelScope.launch {
        _rewardEvents.emit(
            LevelUpRewardUiEvent.RewardsGranted(newLevel = updated.level, summary = result.summary))
      }
    }
  }

  private fun computeLevelFromPoints(points: Int): Int = levelForPoints(points)

  fun debugLevelUpForTests() {
    applyProfileWithPotentialRewards { current -> current.copy(level = current.level + 1) }
  }

  fun syncProfileWithStats(stats: UserStats) {
    val old = _userProfile.value

    val newPoints = stats.points
    val computedLevel = computeLevelFromPoints(newPoints)

    // If no level change → just sync fields and exit
    if (computedLevel == old.level) {
      val updated =
          old.copy(
              points = newPoints,
              coins = stats.coins,
              streak = stats.streak,
              studyStats =
                  old.studyStats.copy(
                      totalTimeMin = stats.totalStudyMinutes,
                      dailyGoalMin = old.studyStats.dailyGoalMin))
      if (updated != old) {
        _userProfile.value = updated
        pushProfile(updated)
      }
      return
    }

    // ---- LEVEL UP DETECTED ----
    val candidate = old.copy(points = newPoints, level = computedLevel)

    // Apply rewards
    val result = rewardEngine.applyLevelUpRewards(old, candidate)
    val rewardedProfile = result.updatedProfile

    // If rewards include coins, push them to statsRepository
    if (result.summary.coinsGranted > 0) {
      viewModelScope.launch { userStatsRepository.updateCoins(result.summary.coinsGranted) }
    }

    // Final profile = reward result + FIRESTORE stats overlay
    val final =
        rewardedProfile.copy(
            coins = stats.coins + result.summary.coinsGranted,
            streak = stats.streak,
            studyStats =
                old.studyStats.copy(
                    totalTimeMin = stats.totalStudyMinutes,
                    dailyGoalMin = old.studyStats.dailyGoalMin))

    // Update profile
    _userProfile.value = final
    pushProfile(final)

    // Emit level-up snackbar event
    if (!result.summary.isEmpty) {
      viewModelScope.launch {
        _rewardEvents.emit(
            LevelUpRewardUiEvent.RewardsGranted(newLevel = final.level, summary = result.summary))
      }
    }
  }

  fun importIcs(context: Context, uri: Uri) {
    viewModelScope.launch {
      try {
        val stream = context.contentResolver.openInputStream(uri) ?: return@launch
        val importer = IcsImporter(AppRepositories.plannerRepository, context)
        importer.importFromStream(stream)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}
