package com.android.sample.pet

import com.android.sample.repos_providors.AppRepositories
import com.android.sample.pet.data.PetRepository
import com.android.sample.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Wires the Profile repository to the Pet repository.
 * Listens to study minutes, coins, and equipped accessories from the profile,
 * and forwards the changes to the pet engine.
 */
object PetController {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var collectJob: Job? = null
    private var startedForUid: String? = null

    suspend fun start(uid: String) {
        if (uid == startedForUid) return
        startedForUid = uid

        val profileRepo: ProfileRepository = AppRepositories.profileRepository
        val petRepo: PetRepository = AppRepositories.petRepository

        petRepo.start(uid)

        collectJob?.cancel()
        collectJob = scope.launch {
            profileRepo.profile.collectLatest { p ->
                // propagate study minutes delta
                val cur = petRepo.state.value
                val added = p.studyStats.totalTimeMin - cur.lastProfileMinutes
                if (added > 0) {
                    petRepo.onStudyCompleted(addedMinutes = added, newStreak = p.streak)
                }

                // propagate coins
                if (p.coins != cur.coins) {
                    petRepo.onCoinsChanged(p.coins)
                }

                // propagate equipment, convert profile accessories to a flat set of ids
                val equippedIds = p.accessories
                    .mapNotNull { it.substringAfter(':', missingDelimiterValue = "") }
                    .filter { it.isNotBlank() }
                    .toSet()
                val auraId = p.accessories.firstOrNull { it.startsWith("head:halo") }?.let { "gold" }

                petRepo.setEquippedAndRecompute(equippedIds, auraId)
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
        collectJob?.cancel()
        scope.cancel()
        startedForUid = null
    }
}
