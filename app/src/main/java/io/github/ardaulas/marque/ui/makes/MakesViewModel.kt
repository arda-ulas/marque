package io.github.ardaulas.marque.ui.makes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ardaulas.marque.data.repository.VehicleRepository
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.ui.common.RefreshTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MakesViewModel
    @Inject
    constructor(
        private val repository: VehicleRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val refreshTracker = RefreshTracker(viewModelScope)

        // Kept in the SavedStateHandle rather than a plain StateFlow so it survives process death.
        private val query: StateFlow<String> = savedStateHandle.getStateFlow(QUERY_KEY, "")

        val uiState: StateFlow<MakesUiState> =
            combine(repository.observeMakes(), query, refreshTracker.state) { makes, query, refresh ->
                toUiState(makes, query, refresh)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = MakesUiState.Loading,
            )

        init {
            // Not forced: a fresh cache is shown as-is without a network round trip.
            refresh(force = false)
        }

        /** User-initiated refresh (pull-to-refresh, Retry): always goes to the network. */
        fun refresh() = refresh(force = true)

        fun setQuery(query: String) {
            savedStateHandle[QUERY_KEY] = query
        }

        private fun refresh(force: Boolean) = refreshTracker.launch { repository.refreshMakes(force = force) }

        private fun toUiState(
            makes: List<Make>,
            query: String,
            refresh: RefreshTracker.State,
        ): MakesUiState =
            when {
                makes.isNotEmpty() -> MakesUiState.Content(makes.matching(query), query, refresh.isRefreshing, refresh.error)
                refresh.isRefreshing -> MakesUiState.Loading
                refresh.error != null -> MakesUiState.Error(refresh.error)
                else -> MakesUiState.Empty
            }

        private fun List<Make>.matching(query: String): List<Make> {
            val needle = query.trim()
            if (needle.isEmpty()) return this
            return filter { it.name.contains(needle, ignoreCase = true) }
        }

        companion object {
            /** SavedStateHandle key of the search query; visible so tests can seed a restored handle. */
            const val QUERY_KEY = "query"
            private const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
