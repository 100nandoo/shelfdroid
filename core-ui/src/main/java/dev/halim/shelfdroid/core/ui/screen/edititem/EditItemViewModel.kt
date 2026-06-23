package dev.halim.shelfdroid.core.ui.screen.edititem

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.download.ManagedDownload
import dev.halim.shelfdroid.core.data.download.ManagedDownloadManager
import dev.halim.shelfdroid.core.data.screen.edititem.DetailsForm
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemLibraryFileDownloadResult
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemLibraryFileDownloadUseCase
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemRepository
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastScheduleMode
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastScheduleSimpleBuilder
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastScheduleSimpleInterval
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemTab
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.data.screen.edititem.LibraryFileRow
import dev.halim.shelfdroid.core.data.screen.edititem.MatchState
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchDraft
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchField
import dev.halim.shelfdroid.core.data.screen.edititem.coerceFor
import dev.halim.shelfdroid.core.data.screen.edititem.deriveSchedulePresentation
import dev.halim.shelfdroid.core.data.screen.edititem.normalized
import dev.halim.shelfdroid.core.data.screen.edititem.toCronExpressionOrNull
import dev.halim.shelfdroid.core.ui.navigation.EditItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EditItemViewModel.Factory::class)
class EditItemViewModel
@AssistedInject
constructor(
  @Assisted navKey: EditItem,
  private val repository: EditItemRepository,
  private val managedDownloadManager: ManagedDownloadManager,
  private val libraryFileDownloadUseCase: EditItemLibraryFileDownloadUseCase,
) : ViewModel() {
  private val itemId = navKey.itemId

  private val _uiState = MutableStateFlow(EditItemUiState(itemId = itemId))
  val uiState: StateFlow<EditItemUiState> =
    _uiState
      .onStart { load() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        EditItemUiState(itemId = itemId),
      )

  private val _events = MutableSharedFlow<GenericUiEvent>()
  val events = _events.asSharedFlow()

  private fun load() {
    viewModelScope.launch { _uiState.value = repository.load(itemId).normalized() }
  }

  fun onEvent(event: EditItemEvent) {
    when (event) {
      is EditItemEvent.ChangeTab ->
        _uiState.update { it.copy(currentTab = event.tab.coerceFor(it.mediaKind)) }

      is EditItemEvent.UpdateDetails ->
        _uiState.update { it.copy(details = event.transform(it.details)) }

      is EditItemEvent.UpdateBookMatch ->
        _uiState.update { repository.updateBookMatch(it, event.transform) }

      is EditItemEvent.UpdatePodcastMatch ->
        _uiState.update { repository.updatePodcastMatch(it, event.transform) }

      EditItemEvent.Save -> save()
      EditItemEvent.SaveSchedule -> saveSchedule()
      EditItemEvent.QuickMatch -> quickMatch()
      EditItemEvent.ReScan -> reScan()
      is EditItemEvent.UploadCover -> uploadCover(event.uri, event.contentResolver)
      is EditItemEvent.SetCoverUrl -> setCoverUrl(event.url)
      EditItemEvent.DeleteCover -> deleteCover()
      EditItemEvent.RunMatchSearch -> runMatchSearch()
      is EditItemEvent.ApplyBookMatchResult ->
        _uiState.update { repository.applyBookMatch(it, event.index) }

      is EditItemEvent.OpenPodcastMatchReview ->
        _uiState.update { repository.openPodcastMatchReview(it, event.index) }

      EditItemEvent.DismissPodcastMatchReview ->
        _uiState.update { repository.dismissPodcastMatchReview(it) }

      is EditItemEvent.TogglePodcastMatchField ->
        _uiState.update { repository.togglePodcastMatchField(it, event.field) }

      is EditItemEvent.UpdatePodcastMatchDraft ->
        _uiState.update { repository.updatePodcastMatchDraft(it, event.transform) }

      EditItemEvent.ApplyPodcastMatchReview -> applyPodcastMatchReview()

      is EditItemEvent.UpdateCoverSearchProvider ->
        _uiState.update { it.copy(coverSearch = it.coverSearch.copy(provider = event.provider)) }

      is EditItemEvent.UpdateCoverSearchTitle ->
        _uiState.update { it.copy(coverSearch = it.coverSearch.copy(title = event.title)) }

      is EditItemEvent.UpdateCoverSearchAuthor ->
        _uiState.update { it.copy(coverSearch = it.coverSearch.copy(author = event.author)) }

      EditItemEvent.RunCoverSearch -> runCoverSearch()
      EditItemEvent.EmbedMetadata -> embedMetadata()
      is EditItemEvent.DownloadLibraryFile -> downloadLibraryFile(event.ino)
      is EditItemEvent.PromptDeleteLibraryFile ->
        _uiState.update { it.copy(pendingDeleteFile = event.file) }

      EditItemEvent.DismissDeleteLibraryFile ->
        _uiState.update { it.copy(pendingDeleteFile = null, activeFileActionIno = null) }

      EditItemEvent.ConfirmDeleteLibraryFile -> deleteLibraryFile()
      is EditItemEvent.UpdateEpisodeCutoffMillis ->
        _uiState.update {
          it.copy(episodeUpdate = it.episodeUpdate.copy(selectedCutoffMillis = event.value))
        }

      is EditItemEvent.UpdateEpisodeLimitInput ->
        _uiState.update { it.copy(episodeUpdate = it.episodeUpdate.copy(limitInput = event.value)) }

      is EditItemEvent.UpdateScheduleEnabled ->
        _uiState.update {
          it.copy(
            schedule = it.schedule.copy(autoDownloadEpisodes = event.value),
            scheduleCronError = null,
          )
        }

      is EditItemEvent.ChangeScheduleMode ->
        _uiState.update { state ->
          val schedulePresentation =
            deriveSchedulePresentation(
              schedule = state.schedule,
              preferredMode = event.mode,
              currentBuilder = state.simpleScheduleBuilder,
            )
          state.copy(
            scheduleMode = schedulePresentation.mode,
            simpleScheduleBuilder = schedulePresentation.simpleBuilder,
            scheduleCronError = null,
          )
        }

      is EditItemEvent.UpdateScheduleCronExpression ->
        _uiState.update {
          it.copy(
            schedule = it.schedule.copy(cronExpression = event.value),
            scheduleCronError = null,
          )
        }

      is EditItemEvent.UpdateSimpleScheduleInterval ->
        _uiState.update { updateSimpleScheduleBuilder(it) { builder ->
          builder.copy(interval = event.interval)
        } }

      is EditItemEvent.UpdateSimpleScheduleHour ->
        _uiState.update { updateSimpleScheduleBuilder(it) { builder ->
          builder.copy(selectedHour = event.value.filter(Char::isDigit).take(2))
        } }

      is EditItemEvent.UpdateSimpleScheduleMinute ->
        _uiState.update { updateSimpleScheduleBuilder(it) { builder ->
          builder.copy(selectedMinute = event.value.filter(Char::isDigit).take(2))
        } }

      is EditItemEvent.ToggleSimpleScheduleWeekday ->
        _uiState.update { updateSimpleScheduleBuilder(it) { builder ->
          val weekdays =
            if (event.weekday in builder.selectedWeekdays) builder.selectedWeekdays - event.weekday
            else builder.selectedWeekdays + event.weekday
          builder.copy(selectedWeekdays = weekdays)
        } }

      is EditItemEvent.UpdateScheduleMaxEpisodesToKeepInput ->
        _uiState.update {
          it.copy(
            schedule = it.schedule.copy(maxEpisodesToKeepInput = event.value),
            scheduleCronError = null,
          )
        }

      is EditItemEvent.UpdateScheduleMaxNewEpisodesToDownloadInput ->
        _uiState.update {
          it.copy(
            schedule = it.schedule.copy(maxNewEpisodesToDownloadInput = event.value),
            scheduleCronError = null,
          )
        }

      EditItemEvent.RunEpisodeUpdateCheck -> runEpisodeUpdateCheck()
    }
  }

  private fun save() = viewModelScope.launch {
    _uiState.update { it.copy(isSaving = true) }
    val result = repository.save(_uiState.value, _events).normalized()
    _uiState.value = result
  }

  private fun saveSchedule() = viewModelScope.launch {
    _uiState.update { it.copy(isSaving = true) }
    _uiState.value = repository.saveSchedule(_uiState.value, _events).normalized()
  }

  private fun quickMatch() = viewModelScope.launch {
    _uiState.update { it.copy(isSaving = true) }
    _uiState.value = repository.quickMatch(_uiState.value, _events).normalized()
  }

  private fun reScan() = viewModelScope.launch {
    _uiState.update { it.copy(isSaving = true) }
    _uiState.value = repository.reScan(_uiState.value, _events).normalized()
  }

  private fun uploadCover(uri: Uri, contentResolver: ContentResolver) = viewModelScope.launch {
    _uiState.update { it.copy(isCoverWorking = true) }
    _uiState.value =
      repository.uploadCover(_uiState.value, uri, contentResolver, _events).normalized()
  }

  private fun setCoverUrl(url: String) = viewModelScope.launch {
    _uiState.update { it.copy(isCoverWorking = true) }
    _uiState.value = repository.setCoverUrl(_uiState.value, url, _events).normalized()
  }

  private fun deleteCover() = viewModelScope.launch {
    _uiState.update { it.copy(isCoverWorking = true) }
    _uiState.value = repository.deleteCover(_uiState.value, _events).normalized()
  }

  private fun runMatchSearch() = viewModelScope.launch {
    _uiState.update {
      it.copy(
        match =
          when (val match = it.match) {
            is MatchState.Book -> match.copy(isSearching = true)
            is MatchState.Podcast -> match.copy(isSearching = true)
          }
      )
    }
    _uiState.value = repository.searchMatches(_uiState.value, _events).normalized()
  }

  private fun applyPodcastMatchReview() = viewModelScope.launch {
    _uiState.update { it.copy(isSaving = true) }
    _uiState.value = repository.applyPodcastMatchReview(_uiState.value, _events).normalized()
  }

  private fun runCoverSearch() = viewModelScope.launch {
    _uiState.update { it.copy(coverSearch = it.coverSearch.copy(state = GenericState.Loading)) }
    _uiState.value = repository.searchCovers(_uiState.value, _events).normalized()
  }

  private fun embedMetadata() = viewModelScope.launch {
    _uiState.update { it.copy(isToolWorking = true) }
    _uiState.value = repository.embedMetadata(_uiState.value, _events).normalized()
  }

  private fun downloadLibraryFile(ino: String) = viewModelScope.launch {
    _uiState.update { it.copy(activeFileActionIno = ino) }
    when (val result = libraryFileDownloadUseCase.prepare(_uiState.value, ino)) {
      is EditItemLibraryFileDownloadResult.Success -> {
        _uiState.value = result.state.normalized()
        _events.emit(GenericUiEvent.RequestManagedDownload(result.download))
      }

      is EditItemLibraryFileDownloadResult.Failure -> {
        _uiState.value = result.state.normalized()
        _events.emit(GenericUiEvent.ShowErrorSnackbar(result.message))
      }
    }
  }

  fun enqueueManagedDownload(download: ManagedDownload) {
    managedDownloadManager.enqueue(download)
  }

  private fun deleteLibraryFile() = viewModelScope.launch {
    val target = _uiState.value.pendingDeleteFile ?: return@launch
    _uiState.update { it.copy(activeFileActionIno = target.ino) }
    _uiState.value = repository.deleteFile(_uiState.value, target.ino, _events).normalized()
  }

  private fun runEpisodeUpdateCheck() = viewModelScope.launch {
    _uiState.update { it.copy(episodeUpdate = it.episodeUpdate.copy(isRunning = true)) }
    _uiState.value = repository.runEpisodeUpdateCheck(_uiState.value, _events).normalized()
  }

  private fun updateSimpleScheduleBuilder(
    state: EditItemUiState,
    transform: (PodcastScheduleSimpleBuilder) -> PodcastScheduleSimpleBuilder,
  ): EditItemUiState {
    val updatedBuilder = transform(state.simpleScheduleBuilder)
    val updatedCron = updatedBuilder.toCronExpressionOrNull()
    return state.copy(
      scheduleMode = PodcastScheduleMode.Simple,
      simpleScheduleBuilder = updatedBuilder,
      schedule =
        state.schedule.copy(
          cronExpression = updatedCron ?: state.schedule.cronExpression,
        ),
      scheduleCronError = null,
    )
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: EditItem): EditItemViewModel
  }
}

