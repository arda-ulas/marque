package io.github.ardaulas.marque.ui.makes

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.domain.model.Make

/**
 * What the makes screen can show. Derived from two inputs, the cached list and the state of the
 * last refresh, so cached data always wins: a failed refresh over a non-empty cache is
 * [Content] with an [Content.error], never [Error].
 */
sealed interface MakesUiState {
    /** No cache yet and a refresh is in flight. */
    data object Loading : MakesUiState

    /** Cached makes; [isRefreshing] and [error] describe the most recent refresh attempt. */
    data class Content(
        val makes: List<Make>,
        val isRefreshing: Boolean,
        val error: DataError?,
    ) : MakesUiState

    /** No cache, no refresh in flight, and the last refresh did not fail (it returned nothing). */
    data object Empty : MakesUiState

    /** No cache and the refresh failed; the only thing to show is the error and a retry. */
    data class Error(
        val error: DataError,
    ) : MakesUiState
}
