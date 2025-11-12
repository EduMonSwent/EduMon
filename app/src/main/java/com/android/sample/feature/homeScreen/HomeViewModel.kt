package com.android.sample.feature.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.CreatureStats
import com.android.sample.data.ToDo
import com.android.sample.data.UserProfile
import com.android.sample.pet.model.PetState
import com.android.sample.repos_providors.AppRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.floor


class HomeViewModel(
    private val petFlow: Flow<PetState> = AppRepositories.petRepository.state,
    private val profileFlow: Flow<UserProfile> = AppRepositories.profileRepository.profile,
    private val quoteFlow: Flow<String> = flow { emit(AppRepositories.homeRepository.dailyQuote()) },
    private val todosFlow: Flow<List<ToDo>> = flow { emit(AppRepositories.homeRepository.fetchTodos()) }
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            petFlow.map { it.toCreatureStats() },
            profileFlow,
            quoteFlow,
            todosFlow
        ) { creature, profile, quote, todos ->
            HomeUiState(
                isLoading = false,
                creatureStats = creature,
                userStats = profile,
                quote = quote,
                todos = todos
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState()
        )
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val creatureStats: CreatureStats = CreatureStats(level = 1, energy = 1f, happiness = 0.5f, health = 0f),
    val userStats: UserProfile = UserProfile(),
    val quote: String = "",
    val todos: List<ToDo> = emptyList()
)


private fun PetState.toCreatureStats(): CreatureStats {
    val lvl = floor(this.growth * 10f).toInt().coerceAtLeast(1)
    return CreatureStats(
        level = lvl,
        energy = this.energy.coerceIn(0f, 1f),
        happiness = this.happiness.coerceIn(0f, 1f),
        health = this.growth.coerceIn(0f, 1f)
    )
}
