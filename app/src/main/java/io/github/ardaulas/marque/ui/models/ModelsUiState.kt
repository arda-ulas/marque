package io.github.ardaulas.marque.ui.models

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.domain.model.Model

/**
 * What the models screen can show; the same shape as `MakesUiState`, with the make's name
 * carried on every variant because the top bar needs it whatever the list is doing. As on the
 * makes screen, cached rows always win: a failed refresh over a non-empty cache is [Content]
 * with an [Content.error], never [Error].
 */
sealed interface ModelsUiState {
    /** Null until the make row has been read from the cache (or if it was never cached). */
    val makeName: String?

    data class Loading(
        override val makeName: String?,
    ) : ModelsUiState

    data class Content(
        override val makeName: String?,
        val models: List<Model>,
        val isRefreshing: Boolean,
        val error: DataError?,
    ) : ModelsUiState

    data class Empty(
        override val makeName: String?,
    ) : ModelsUiState

    data class Error(
        override val makeName: String?,
        val error: DataError,
    ) : ModelsUiState
}