sealed interface EditItemEvent {
  data class ChangeTab(val tab: EditItemTab) : EditItemEvent

  data class UpdateDetails(val transform: (DetailsForm) -> DetailsForm) : EditItemEvent

  data class UpdateBookMatch(val transform: (MatchState.Book) -> MatchState.Book) : EditItemEvent

  data class UpdatePodcastMatch(val transform: (MatchState.Podcast) -> MatchState.Podcast) :
    EditItemEvent

  data object Save : EditItemEvent

  data object QuickMatch : EditItemEvent

  data object ReScan : EditItemEvent

  data class UploadCover(val uri: Uri, val contentResolver: ContentResolver) : EditItemEvent

  data class SetCoverUrl(val url: String) : EditItemEvent

  data object DeleteCover : EditItemEvent

  data object RunMatchSearch : EditItemEvent

  data class ApplyBookMatchResult(val index: Int) : EditItemEvent

  data class OpenPodcastMatchReview(val index: Int) : EditItemEvent

  data object DismissPodcastMatchReview : EditItemEvent

  data class TogglePodcastMatchField(val field: PodcastMatchField) : EditItemEvent

  data class UpdatePodcastMatchDraft(val transform: (PodcastMatchDraft) -> PodcastMatchDraft) :
    EditItemEvent

