package com.android.sample.ui.profile

import LevelingConfig.levelForPoints
import android.content.Context
import android.net.Uri
import android.util.Log
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
import com.android.sample.profile.FirestoreProfileRepository
import com.android.sample.profile.ProfileRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.gamification.ToastNotifier
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// This code has been written partially using A.I (LLM).
class ProfileViewModel(
    private val profileRepository: ProfileRepository = AppRepositories.profileRepository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository,
) : ViewModel() {

  companion object {
    // Global state shared across all instances to handle the multiple-instance bug
    private var globalSyncCount = 0
    private var globalStartupComplete = false
    private var globalLastProcessedPoints = 0
    private var globalLastProcessedCoins = 0
    private var globalJustLeveledUp = false
  }

  // ----- State Flows -----
  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile

  private val _userStats = MutableStateFlow(UserStats())
  val userStats: StateFlow<UserStats> = _userStats

  private val _rewardEvents = MutableSharedFlow<LevelUpRewardUiEvent>()
  val rewardEvents: SharedFlow<LevelUpRewardUiEvent> = _rewardEvents

  private val accentVariant = MutableStateFlow(AccentVariant.Base)
  val accentVariantFlow: StateFlow<AccentVariant> = accentVariant

  // ----- Startup synchronization flags -----
  private var baselineLevel: Int? = null

  // Use global state shared across instances
  private var startupCompleted: Boolean
    get() = globalStartupComplete
    set(value) {
      globalStartupComplete = value
    }

  private var lastProcessedPoints: Int
    get() = globalLastProcessedPoints
    set(value) {
      globalLastProcessedPoints = value
    }

  private var lastProcessedCoins: Int
    get() = globalLastProcessedCoins
    set(value) {
      globalLastProcessedCoins = value
    }

  // Track if we just did a level up to suppress stat gain toasts
  // Must be global to work across multiple instances
  private var justLeveledUp: Boolean
    get() = globalJustLeveledUp
    set(value) {
      globalJustLeveledUp = value
  // Flag pour savoir si le profil a été chargé depuis Firestore
  private var profileLoaded = false

  init {
    viewModelScope.launch {
      profileRepository.profile.collect { profile ->
        Log.d("ProfileViewModel", "Profile from repo: starterId='${profile.starterId}'")
        _userProfile.value = profile
        profileLoaded = true
      }
    }

    viewModelScope.launch {
      if (profileRepository is FirestoreProfileRepository) {
        profileRepository.isLoaded.first { it }
      }

      userStatsRepository.start()
      userStatsRepository.stats.collect { stats ->
        _userStats.value = stats
        if (profileLoaded) {
          syncProfileWithStats(stats)
        }
      }
    }

  // ----- Reward engine -----
  private val rewardEngine = LevelRewardEngine()

  // ----- Theme palette -----
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

  val accentEffective: StateFlow<Color> =
      combine(userProfile, accentVariantFlow) { user, variant ->
            applyAccentVariant(Color(user.avatarAccent.toInt()), variant)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Color(0xFF7C4DFF))

  init {

    viewModelScope.launch {
      userStatsRepository.start()

      profileRepository.profile.collect { remote ->
        val local = _userProfile.value

        if (remote.level < local.level) {
          return@collect
        }

        val corrected =
            if (remote.lastRewardedLevel < remote.level) {
              remote.copy(lastRewardedLevel = remote.level)
            } else {
              remote
            }

        _userProfile.value = corrected
      }
    }

    viewModelScope.launch {
      userStatsRepository.stats.collect { stats ->
        _userStats.value = stats
        syncProfileWithStats(stats)
      }
    }
  }

  // ----- Accessory Management -----
  private val rewardEngine = LevelRewardEngine()

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

  // ----- Theme and Accent Management -----

  fun setAvatarAccent(color: Color) {
    val argb = color.toArgb().toLong()
    _userProfile.update { it.copy(avatarAccent = argb) }
    pushProfile()
  }

  fun setAccentVariant(variant: AccentVariant) {
    accentVariant.value = variant
  }

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

  // ----- Settings Management -----

  fun toggleNotifications() = updateLocal {
    it.copy(notificationsEnabled = !it.notificationsEnabled)
  }

  fun toggleLocation() = updateLocal { it.copy(locationEnabled = !it.locationEnabled) }

  fun toggleFocusMode() = updateLocal { it.copy(focusModeEnabled = !it.focusModeEnabled) }

  // ----- Points and Coins Management -----
  fun setStarter(starterId: String) {
    if (starterId.isBlank()) {
      Log.w("ProfileViewModel", "setStarter: blank id ignored")
      return
    }

    Log.d("ProfileViewModel", "=== setStarter CALLED: '$starterId' ===")

    val updated = _userProfile.value.copy(starterId = starterId)
    _userProfile.value = updated
    profileLoaded = true

    viewModelScope.launch {
      try {
        profileRepository.updateProfile(updated)
        Log.d("ProfileViewModel", "=== setStarter SUCCESS ===")
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "=== setStarter FAILED ===", e)
      }
    }
  }

  fun starterDrawable(): Int {
    return when (_userProfile.value.starterId) {
      "pyromon" -> R.drawable.edumon
      "aquamon" -> R.drawable.edumon2
      "floramon" -> R.drawable.edumon1
      else -> R.drawable.edumon
    }
  }

  fun addCoins(amount: Int) {
    if (amount <= 0) return
    viewModelScope.launch { userStatsRepository.updateCoins(amount) }
  }

  fun addPoints(amount: Int) {
    if (amount <= 0) return
    viewModelScope.launch { userStatsRepository.addPoints(amount) }
  }

  // ----- Level and Rewards Management -----

  private fun computeLevelFromPoints(points: Int): Int = levelForPoints(points)

  fun syncProfileWithStats(stats: UserStats) {
    globalSyncCount++
  private fun pushProfile(updated: UserProfile = _userProfile.value) {

    if (!profileLoaded) {
      Log.d("ProfileViewModel", "pushProfile: skipped (profile not loaded yet)")
      return
    }

    viewModelScope.launch {
      try {
        profileRepository.updateProfile(updated)
        Log.d("ProfileViewModel", "pushProfile: saved")
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "pushProfile failed", e)
      }
    }
  }

    val old = _userProfile.value
    val newPoints = stats.points
    val computed = computeLevelFromPoints(newPoints)

    if (!startupCompleted) {
      val initial =
          old.copy(
              points = newPoints,
              level = computed,
              coins = stats.coins,
              streak = stats.streak,
              lastRewardedLevel = computed,
              studyStats =
                  old.studyStats.copy(
                      totalTimeMin = stats.totalStudyMinutes,
                      dailyGoalMin = old.studyStats.dailyGoalMin))

      _userProfile.value = initial
      pushProfile(initial)

      baselineLevel = computed
      startupCompleted = true
      // Set baseline immediately on first sync
      lastProcessedPoints = newPoints
      lastProcessedCoins = stats.coins

      return
    }

    val safeNewLevel = maxOf(computed, old.level)

    // Calculate deltas BEFORE updating tracking variables
    val pointsDelta = (newPoints - lastProcessedPoints).coerceAtLeast(0)
    val coinsDelta = (stats.coins - lastProcessedCoins).coerceAtLeast(0)

    // NO LEVEL CHANGE
    if (safeNewLevel == old.level) {
      // Update tracking variables
      lastProcessedPoints = newPoints
      lastProcessedCoins = stats.coins

  fun syncProfileWithStats(stats: UserStats) {

    if (!profileLoaded) {
      Log.d("ProfileViewModel", "syncProfileWithStats: skipped (profile not loaded yet)")
      return
    }

    val old = _userProfile.value
    val newPoints = stats.points
    val computedLevel = computeLevelFromPoints(newPoints)

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
      _userProfile.value = updated
      pushProfile(updated)

      // Show toast for stat gains ONLY if:
      // 1. After startup (globalSyncCount > 2)
      // 2. Not right after a level-up (justLeveledUp = false)
      if ((pointsDelta > 0 || coinsDelta > 0) && globalSyncCount > 2 && !justLeveledUp) {
        ToastNotifier.showStatGain(pointsDelta, coinsDelta)
      }

      // Reset level-up flag AFTER we've blocked the follow-up sync
      if (justLeveledUp) {
        justLeveledUp = false
      }

      return
    }

    // LEVEL UP
    justLeveledUp = true

    val candidate = old.copy(points = newPoints, level = safeNewLevel)
    val candidate = old.copy(points = newPoints, level = computedLevel)
    val result = rewardEngine.applyLevelUpRewards(old, candidate)
    val rewarded = result.updatedProfile

    // Persist reward coins to Firestore
    if (result.summary.coinsGranted > 0) {
      viewModelScope.launch { userStatsRepository.updateCoins(result.summary.coinsGranted) }
    }

    // Update tracking - include ALL coins (Pomodoro + level rewards)
    // This way the NEXT sync won't show them again
    lastProcessedPoints = newPoints
    lastProcessedCoins = stats.coins + result.summary.coinsGranted

    val final =
        rewarded.copy(
            coins = stats.coins + result.summary.coinsGranted,
            streak = stats.streak,
            studyStats =
                old.studyStats.copy(
                    totalTimeMin = stats.totalStudyMinutes,
                    dailyGoalMin = old.studyStats.dailyGoalMin))

    _userProfile.value = final
    pushProfile(final)

    // Only show level-up toast (which includes level rewards)
    // Don't show stat gain toast because it would mix Pomodoro + level rewards confusingly
    if (!result.summary.isEmpty && globalSyncCount > 2) {
      val event =
          LevelUpRewardUiEvent.RewardsGranted(newLevel = safeNewLevel, summary = result.summary)
    if (!result.summary.isEmpty) {
      viewModelScope.launch {
        _rewardEvents.emit(event)
        ToastNotifier.showLevelUpEvent(event)
      }
    }
  }

  // ----- Profile Persistence -----

  private fun updateLocal(edit: (UserProfile) -> UserProfile) {
    _userProfile.update(edit)
    pushProfile()
  }

  private fun pushProfile(updated: UserProfile = _userProfile.value) {
    viewModelScope.launch { runCatching { profileRepository.updateProfile(updated) } }
  }

  // ----- ICS Import -----

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
