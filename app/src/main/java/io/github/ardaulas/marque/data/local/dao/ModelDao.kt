package io.github.ardaulas.marque.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ardaulas.marque.data.local.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ModelDao {
    @Query("SELECT * FROM models WHERE make_id = :makeId ORDER BY name COLLATE NOCASE")
    abstract fun observeByMake(makeId: Int): Flow<List<ModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(models: List<ModelEntity>)

    @Query("DELETE FROM models WHERE make_id = :makeId")
    abstract suspend fun deleteByMake(makeId: Int)

    /** Replaces only the given make's models; other makes' rows are untouched. */
    @Transaction
    open suspend fun replaceForMake(
        makeId: Int,
        models: List<ModelEntity>,
    ) {
        deleteByMake(makeId)
        insertAll(models)
    }
}
