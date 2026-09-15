package io.github.ardaulas.marque.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ardaulas.marque.data.repository.VehicleRepository
import io.github.ardaulas.marque.domain.model.Model
import io.github.ardaulas.marque.ui.common.RefreshTracker
import io.github.ardaulas.marque.ui.navigation.ModelsRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel
    @Inject
    constructor(
        private val repository: VehicleRepository,
        // Decoded from the SavedStateHandle by RouteModule, so it survives process death without
        // the ViewModel having to save anything itself.
        route: ModelsRoute,
    ) : ViewModel() {
        private val makeId: Int = route.makeId

        private val refreshTracker = RefreshTracker(viewModelScope)

        val uiState: StateFlow<ModelsUiState> =
            combine(
                repository.observeMake(makeId),
                repository.observeModels(makeId),
                refreshTracker.state,
            ) { make, models, refresh -> toUiState(make?.name, models, refresh) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = ModelsUiState.Loading(makeName = null),
                )

        init {
            // Not forced: a fresh cache for this make is shown as-is without a network round trip.
            refresh(force = false)
        }

        /** User-initiated refresh (pull-to-refresh, Retry): always goes to the network. */
        fun refresh() = refresh(force = true)

        private fun refresh(force: Boolean) = refreshTracker.launch { repository.refreshModels(makeId, force = force) }

        private fun toUiState(
            makeName: String?,
            models: List<Model>,
            refresh: RefreshTracker.State,
        ): ModelsUiState =
            when {
                models.isNotEmpty() -> ModelsUiState.Content(makeName, models, refresh.isRefreshing, refresh.error)
                refresh.isRefreshing -> ModelsUiState.Loading(makeName)
                refresh.error != null -> ModelsUiState.Error(makeName, refresh.error)
                else -> ModelsUiState.Empty(makeName)
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
