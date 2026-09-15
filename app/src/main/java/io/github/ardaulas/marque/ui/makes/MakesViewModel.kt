package io.github.ardaulas.marque.ui.makes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.errorOrNull
import io.github.ardaulas.marque.data.repository.VehicleRepository
import io.github.ardaulas.marque.domain.model.Make
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MakesViewModel
    @Inject
    constructor(
        private val repository: VehicleRepository,
    ) : ViewModel() {
        private data class RefreshState(
            val isRefreshing: Boolean,
            val error: DataError?,
        )

        private val refreshState = MutableStateFlow(RefreshState(isRefreshing = false, error = null))
        private var refreshJob: Job? = null

        val uiState: StateFlow<MakesUiState> =
            combine(repository.observeMakes(), refreshState) { makes, refresh -> toUiState(makes, refresh) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = MakesUiState.Loading,
                )

        init {
            // Not forced: a fresh cache is shown as-is without a network round trip.
            refresh(force = false)
        }

        /** User-initiated refresh (Retry button, later pull-to-refresh): always goes to the network. */
        fun refresh() = refresh(force = true)

        private fun refresh(force: Boolean) {
            if (refreshJob?.isActive == true) return
            // Set synchronously so the first collected state is Loading rather than Empty.
            refreshState.value = RefreshState(isRefreshing = true, error = null)
            refreshJob =
                viewModelScope.launch {
                    val result = repository.refreshMakes(force = force)
                    refreshState.value = RefreshState(isRefreshing = false, error = result.errorOrNull())
                }
        }

        private fun toUiState(
            makes: List<Make>,
            refresh: RefreshState,
        ): MakesUiState =
            when {
                makes.isNotEmpty() -> MakesUiState.Content(makes, refresh.isRefreshing, refresh.error)
                refresh.isRefreshing -> MakesUiState.Loading
                refresh.error != null -> MakesUiState.Error(refresh.error)
                else -> MakesUiState.Empty
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
