package io.github.ardaulas.marque.data.local

import androidx.room.withTransaction
import io.github.ardaulas.marque.data.local.dao.MakeDao
import io.github.ardaulas.marque.data.local.dao.ModelDao
import io.github.ardaulas.marque.data.local.dao.RefreshMetadataDao
import io.github.ardaulas.marque.data.local.entity.MakeEntity
import io.github.ardaulas.marque.data.local.entity.ModelEntity
import io.github.ardaulas.marque.data.local.entity.RefreshKeys
import io.github.ardaulas.marque.data.local.entity.RefreshMetadataEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The cache as the repository sees it. Each replace call writes the rows and the refresh
 * timestamp atomically, so a crash between the two can never leave the metadata claiming a
 * refresh that did not land.
 */
interface VehicleLocalDataSource {
    fun observeMakes(): Flow<List<MakeEntity>>

    fun observeMake(makeId: Int): Flow<MakeEntity?>

    fun observeModels(makeId: Int): Flow<List<ModelEntity>>

    suspend fun lastRefreshedAt(key: String): Long?

    suspend fun replaceMakes(
        makes: List<MakeEntity>,
        refreshedAtEpochMillis: Long,
    )

    suspend fun replaceModels(
        makeId: Int,
        models: List<ModelEntity>,
        refreshedAtEpochMillis: Long,
    )
}

class RoomVehicleLocalDataSource
    @Inject
    constructor(
        private val database: MarqueDatabase,
        private val makeDao: MakeDao,
        private val modelDao: ModelDao,
        private val refreshMetadataDao: RefreshMetadataDao,
    ) : VehicleLocalDataSource {
        override fun observeMakes(): Flow<List<MakeEntity>> = makeDao.observeAll()

        override fun observeMake(makeId: Int): Flow<MakeEntity?> = makeDao.observeById(makeId)

        override fun observeModels(makeId: Int): Flow<List<ModelEntity>> = modelDao.observeByMake(makeId)

        override suspend fun lastRefreshedAt(key: String): Long? = refreshMetadataDao.get(key)?.refreshedAtEpochMillis

        override suspend fun replaceMakes(
            makes: List<MakeEntity>,
            refreshedAtEpochMillis: Long,
        ) {
            database.withTransaction {
                makeDao.replaceAll(makes)
                refreshMetadataDao.upsert(RefreshMetadataEntity(RefreshKeys.MAKES, refreshedAtEpochMillis))
            }
        }

        override suspend fun replaceModels(
            makeId: Int,
            models: List<ModelEntity>,
            refreshedAtEpochMillis: Long,
        ) {
            database.withTransaction {
                modelDao.replaceForMake(makeId, models)
                refreshMetadataDao.upsert(RefreshMetadataEntity(RefreshKeys.models(makeId), refreshedAtEpochMillis))
            }
        }
    }
