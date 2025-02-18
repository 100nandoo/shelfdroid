

package dev.halim.shelfdroid.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import dev.halim.shelfdroid.core.data.DefaultAudiobookRepository
import dev.halim.shelfdroid.core.database.Audiobook
import dev.halim.shelfdroid.core.database.AudiobookDao

/**
 * Unit tests for [DefaultAudiobookRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class) // TODO: Remove when stable
class DefaultAudiobookRepositoryTest {

    @Test
    fun audiobooks_newItemSaved_itemIsReturned() = runTest {
        val repository = DefaultAudiobookRepository(FakeAudiobookDao())

        repository.add("Repository")

        assertEquals(repository.audiobooks.first().size, 1)
    }

}

private class FakeAudiobookDao : AudiobookDao {

    private val data = mutableListOf<Audiobook>()

    override fun getAudiobooks(): Flow<List<Audiobook>> = flow {
        emit(data)
    }

    override suspend fun insertAudiobook(item: Audiobook) {
        data.add(0, item)
    }
}
