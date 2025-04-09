package dev.halim.shelfdroid.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity
data class ProgressEntity(
    @PrimaryKey val id: String,
    val libraryItemId: String = "",
    val episodeId: String? = null,
    val mediaItemType: String = "",
    val progress: Float,
    val duration: Float,
    val currentTime: Float,
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progressentity")
    fun all(): List<ProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg entities: ProgressEntity)
}