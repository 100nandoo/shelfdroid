package dev.halim.shelfdroid.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity
data class Audiobook(val name: String) {
  @PrimaryKey(autoGenerate = true) var uid: Int = 0
}

@Dao
interface AudiobookDao {
  @Query("SELECT * FROM audiobook ORDER BY uid DESC LIMIT 10")
  fun getAudiobooks(): Flow<List<Audiobook>>

  @Insert suspend fun insertAudiobook(item: Audiobook)
}