  data object ApplyPodcastMatchReview : EditItemEvent

  data class UpdateCoverSearchProvider(val provider: String) : EditItemEvent

  data class UpdateCoverSearchTitle(val title: String) : EditItemEvent

  data class UpdateCoverSearchAuthor(val author: String) : EditItemEvent

  data object RunCoverSearch : EditItemEvent

  data object EmbedMetadata : EditItemEvent

  data class DownloadLibraryFile(val ino: String) : EditItemEvent

  data class PromptDeleteLibraryFile(val file: LibraryFileRow) : EditItemEvent

  data object ConfirmDeleteLibraryFile : EditItemEvent

  data object DismissDeleteLibraryFile : EditItemEvent

  data class UpdateEpisodeCutoffMillis(val value: Long) : EditItemEvent

  data class UpdateEpisodeLimitInput(val value: String) : EditItemEvent

  data class UpdateScheduleEnabled(val value: Boolean) : EditItemEvent

  data class ChangeScheduleMode(val mode: PodcastScheduleMode) : EditItemEvent

  data class UpdateScheduleCronExpression(val value: String) : EditItemEvent

  data class UpdateSimpleScheduleInterval(val interval: PodcastScheduleSimpleInterval) :
    EditItemEvent

  data class UpdateSimpleScheduleHour(val value: String) : EditItemEvent

  data class UpdateSimpleScheduleMinute(val value: String) : EditItemEvent

  data class ToggleSimpleScheduleWeekday(val weekday: Int) : EditItemEvent

  data class UpdateScheduleMaxEpisodesToKeepInput(val value: String) : EditItemEvent

  data class UpdateScheduleMaxNewEpisodesToDownloadInput(val value: String) : EditItemEvent

  data object SaveSchedule : EditItemEvent

  data object RunEpisodeUpdateCheck : EditItemEvent
}
