package io.github.ardaulas.marque.ui.common

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.core.result.errorOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Refresh bookkeeping shared by the list ViewModels: at most one refresh in flight, and the
 * outcome of the most recent one. [State] is updated synchronously when a refresh starts so the
 * first UI state a collector sees is "loading" rather than "empty cache".
 */
class RefreshTracker(
    private val scope: CoroutineScope,
) {
    data class State(
        val isRefreshing: Boolean,
        val error: DataError?,
    )

    private val mutableState = MutableStateFlow(State(isRefreshing = false, error = null))
    val state: StateFlow<State> = mutableState.asStateFlow()
    private var job: Job? = null

    /** Runs [refresh] unless one is already in flight, and records its outcome. */
    fun launch(refresh: suspend () -> Result<Unit, DataError>) {
        if (job?.isActive == true) return
        mutableState.value = State(isRefreshing = true, error = null)
        job =
            scope.launch {
                val result = refresh()
                mutableState.value = State(isRefreshing = false, error = result.errorOrNull())
            }
    }
}
