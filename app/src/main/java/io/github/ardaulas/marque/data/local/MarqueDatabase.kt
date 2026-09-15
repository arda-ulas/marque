package io.github.ardaulas.marque.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.ardaulas.marque.data.local.dao.MakeDao
import io.github.ardaulas.marque.data.local.dao.ModelDao
import io.github.ardaulas.marque.data.local.dao.RefreshMetadataDao
import io.github.ardaulas.marque.data.local.entity.MakeEntity
import io.github.ardaulas.marque.data.local.entity.ModelEntity
import io.github.ardaulas.marque.data.local.entity.RefreshMetadataEntity

/**
 * The single source of truth. The schema JSON is exported to `app/schemas` by the Room Gradle
 * plugin and committed, so any future migration has a baseline to diff against.
 */
@Database(
    entities = [MakeEntity::class, ModelEntity::class, RefreshMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MarqueDatabase : RoomDatabase() {
    abstract fun makeDao(): MakeDao

    abstract fun modelDao(): ModelDao

    abstract fun refreshMetadataDao(): RefreshMetadataDao

    companion object {
        const val NAME = "marque.db"
    }
}
