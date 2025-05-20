package dev.halim.shelfdroid.core.data

import dev.halim.shelfdroid.core.database.Audiobook
import dev.halim.shelfdroid.core.database.AudiobookDao
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AudiobookRepository {
  val audiobooks: Flow<List<String>>

  suspend fun add(name: String)
}

class DefaultAudiobookRepository @Inject constructor(private val audiobookDao: AudiobookDao) :
  AudiobookRepository {

  override val audiobooks: Flow<List<String>> =
    audiobookDao.getAudiobooks().map { items -> items.map { it.name } }

  override suspend fun add(name: String) {
    audiobookDao.insertAudiobook(Audiobook(name = name))
  }
}
