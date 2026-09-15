package io.github.ardaulas.marque.data.remote

import io.github.ardaulas.marque.core.result.DataError
import io.github.ardaulas.marque.core.result.Result
import io.github.ardaulas.marque.data.remote.dto.MakeDto
import io.github.ardaulas.marque.data.remote.dto.ModelDto
import javax.inject.Inject

/** Network access as the repository sees it: typed results, no exceptions, retry already applied. */
interface VehicleRemoteDataSource {
    suspend fun fetchMakes(): Result<List<MakeDto>, DataError>

    suspend fun fetchModels(makeId: Int): Result<List<ModelDto>, DataError>
}

class VpicRemoteDataSource
    @Inject
    constructor(
        private val api: VpicApi,
        private val retryPolicy: RetryPolicy,
    ) : VehicleRemoteDataSource {
        override suspend fun fetchMakes(): Result<List<MakeDto>, DataError> =
            retryPolicy.retryOnce {
                safeCall { api.getMakesForCars().results }
            }

        override suspend fun fetchModels(makeId: Int): Result<List<ModelDto>, DataError> =
            retryPolicy.retryOnce {
                safeCall { api.getModelsForMake(makeId).results }
            }
    }
