package com.android.sample.pet

import com.android.sample.pet.data.PetRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Bridges Profile to PetRepository. Applies deltas from study minutes, mirrors coins, and propagates equipment.
 */
object PetController {

    private var scope: CoroutineScope? = null
    private var startedUid: String? = null
    private var lastMinutes: Int = 0
    private var lastEquippedKey: String = ""
    private var lastCoins: Int = -1

    fun start(uid: String) {
        if (startedUid == uid && scope != null) return
        scope?.cancel()
        startedUid = uid
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val profileRepo: ProfileRepository = AppRepositories.profileRepository
        val petRepo: PetRepository = AppRepositories.petRepository

        petRepo.start(uid)

        scope!!.launch {
            profileRepo.profile.collectLatest { profile ->
                val minutes = profile.studyStats.totalTimeMin
                val added = (minutes - lastMinutes).coerceAtLeast(0)
                if (added > 0) {
                    lastMinutes = minutes
                    petRepo.onStudyCompleted(added, profile.streak)
                }

                if (profile.coins != lastCoins) {
                    lastCoins = profile.coins
                    petRepo.onCoinsChanged(profile.coins)
                }

                val equipped = profile.accessories
                    .mapNotNull { s -> s.substringAfter(':', "") }
                    .filter { it.isNotBlank() && it != "none" }
                    .toSet()
                val auraId = equipped.firstOrNull { it.contains("aura", ignoreCase = true) }
                val key = equipped.sorted().joinToString(",") + "|" + (auraId ?: "")
                if (key != lastEquippedKey) {
                    lastEquippedKey = key
                    petRepo.setEquippedAndRecompute(equipped, auraId)
                }
            }
        }
    }

    suspend fun setEquipped(ids: Set<String>, auraId: String?) {
        AppRepositories.petRepository.setEquippedAndRecompute(ids, auraId)
    }

    suspend fun notifyCoinsChanged(newCoins: Int) {
        AppRepositories.petRepository.onCoinsChanged(newCoins)
    }

    fun stop() {
        scope?.cancel()
        scope = null
        startedUid = null
        lastMinutes = 0
        lastEquippedKey = ""
        lastCoins = -1
    }
}
