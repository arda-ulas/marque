package io.github.ardaulas.marque.data.repository

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.data.local.VehicleLocalDataSource
import io.github.ardaulas.marque.data.local.entity.RefreshKeys
import io.github.ardaulas.marque.data.mapper.toDomain
import io.github.ardaulas.marque.data.mapper.toEntities
import io.github.ardaulas.marque.data.remote.VehicleRemoteDataSource
import io.github.ardaulas.marque.domain.model.Make
import io.github.ardaulas.marque.domain.model.Model
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/**
 * Offline-first: the database is the only thing the UI observes. A refresh fetches from the
 * network and, on success, replaces the cached rows and the refresh timestamp in one
 * transaction. On failure nothing is written, so whatever was cached stays visible and the
 * caller gets the mapped [DataError] to show alongside it.
 *
 * Cache policy: a refresh that is not [forced][refreshMakes] is skipped while the last
 * successful refresh of that key is younger than [CACHE_TTL]. Time comes from an injected
 * [Clock] so the policy is testable without sleeping.
 */
class OfflineFirstVehicleRepository
    @Inject
    constructor(
        private val remote: VehicleRemoteDataSource,
        private val local: VehicleLocalDataSource,
        private val clock: Clock,
    ) : VehicleRepository {
        override fun observeMakes(): Flow<List<Make>> = local.observeMakes().map { entities -> entities.map { it.toDomain() } }

        override fun observeMake(makeId: Int): Flow<Make?> = local.observeMake(makeId).map { it?.toDomain() }

        override fun observeModels(makeId: Int): Flow<List<Model>> =
            local.observeModels(makeId).map { entities -> entities.map { it.toDomain() } }

        override suspend fun refreshMakes(force: Boolean): Result<Unit, DataError> {
            if (!force && isFresh(RefreshKeys.MAKES)) return Result.Success(Unit)
            return when (val fetched = remote.fetchMakes()) {
                is Result.Failure -> fetched
                is Result.Success -> writeCache { local.replaceMakes(fetched.data.toEntities(), clock.millis()) }
            }
        }

        override suspend fun refreshModels(
            makeId: Int,
            force: Boolean,
        ): Result<Unit, DataError> {
            if (!force && isFresh(RefreshKeys.models(makeId))) return Result.Success(Unit)
            return when (val fetched = remote.fetchModels(makeId)) {
                is Result.Failure -> fetched
                is Result.Success -> writeCache { local.replaceModels(makeId, fetched.data.toEntities(makeId), clock.millis()) }
            }
        }

        private suspend fun isFresh(key: String): Boolean {
            val refreshedAt = local.lastRefreshedAt(key) ?: return false
            return clock.millis() - refreshedAt < CACHE_TTL.toMillis()
        }

        /**
         * A cache write failing is a bug (for example a models insert whose make was cascaded
         * away by a concurrent makes refresh), but the repository's contract is that no
         * exception escapes it, so it surfaces as [DataError.Unknown] rather than a crash.
         */
        private suspend inline fun writeCache(crossinline write: suspend () -> Unit): Result<Unit, DataError> =
            try {
                write()
                Result.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.Failure(DataError.Unknown)
            }

        companion object {
            val CACHE_TTL: Duration = Duration.ofHours(24)
        }
    }
