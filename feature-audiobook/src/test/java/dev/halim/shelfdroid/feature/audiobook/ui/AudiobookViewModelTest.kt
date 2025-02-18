

package dev.halim.shelfdroid.feature.audiobook.ui.audiobook


import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import dev.halim.shelfdroid.core.data.AudiobookRepository
import dev.halim.shelfdroid.feature.audiobook.ui.AudiobookUiState
import dev.halim.shelfdroid.feature.audiobook.ui.AudiobookViewModel

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@OptIn(ExperimentalCoroutinesApi::class) // TODO: Remove when stable
class AudiobookViewModelTest {
    @Test
    fun uiState_initiallyLoading() = runTest {
        val viewModel = AudiobookViewModel(FakeAudiobookRepository())
        assertEquals(viewModel.uiState.first(), AudiobookUiState.Loading)
    }

    @Test
    fun uiState_onItemSaved_isDisplayed() = runTest {
        val viewModel = AudiobookViewModel(FakeAudiobookRepository())
        assertEquals(viewModel.uiState.first(), AudiobookUiState.Loading)
    }
}

private class FakeAudiobookRepository : AudiobookRepository {

    private val data = mutableListOf<String>()

    override val audiobooks: Flow<List<String>>
        get() = flow { emit(data.toList()) }

    override suspend fun add(name: String) {
        data.add(0, name)
    }
}
