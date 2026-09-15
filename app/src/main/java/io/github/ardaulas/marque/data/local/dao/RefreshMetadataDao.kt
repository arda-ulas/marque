package io.github.ardaulas.marque.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.ardaulas.marque.data.local.entity.RefreshMetadataEntity

@Dao
interface RefreshMetadataDao {
    @Query("SELECT * FROM refresh_metadata WHERE cache_key = :key")
    suspend fun get(key: String): RefreshMetadataEntity?

    @Upsert
    suspend fun upsert(metadata: RefreshMetadataEntity)
}
