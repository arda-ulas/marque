package io.github.ardaulas.marque.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.ardaulas.marque.data.local.entity.MakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MakeDao {
    @Query("SELECT * FROM makes ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<MakeEntity>>

    @Query("SELECT * FROM makes WHERE id = :id")
    abstract fun observeById(id: Int): Flow<MakeEntity?>

    @Upsert
    abstract suspend fun upsertAll(makes: List<MakeEntity>)

    /**
     * Bound by SQLite's host-parameter limit (999 on the oldest supported API levels). The makes
     * list is ~200 rows today, so a single statement is fine; chunk if that ever changes.
     */
    @Query("DELETE FROM makes WHERE id NOT IN (:ids)")
    abstract suspend fun deleteNotIn(ids: List<Int>)

    @Query("DELETE FROM makes")
    abstract suspend fun deleteAll()

    /**
     * Replace-on-refresh in one transaction: upsert every row from the response, then delete the
     * rows the response no longer contains. Observers see a single emission with the new list.
     */
    @Transaction
    open suspend fun replaceAll(makes: List<MakeEntity>) {
        if (makes.isEmpty()) {
            deleteAll()
        } else {
            upsertAll(makes)
            deleteNotIn(makes.map { it.id })
        }
    }
}
