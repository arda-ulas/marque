package io.github.ardaulas.marque.data.repository

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.domain.model.Model
import kotlinx.coroutines.flow.Flow

/**
 * Reads come from the local cache as observable streams; refreshes pull from the network into
 * that cache and report whether they succeeded. The two never mix: a refresh result carries no
 * data, and a stream never carries an error.
 */
interface VehicleRepository {
    fun observeMakes(): Flow<List<Make>>

    fun observeMake(makeId: Int): Flow<Make?>

    fun observeModels(makeId: Int): Flow<List<Model>>

    /** Refreshes the makes cache. Skipped when the cache is fresh unless [force] is set. */
    suspend fun refreshMakes(force: Boolean = false): Result<Unit, DataError>

    /** Refreshes one make's models. Skipped when that make's cache is fresh unless [force] is set. */
    suspend fun refreshModels(
        makeId: Int,
        force: Boolean = false,
    ): Result<Unit, DataError>
}
