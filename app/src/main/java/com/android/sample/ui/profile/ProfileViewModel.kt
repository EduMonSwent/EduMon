// This code was written with the assistance of an AI (LLM).
package com.android.sample.ui.profile

import LevelingConfig.levelForPoints
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessoryItem
import com.android.sample.data.AccessorySlot
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.domain.model.EduMonType
import com.android.sample.domain.ressources.EduMonResourceRegistry
import com.android.sample.feature.rewards.LevelRewardEngine
import com.android.sample.feature.schedule.repository.schedule.IcsExamImporter
import com.android.sample.feature.schedule.repository.schedule.IcsImporter
import com.android.sample.feature.schedule.repository.schedule.KeywordMatcher
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository = AppRepositories.profileRepository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository,
) : ViewModel() {

  private object Constants {
    const val LOG_TAG = "ProfileViewModel"
    const val FLOW_TIMEOUT_MS = 5_000L
    const val STARTUP_SYNC_THRESHOLD = 2
    const val OWNED_PREFIX = "owned:"
    const val SLOT_SEPARATOR = ":"
    const val ACCESSORY_NONE = "none"
    const val MIN_DELTA = 0
    const val MIN_AMOUNT = 0
    const val COLOR_LIGHT_FACTOR = 0.25f
    const val COLOR_DARK_FACTOR = 0.75f
    const val COLOR_VIBRANT_FACTOR = 1.1f
    const val COLOR_MIN = 0f
    const val COLOR_MAX = 1f
    val DEFAULT_ACCENT_COLOR = Color(0xFF7C4DFF)
  }

  private object AccessoryIds {
    const val NONE = "none"
    const val HAT = "hat"
    const val GLASSES = "glasses"
    const val SCARF = "scarf"
    const val CAPE = "cape"
    const val WINGS = "wings"
    const val AURA = "aura"
  }

  private object AccessoryLabels {
    const val NONE = "None"
    const val HAT = "Hat"
    const val GLASSES = "Glasses"
    const val SCARF = "Scarf"
    const val CAPE = "Cape"
    const val WINGS = "Wings"
    const val AURA = "Aura"
  }

  companion object {
    private var globalSyncCount = 0
    private var globalStartupComplete = false
    private var globalLastProcessedPoints = 0
    private var globalLastProcessedCoins = 0
    private var globalJustLeveledUp = false
  }

  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile

  private val _userStats = MutableStateFlow(UserStats())
  val userStats: StateFlow<UserStats> = _userStats

  private val _rewardEvents = MutableSharedFlow<LevelUpRewardUiEvent>()
  val rewardEvents: SharedFlow<LevelUpRewardUiEvent> = _rewardEvents

  private val accentVariant = MutableStateFlow(AccentVariant.Base)
  val accentVariantFlow: StateFlow<AccentVariant> = accentVariant

  val eduMonType: StateFlow<EduMonType> =
      _userProfile
          .map { EduMonType.fromId(it.starterId) }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(Constants.FLOW_TIMEOUT_MS),
              EduMonType.PYROMON)

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

  private var justLeveledUp: Boolean
    get() = globalJustLeveledUp
    set(value) {
      globalJustLeveledUp = value
    }

  private var profileLoaded = false
  private val rewardEngine = LevelRewardEngine()

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
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(Constants.FLOW_TIMEOUT_MS),
              Constants.DEFAULT_ACCENT_COLOR)

  init {
    Log.d(Constants.LOG_TAG, "=== ProfileViewModel INIT ===")

    viewModelScope.launch {
      profileRepository.profile.collect { profile ->
        Log.d(Constants.LOG_TAG, "Profile from repo: starterId='${profile.starterId}'")
        _userProfile.value = profile
        profileLoaded = true
      }
    }

    viewModelScope.launch {
      if (profileRepository is FirestoreProfileRepository) {
        Log.d(Constants.LOG_TAG, "Waiting for Firestore profile to load...")
        profileRepository.isLoaded.first { it }
        Log.d(Constants.LOG_TAG, "Firestore profile loaded")
      }

      userStatsRepository.start()
      userStatsRepository.stats.collect { stats ->
        _userStats.value = stats
        if (profileLoaded) {
          syncProfileWithStats(stats)
        }
      }
    }
  }

  @DrawableRes
  fun starterDrawable(): Int {
    val type = EduMonType.fromId(_userProfile.value.starterId)
    return EduMonResourceRegistry.getBaseDrawable(type)
  }

  @DrawableRes
  fun accessoryResId(slot: AccessorySlot, accessoryId: String): Int {
    val type = EduMonType.fromId(_userProfile.value.starterId)
    return EduMonResourceRegistry.getAccessoryDrawable(type, slot, accessoryId)
  }

  private fun fullCatalog(): List<AccessoryItem> =
      listOf(
          AccessoryItem(AccessoryIds.NONE, AccessorySlot.HEAD, AccessoryLabels.NONE),
          AccessoryItem(AccessoryIds.HAT, AccessorySlot.HEAD, AccessoryLabels.HAT),
          AccessoryItem(AccessoryIds.GLASSES, AccessorySlot.HEAD, AccessoryLabels.GLASSES),
          AccessoryItem(AccessoryIds.NONE, AccessorySlot.TORSO, AccessoryLabels.NONE),
          AccessoryItem(AccessoryIds.SCARF, AccessorySlot.TORSO, AccessoryLabels.SCARF),
          AccessoryItem(AccessoryIds.CAPE, AccessorySlot.TORSO, AccessoryLabels.CAPE),
          AccessoryItem(AccessoryIds.NONE, AccessorySlot.BACK, AccessoryLabels.NONE),
          AccessoryItem(AccessoryIds.WINGS, AccessorySlot.BACK, AccessoryLabels.WINGS),
          AccessoryItem(AccessoryIds.AURA, AccessorySlot.BACK, AccessoryLabels.AURA))

  val accessoryCatalog: List<AccessoryItem>
    get() {
      val owned = ownedIds()
      return fullCatalog().filter { item ->
        item.id == Constants.ACCESSORY_NONE || owned.contains(item.id)
      }
    }

  private fun ownedIds(): Set<String> {
    return _userProfile.value.accessories
        .filter { it.startsWith(Constants.OWNED_PREFIX) }
        .map { it.removePrefix(Constants.OWNED_PREFIX) }
        .toSet()
  }

  fun equip(slot: AccessorySlot, id: String) {
    val owned = ownedIds()
    if (id != Constants.ACCESSORY_NONE && !owned.contains(id)) return

    val cur = _userProfile.value
    val prefix = slot.name.lowercase() + Constants.SLOT_SEPARATOR
    val cleaned = cur.accessories.filterNot { it.startsWith(prefix) }
    val updatedAccessories =
        if (id == Constants.ACCESSORY_NONE) {
          cleaned
        } else {
          cleaned + (prefix + id)
        }
    val updated = cur.copy(accessories = updatedAccessories)

    _userProfile.value = updated
    pushProfile(updated)
  }

  fun unequip(slot: AccessorySlot) {
    val cur = _userProfile.value
    val prefix = slot.name.lowercase() + Constants.SLOT_SEPARATOR
    val updated = cur.copy(accessories = cur.accessories.filterNot { it.startsWith(prefix) })
    _userProfile.value = updated
    pushProfile(updated)
  }

  fun equippedId(slot: AccessorySlot): String? {
    val prefix = slot.name.lowercase() + Constants.SLOT_SEPARATOR
    val entry = _userProfile.value.accessories.firstOrNull { it.startsWith(prefix) } ?: return null
    return entry.removePrefix(prefix)
  }

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
                (base.red + (Constants.COLOR_MAX - base.red) * Constants.COLOR_LIGHT_FACTOR),
                (base.green + (Constants.COLOR_MAX - base.green) * Constants.COLOR_LIGHT_FACTOR),
                (base.blue + (Constants.COLOR_MAX - base.blue) * Constants.COLOR_LIGHT_FACTOR),
                base.alpha)
        AccentVariant.Dark ->
            Color(
                base.red * Constants.COLOR_DARK_FACTOR,
                base.green * Constants.COLOR_DARK_FACTOR,
                base.blue * Constants.COLOR_DARK_FACTOR,
                base.alpha)
        AccentVariant.Vibrant ->
            Color(
                (base.red * Constants.COLOR_VIBRANT_FACTOR).coerceIn(
                    Constants.COLOR_MIN, Constants.COLOR_MAX),
                (base.green * Constants.COLOR_VIBRANT_FACTOR).coerceIn(
                    Constants.COLOR_MIN, Constants.COLOR_MAX),
                (base.blue * Constants.COLOR_VIBRANT_FACTOR).coerceIn(
                    Constants.COLOR_MIN, Constants.COLOR_MAX),
                base.alpha)
      }

  fun toggleNotifications() = updateLocal {
    it.copy(notificationsEnabled = !it.notificationsEnabled)
  }

  fun toggleLocation() = updateLocal { it.copy(locationEnabled = !it.locationEnabled) }

  fun toggleFocusMode() = updateLocal { it.copy(focusModeEnabled = !it.focusModeEnabled) }

  fun setStarter(starterId: String) {
    if (starterId.isBlank()) {
      Log.w(Constants.LOG_TAG, "setStarter: blank id ignored")
      return
    }

    Log.d(Constants.LOG_TAG, "=== setStarter CALLED: '$starterId' ===")

    val updated = _userProfile.value.copy(starterId = starterId)
    _userProfile.value = updated
    profileLoaded = true

    viewModelScope.launch {
      try {
        profileRepository.updateProfile(updated)
        Log.d(Constants.LOG_TAG, "=== setStarter SUCCESS ===")
      } catch (e: Exception) {
        Log.e(Constants.LOG_TAG, "=== setStarter FAILED ===", e)
      }
    }
  }

  fun addCoins(amount: Int) {
    if (amount <= Constants.MIN_AMOUNT) return
    viewModelScope.launch { userStatsRepository.updateCoins(amount) }
  }

  fun addPoints(amount: Int) {
    if (amount <= Constants.MIN_AMOUNT) return
    viewModelScope.launch { userStatsRepository.addPoints(amount) }
  }

  private fun computeLevelFromPoints(points: Int): Int = levelForPoints(points)

  fun syncProfileWithStats(stats: UserStats) {
    globalSyncCount++

    if (!profileLoaded) {
      Log.d(Constants.LOG_TAG, "syncProfileWithStats: skipped (profile not loaded yet)")
      return
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

      startupCompleted = true
      lastProcessedPoints = newPoints
      lastProcessedCoins = stats.coins

      Log.d(Constants.LOG_TAG, "Baseline set: level=$computed, points=$newPoints")
      return
    }

    val safeNewLevel = maxOf(computed, old.level)
    val pointsDelta = (newPoints - lastProcessedPoints).coerceAtLeast(Constants.MIN_DELTA)
    val coinsDelta = (stats.coins - lastProcessedCoins).coerceAtLeast(Constants.MIN_DELTA)

    if (safeNewLevel == old.level) {
      lastProcessedPoints = newPoints
      lastProcessedCoins = stats.coins

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

      val hasGains = pointsDelta > Constants.MIN_DELTA || coinsDelta > Constants.MIN_DELTA
      val afterStartup = globalSyncCount > Constants.STARTUP_SYNC_THRESHOLD
      if (hasGains && afterStartup && !justLeveledUp) {
        ToastNotifier.showStatGain(pointsDelta, coinsDelta)
      }

      if (justLeveledUp) {
        justLeveledUp = false
      }

      return
    }

    justLeveledUp = true

    val candidate = old.copy(points = newPoints, level = safeNewLevel)
    val result = rewardEngine.applyLevelUpRewards(old, candidate)
    val rewarded = result.updatedProfile

    if (result.summary.coinsGranted > Constants.MIN_DELTA) {
      viewModelScope.launch { userStatsRepository.updateCoins(result.summary.coinsGranted) }
    }

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

    if (!result.summary.isEmpty && globalSyncCount > Constants.STARTUP_SYNC_THRESHOLD) {
      val event =
          LevelUpRewardUiEvent.RewardsGranted(newLevel = safeNewLevel, summary = result.summary)
      viewModelScope.launch {
        _rewardEvents.emit(event)
        ToastNotifier.showLevelUpEvent(event)
      }
    }
  }

  private fun updateLocal(edit: (UserProfile) -> UserProfile) {
    _userProfile.update(edit)
    pushProfile()
  }

  private fun pushProfile(updated: UserProfile = _userProfile.value) {
    if (!profileLoaded) {
      Log.d(Constants.LOG_TAG, "pushProfile: skipped (profile not loaded yet)")
      return
    }

    viewModelScope.launch {
      try {
        profileRepository.updateProfile(updated)
        Log.d(Constants.LOG_TAG, "pushProfile: saved")
      } catch (e: Exception) {
        Log.e(Constants.LOG_TAG, "pushProfile failed", e)
      }
    }
  }

  fun importIcs(context: Context, uri: Uri) {
    viewModelScope.launch {
      try {
        val input = context.contentResolver.openInputStream(uri) ?: return@launch
        val bytes = input.readBytes()
        input.close()

        val matcher = KeywordMatcher(context)

        IcsImporter(AppRepositories.plannerRepository, context)
            .importFromStream(bytes.inputStream())

        IcsExamImporter(scheduleRepository = AppRepositories.scheduleRepository, matcher = matcher)
            .importFromStream(bytes.inputStream())
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}
