package io.github.ardaulas.marque.ui

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.data.repository.VehicleRepository
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.domain.model.Model
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-in for the repository used by the ViewModel tests. The "cache" is a pair of
 * StateFlows; a successful refresh publishes the configured rows into them, the way the real
 * repository's network write shows up through Room.
 */
class FakeVehicleRepository : VehicleRepository {
    val makes = MutableStateFlow<List<Make>>(emptyList())
    val models = MutableStateFlow<Map<Int, List<Model>>>(emptyMap())
    var refreshResult: Result<Unit, DataError> = Result.Success(Unit)

    /** Simulates the network writing the cache: published into [makes] when a refresh succeeds. */
    var makesToPublishOnRefresh: List<Make>? = null

    /** Same for models: published under the refreshed make's id when a refresh succeeds. */
    var modelsToPublishOnRefresh: List<Model>? = null

    val refreshForceCalls = mutableListOf<Boolean>()
    val refreshModelsCalls = mutableListOf<Pair<Int, Boolean>>()

    /** When set, refreshes suspend until it completes, so tests can observe the in-flight state. */
    var refreshGate: CompletableDeferred<Unit>? = null

    override fun observeMakes(): Flow<List<Make>> = makes

    override fun observeMake(makeId: Int): Flow<Make?> = makes.map { list -> list.firstOrNull { it.id == makeId } }

    override fun observeModels(makeId: Int): Flow<List<Model>> = models.map { it[makeId].orEmpty() }

    override suspend fun refreshMakes(force: Boolean): Result<Unit, DataError> {
        refreshForceCalls += force
        refreshGate?.await()
        if (refreshResult is Result.Success) makesToPublishOnRefresh?.let { makes.value = it }
        return refreshResult
    }

    override suspend fun refreshModels(
        makeId: Int,
        force: Boolean,
    ): Result<Unit, DataError> {
        refreshModelsCalls += makeId to force
        refreshGate?.await()
        if (refreshResult is Result.Success) modelsToPublishOnRefresh?.let { models.value = models.value + (makeId to it) }
        return refreshResult
    }
}
