package io.github.ardaulas.marque.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "makes")
data class MakeEntity(
    @PrimaryKey val id: Int,
    val name: String,
)

/**
 * Models belong to exactly one make. The foreign key cascades so a make that disappears from
 * the makes refresh takes its cached models with it instead of leaving orphans behind.
 */
@Entity(
    tableName = "models",
    foreignKeys = [
        ForeignKey(
            entity = MakeEntity::class,
            parentColumns = ["id"],
            childColumns = ["make_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("make_id")],
)
data class ModelEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "make_id") val makeId: Int,
    val name: String,
)

/**
 * When a cache key was last refreshed successfully. Keys are built by [RefreshKeys]; the
 * repository compares the timestamp against its TTL to decide whether to hit the network.
 */
@Entity(tableName = "refresh_metadata")
data class RefreshMetadataEntity(
    @PrimaryKey @ColumnInfo(name = "cache_key") val key: String,
    @ColumnInfo(name = "refreshed_at_epoch_millis") val refreshedAtEpochMillis: Long,
)

object RefreshKeys {
    const val MAKES = "makes"

    fun models(makeId: Int): String = "models:$makeId"
}
