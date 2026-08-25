package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateNavigation
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateResult
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationScheduleMode
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationScheduleInterval
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationScheduleDraft
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationScheduleValidationException
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationScheduleValidationState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationBookSettings
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationDraft
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationPodcastSettings
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationProviderState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateSubmissionState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.validateLibraryAdministrationDraft
import dev.halim.shelfdroid.core.data.screen.libraryadministration.normalizeLibraryFolderPath
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryAdministrationCreateViewModel
@Inject
constructor(private val repository: LibraryAdministrationCreateContract) : ViewModel() {

  private val _uiState = MutableStateFlow(LibraryAdministrationCreateUiState())
  val uiState: StateFlow<LibraryAdministrationCreateUiState> =
    _uiState
      .onStart { loadProviders(LibraryAdministrationMediaType.BOOK) }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LibraryAdministrationCreateUiState(),
      )

  fun onEvent(event: LibraryAdministrationCreateEvent) {
    when (event) {
      LibraryAdministrationCreateEvent.Load -> loadProviders(_uiState.value.draft.mediaType)
      LibraryAdministrationCreateEvent.RetryProviders -> loadProviders(_uiState.value.draft.mediaType)
      is LibraryAdministrationCreateEvent.SelectMediaType -> selectMediaType(event.mediaType)
      is LibraryAdministrationCreateEvent.UpdateName -> updateDraft { copy(name = event.value) }
      is LibraryAdministrationCreateEvent.SelectIcon -> updateDraft { copy(icon = event.icon) }
      is LibraryAdministrationCreateEvent.SelectProvider -> updateDraft { withProvider(event.providerId) }
      is LibraryAdministrationCreateEvent.UpdateCoverAspectRatio ->
        updateCommonSettings(
          updateBook = { settings -> settings.copy(coverAspectRatio = event.value) },
          updatePodcast = { settings -> settings.copy(coverAspectRatio = event.value) },
        )
      is LibraryAdministrationCreateEvent.UpdateWatcher ->
        updateCommonSettings(
          updateBook = { settings -> settings.copy(disableWatcher = !event.enabled) },
          updatePodcast = { settings -> settings.copy(disableWatcher = !event.enabled) },
        )
      is LibraryAdministrationCreateEvent.UpdateAudiobooksOnly ->
        updateBookSettings { it.copy(audiobooksOnly = event.enabled) }
      is LibraryAdministrationCreateEvent.UpdateSkipMatchingAsin ->
        updateBookSettings { it.copy(skipMatchingMediaWithAsin = event.enabled) }
      is LibraryAdministrationCreateEvent.UpdateSkipMatchingIsbn ->
        updateBookSettings { it.copy(skipMatchingMediaWithIsbn = event.enabled) }
      is LibraryAdministrationCreateEvent.UpdateHideSingleBookSeries ->
        updateBookSettings { it.copy(hideSingleBookSeries = event.enabled) }
      is LibraryAdministrationCreateEvent.UpdateOnlyShowLaterBooks ->
        updateBookSettings { it.copy(onlyShowLaterBooksInContinueSeries = event.enabled) }
      is LibraryAdministrationCreateEvent.UpdateScriptedEpubs ->
        updateBookSettings { it.copy(epubsAllowScriptedContent = event.enabled) }
      is LibraryAdministrationCreateEvent.UpdatePodcastSearchRegion ->
        updatePodcastSettings { it.copy(podcastSearchRegion = event.value) }
      is LibraryAdministrationCreateEvent.SelectFinishThresholdMode ->
        selectFinishThresholdMode(event.mode)
      is LibraryAdministrationCreateEvent.UpdateFinishThresholdValue ->
        updateFinishThresholdValue(event.value)
      is LibraryAdministrationCreateEvent.ToggleMetadataSource ->
        updateDraft { withMetadataSource(event.id, event.enabled) }
      is LibraryAdministrationCreateEvent.MoveMetadataSource ->
        updateDraft { moveMetadataSource(event.id, event.delta) }
      is LibraryAdministrationCreateEvent.ToggleSchedule ->
        updateDraft { copy(schedule = schedule.copy(enabled = event.enabled)) }
      is LibraryAdministrationCreateEvent.SelectScheduleMode ->
        updateSchedule { copy(mode = event.mode) }
      is LibraryAdministrationCreateEvent.SelectScheduleInterval ->
        updateSchedule { copy(simple = simple.copy(interval = event.interval)) }
      is LibraryAdministrationCreateEvent.UpdateScheduleHour ->
        updateSchedule { copy(simple = simple.copy(hour = event.value)) }
      is LibraryAdministrationCreateEvent.UpdateScheduleMinute ->
        updateSchedule { copy(simple = simple.copy(minute = event.value)) }
      is LibraryAdministrationCreateEvent.ToggleScheduleWeekday ->
        updateSchedule {
          copy(
            simple =
              simple.copy(
                weekdays =
                  if (event.selected) simple.weekdays + event.weekday
                  else simple.weekdays - event.weekday
              )
          )
        }
      is LibraryAdministrationCreateEvent.UpdateAdvancedScheduleCron ->
        updateSchedule { copy(advancedCronExpression = event.value) }
      LibraryAdministrationCreateEvent.ValidateSchedule -> validateSchedule()
      is LibraryAdministrationCreateEvent.SelectTab ->
        _uiState.update { it.copy(selectedTab = event.tab) }
      is LibraryAdministrationCreateEvent.UpdateManualFolder ->
        _uiState.update { it.copy(manualFolderDraft = event.value, isDirty = true) }
      LibraryAdministrationCreateEvent.AddManualFolder -> addManualFolder()
      is LibraryAdministrationCreateEvent.SelectFolder -> addFolder(event.path)
      is LibraryAdministrationCreateEvent.RemoveFolder ->
        updateDraft { copy(folders = folders.filterNot { it == event.path }) }
      LibraryAdministrationCreateEvent.OpenFilesystem -> browseFilesystem(null)
      is LibraryAdministrationCreateEvent.OpenFilesystemPath -> browseFilesystem(event.path)
      LibraryAdministrationCreateEvent.CloseFilesystem ->
        _uiState.update { it.copy(filesystemState = LibraryAdministrationFilesystemState.Closed) }
      LibraryAdministrationCreateEvent.Submit -> submit()
      LibraryAdministrationCreateEvent.RetryLocalSynchronization -> retryLocalSynchronization()
      LibraryAdministrationCreateEvent.Back -> handleBack()
      LibraryAdministrationCreateEvent.ConfirmDiscard ->
        _uiState.update {
          it.copy(navigation = LibraryAdministrationCreateNavigation.Back, discardDialog = false)
        }
      LibraryAdministrationCreateEvent.CancelDiscard ->
        _uiState.update { it.copy(discardDialog = false) }
      LibraryAdministrationCreateEvent.ConsumeNavigation ->
        _uiState.update { it.copy(navigation = null) }
      LibraryAdministrationCreateEvent.ConsumeFocus ->
        _uiState.update { it.copy(focusField = null) }
    }
  }

  private fun selectMediaType(mediaType: LibraryAdministrationMediaType) {
    if (mediaType == _uiState.value.draft.mediaType) return
    _uiState.update {
      it.copy(
        draft = it.draft.withMediaType(mediaType),
        providerState = LibraryAdministrationProviderState.Loading,
        selectedTab =
          if (mediaType == LibraryAdministrationMediaType.PODCAST &&
              it.selectedTab == LibraryAdministrationCreateTab.SCANNER) {
            LibraryAdministrationCreateTab.DETAILS
          } else {
            it.selectedTab
          },
        validation = it.validation.copy(errors = it.validation.errors - LibraryAdministrationCreateField.PROVIDER),
        scheduleValidation = LibraryAdministrationScheduleValidationState.Idle,
        isDirty = true,
      )
    }
    loadProviders(mediaType)
  }

  private fun loadProviders(mediaType: LibraryAdministrationMediaType) {
    if (_uiState.value.isSubmitting) return
    _uiState.update { it.copy(providerState = LibraryAdministrationProviderState.Loading) }
    viewModelScope.launch {
      repository.loadLibraryProviders(mediaType).fold(
        onSuccess = { providers ->
          _uiState.update { state ->
            if (state.draft.mediaType != mediaType) state
            else {
              val selected = state.draft.provider
              val provider = selected?.takeIf { value -> providers.any { it.id == value } } ?: providers.firstOrNull()?.id
              val draft = state.draft.withProvider(provider)
              state.copy(
                draft = draft,
                providerState = LibraryAdministrationProviderState.Success(providers),
                scheduleValidation =
                  if (draft == state.draft) state.scheduleValidation
                  else LibraryAdministrationScheduleValidationState.Idle,
              )
            }
          }
        },
        onFailure = { _ ->
          _uiState.update { state ->
            if (state.draft.mediaType == mediaType) {
              state.copy(providerState = LibraryAdministrationProviderState.Failure(null))
            } else state
          }
        },
      )
    }
  }

  private fun browseFilesystem(path: String?) {
    if (_uiState.value.isSubmitting) return
    _uiState.update { it.copy(filesystemState = LibraryAdministrationFilesystemState.Loading(path)) }
    viewModelScope.launch {
      repository.browseLibraryFilesystem(path).fold(
        onSuccess = { filesystem ->
          _uiState.update {
            it.copy(filesystemState = LibraryAdministrationFilesystemState.Success(path, filesystem))
          }
        },
        onFailure = { _ ->
          _uiState.update {
            it.copy(filesystemState = LibraryAdministrationFilesystemState.Failure(path, null))
          }
        },
      )
    }
  }

  private fun addManualFolder() {
    val path = normalizeLibraryFolderPath(_uiState.value.manualFolderDraft)
    if (path.isBlank()) return
    addFolder(path)
    _uiState.update { it.copy(manualFolderDraft = "") }
  }

  private fun addFolder(path: String) {
    val normalized = normalizeLibraryFolderPath(path)
    if (normalized.isBlank() || _uiState.value.draft.folders.contains(normalized)) return
    updateDraft { copy(folders = folders + normalized) }
  }

  private fun updateDraft(update: LibraryAdministrationDraft.() -> LibraryAdministrationDraft) {
    _uiState.update {
      it.copy(
        draft = update(it.draft),
        isDirty = true,
        validation = it.validation.copy(errors = emptyMap()),
        scheduleValidation = LibraryAdministrationScheduleValidationState.Idle,
      )
    }
  }

  private fun updateSchedule(
    update: LibraryAdministrationScheduleDraft.() -> LibraryAdministrationScheduleDraft
  ) {
    updateDraft { copy(schedule = update(schedule)) }
  }

  private fun updateCommonSettings(
    updateBook: (LibraryAdministrationBookSettings) -> LibraryAdministrationBookSettings,
    updatePodcast: (LibraryAdministrationPodcastSettings) -> LibraryAdministrationPodcastSettings,
  ) {
    _uiState.update {
      it.copy(
        draft =
          if (it.draft.mediaType == LibraryAdministrationMediaType.BOOK) {
            it.draft.copy(bookSettings = updateBook(it.draft.bookSettings))
          } else {
            it.draft.copy(podcastSettings = updatePodcast(it.draft.podcastSettings))
          },
        isDirty = true,
        validation = it.validation.copy(errors = emptyMap()),
        scheduleValidation = LibraryAdministrationScheduleValidationState.Idle,
      )
    }
  }

  private fun updateBookSettings(update: (LibraryAdministrationBookSettings) -> LibraryAdministrationBookSettings) {
    _uiState.update {
      it.copy(
        draft = it.draft.copy(bookSettings = update(it.draft.bookSettings)),
        isDirty = true,
        validation = it.validation.copy(errors = emptyMap()),
        scheduleValidation = LibraryAdministrationScheduleValidationState.Idle,
      )
    }
  }

  private fun updatePodcastSettings(
    update: (LibraryAdministrationPodcastSettings) -> LibraryAdministrationPodcastSettings
  ) {
    _uiState.update {
      it.copy(
        draft = it.draft.copy(podcastSettings = update(it.draft.podcastSettings)),
        isDirty = true,
        validation = it.validation.copy(errors = emptyMap()),
        scheduleValidation = LibraryAdministrationScheduleValidationState.Idle,
      )
    }
  }

  private fun selectFinishThresholdMode(mode: LibraryAdministrationFinishThresholdMode) {
    if (_uiState.value.draft.mediaType == LibraryAdministrationMediaType.BOOK) {
      updateBookSettings {
        when (mode) {
          LibraryAdministrationFinishThresholdMode.TIME_REMAINING ->
            it.copy(
              markAsFinishedPercentComplete = null,
              markAsFinishedTimeRemaining =
                it.markAsFinishedTimeRemaining
                  ?: it.markAsFinishedPercentComplete
                  ?: dev.halim.shelfdroid.core.data.screen.libraryadministration.DEFAULT_FINISH_TIME_REMAINING,
            )
          LibraryAdministrationFinishThresholdMode.PERCENT_COMPLETE ->
            it.copy(
              markAsFinishedPercentComplete =
                it.markAsFinishedPercentComplete
                  ?: it.markAsFinishedTimeRemaining
                  ?: dev.halim.shelfdroid.core.data.screen.libraryadministration.DEFAULT_FINISH_TIME_REMAINING,
              markAsFinishedTimeRemaining = null,
            )
        }
      }
    } else {
      updatePodcastSettings {
        when (mode) {
          LibraryAdministrationFinishThresholdMode.TIME_REMAINING ->
            it.copy(
              markAsFinishedPercentComplete = null,
              markAsFinishedTimeRemaining =
                it.markAsFinishedTimeRemaining
                  ?: it.markAsFinishedPercentComplete
                  ?: dev.halim.shelfdroid.core.data.screen.libraryadministration.DEFAULT_FINISH_TIME_REMAINING,
            )
          LibraryAdministrationFinishThresholdMode.PERCENT_COMPLETE ->
            it.copy(
              markAsFinishedPercentComplete =
                it.markAsFinishedPercentComplete
                  ?: it.markAsFinishedTimeRemaining
                  ?: dev.halim.shelfdroid.core.data.screen.libraryadministration.DEFAULT_FINISH_TIME_REMAINING,
              markAsFinishedTimeRemaining = null,
            )
        }
      }
    }
  }

  private fun updateFinishThresholdValue(value: Int) {
    if (_uiState.value.draft.mediaType == LibraryAdministrationMediaType.BOOK) {
      updateBookSettings {
        if (it.markAsFinishedPercentComplete != null) {
          it.copy(markAsFinishedPercentComplete = value.coerceAtLeast(0))
        } else {
          it.copy(markAsFinishedTimeRemaining = value.coerceAtLeast(0))
        }
      }
    } else {
      updatePodcastSettings {
        if (it.markAsFinishedPercentComplete != null) {
          it.copy(markAsFinishedPercentComplete = value.coerceAtLeast(0))
        } else {
          it.copy(markAsFinishedTimeRemaining = value.coerceAtLeast(0))
        }
      }
    }
  }

  private fun submit() {
    val state = _uiState.value
    if (state.isBusy) return
    val validation =
      validateLibraryAdministrationDraft(
        draft = state.draft,
        providers = (state.providerState as? LibraryAdministrationProviderState.Success)?.providers,
      )
    if (!validation.isValid) {
      _uiState.update {
        it.copy(
          selectedTab =
            when (validation.firstInvalidField) {
              LibraryAdministrationCreateField.SETTINGS_FINISH_THRESHOLD ->
                LibraryAdministrationCreateTab.SETTINGS
              LibraryAdministrationCreateField.SCANNER_PRECEDENCE ->
                LibraryAdministrationCreateTab.SCANNER
              LibraryAdministrationCreateField.SCHEDULE ->
                LibraryAdministrationCreateTab.SCHEDULE
              else -> LibraryAdministrationCreateTab.DETAILS
            },
          validation = validation,
          focusField = validation.firstInvalidField,
        )
      }
      return
    }
    val schedule = state.draft.schedule
    val scheduleExpression = schedule.cronExpression
    if (schedule.enabled && scheduleExpression == null) {
      _uiState.update {
        it.copy(
          selectedTab = LibraryAdministrationCreateTab.SCHEDULE,
          validation =
            it.validation.copy(
              errors =
                it.validation.errors +
                  (LibraryAdministrationCreateField.SCHEDULE to
                    listOf(LibraryAdministrationCreateError.SCHEDULE_INVALID))
            ),
          focusField = LibraryAdministrationCreateField.SCHEDULE,
        )
      }
      return
    }
    if (schedule.enabled && schedule.mode == LibraryAdministrationScheduleMode.Advanced) {
      if (state.scheduleValidation is LibraryAdministrationScheduleValidationState.Valid) {
        createLibrary(state.draft)
      } else {
        validateAdvancedSchedule(state, createOnSuccess = true)
      }
      return
    }
    _uiState.update {
      it.copy(scheduleValidation = LibraryAdministrationScheduleValidationState.Valid)
    }
    createLibrary(state.draft)
  }

  private fun validateSchedule() {
    val state = _uiState.value
    if (state.isBusy) return
    val schedule = state.draft.schedule
    if (!schedule.enabled || schedule.mode != LibraryAdministrationScheduleMode.Advanced) return
    val localValidation = schedule.localValidationMessage()
    if (localValidation != null) {
      val error =
        if (localValidation == "Enter a five-field cron expression.") {
          LibraryAdministrationCreateError.SCHEDULE_REQUIRED
        } else {
          LibraryAdministrationCreateError.SCHEDULE_INVALID
        }
      _uiState.update {
        it.copy(
          selectedTab = LibraryAdministrationCreateTab.SCHEDULE,
          scheduleValidation =
            LibraryAdministrationScheduleValidationState.Invalid(localValidation),
          validation =
            it.validation.copy(
              errors = it.validation.errors +
                (LibraryAdministrationCreateField.SCHEDULE to listOf(error))
            ),
          focusField = LibraryAdministrationCreateField.SCHEDULE,
        )
      }
      return
    }
    validateAdvancedSchedule(state, createOnSuccess = false)
  }

  private fun validateAdvancedSchedule(
    state: LibraryAdministrationCreateUiState,
    createOnSuccess: Boolean,
  ) {
    val expression = state.draft.schedule.cronExpression ?: return
    _uiState.update {
      it.copy(
        scheduleValidation = LibraryAdministrationScheduleValidationState.Validating,
        validation = it.validation.copy(errors = it.validation.errors - LibraryAdministrationCreateField.SCHEDULE),
      )
    }
    viewModelScope.launch {
      repository.validateLibrarySchedule(expression).fold(
        onSuccess = {
          if (_uiState.value.draft != state.draft) return@fold
          _uiState.update {
            it.copy(scheduleValidation = LibraryAdministrationScheduleValidationState.Valid)
          }
          if (createOnSuccess) createLibrary(state.draft)
        },
        onFailure = { error ->
          if (_uiState.value.draft != state.draft) return@fold
          val unavailable =
            error is LibraryAdministrationScheduleValidationException.Unavailable ||
              error !is LibraryAdministrationScheduleValidationException.Invalid
          val validationState =
            if (unavailable) {
              LibraryAdministrationScheduleValidationState.Unavailable(error.message)
            } else {
              LibraryAdministrationScheduleValidationState.Invalid(error.message)
            }
          _uiState.update {
            it.copy(
              selectedTab = LibraryAdministrationCreateTab.SCHEDULE,
              scheduleValidation = validationState,
              validation =
                it.validation.copy(
                  errors =
                    it.validation.errors +
                      (LibraryAdministrationCreateField.SCHEDULE to
                        listOf(
                          if (unavailable) {
                            LibraryAdministrationCreateError.SCHEDULE_VALIDATION_UNAVAILABLE
                          } else {
                            LibraryAdministrationCreateError.SCHEDULE_INVALID
                          }
                        ))
                ),
              focusField = LibraryAdministrationCreateField.SCHEDULE,
            )
          }
        },
      )
    }
  }

  private fun createLibrary(draft: LibraryAdministrationDraft) {
    if (_uiState.value.isSubmitting) return
    _uiState.update {
      it.copy(submissionState = LibraryAdministrationCreateSubmissionState.Submitting)
    }
    viewModelScope.launch {
      repository.createLibrary(draft).fold(
        onSuccess = { result ->
          _uiState.update {
            when (result) {
              is LibraryAdministrationCreateResult.Created ->
                it.copy(
                  submissionState = LibraryAdministrationCreateSubmissionState.Idle,
                  navigation = LibraryAdministrationCreateNavigation.Created(result.library),
                )
              is LibraryAdministrationCreateResult.CreatedButNotSynchronized ->
                it.copy(
                  submissionState =
                    LibraryAdministrationCreateSubmissionState.LocalSyncFailure(
                      result.library,
                      null,
                    )
                )
            }
          }
        },
        onFailure = { _ ->
          _uiState.update {
            it.copy(
              submissionState = LibraryAdministrationCreateSubmissionState.ServerFailure(null)
            )
          }
        },
      )
    }
  }

  private fun retryLocalSynchronization() {
    if (_uiState.value.isSubmitting) return
    val localFailure =
      _uiState.value.submissionState as? LibraryAdministrationCreateSubmissionState.LocalSyncFailure
        ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(submissionState = LibraryAdministrationCreateSubmissionState.Submitting) }
      repository.synchronizeLibraries().fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              submissionState = LibraryAdministrationCreateSubmissionState.Idle,
              navigation = LibraryAdministrationCreateNavigation.Created(localFailure.library),
            )
          }
        },
        onFailure = { _ ->
          _uiState.update {
            it.copy(
              submissionState =
                LibraryAdministrationCreateSubmissionState.LocalSyncFailure(
                  localFailure.library,
                  null,
                )
            )
          }
        },
      )
    }
  }

  private fun handleBack() {
    if (_uiState.value.isDirty) {
      _uiState.update { it.copy(discardDialog = true) }
    } else {
      _uiState.update { it.copy(navigation = LibraryAdministrationCreateNavigation.Back) }
    }
  }
}

sealed interface LibraryAdministrationCreateEvent {
  data object Load : LibraryAdministrationCreateEvent
  data object RetryProviders : LibraryAdministrationCreateEvent
  data class SelectMediaType(val mediaType: LibraryAdministrationMediaType) : LibraryAdministrationCreateEvent
  data class UpdateName(val value: String) : LibraryAdministrationCreateEvent
  data class SelectIcon(val icon: String) : LibraryAdministrationCreateEvent
  data class SelectProvider(val providerId: String) : LibraryAdministrationCreateEvent
  data class UpdateCoverAspectRatio(val value: Int) : LibraryAdministrationCreateEvent
  data class UpdateWatcher(val enabled: Boolean) : LibraryAdministrationCreateEvent
  data class UpdateAudiobooksOnly(val enabled: Boolean) : LibraryAdministrationCreateEvent
  data class UpdateSkipMatchingAsin(val enabled: Boolean) : LibraryAdministrationCreateEvent
  data class UpdateSkipMatchingIsbn(val enabled: Boolean) : LibraryAdministrationCreateEvent
  data class UpdateHideSingleBookSeries(val enabled: Boolean) : LibraryAdministrationCreateEvent
  data class UpdateOnlyShowLaterBooks(val enabled: Boolean) : LibraryAdministrationCreateEvent
  data class UpdateScriptedEpubs(val enabled: Boolean) : LibraryAdministrationCreateEvent
  data class UpdatePodcastSearchRegion(val value: String) : LibraryAdministrationCreateEvent
  data class SelectFinishThresholdMode(val mode: LibraryAdministrationFinishThresholdMode) : LibraryAdministrationCreateEvent
  data class UpdateFinishThresholdValue(val value: Int) : LibraryAdministrationCreateEvent
  data class ToggleMetadataSource(val id: String, val enabled: Boolean) : LibraryAdministrationCreateEvent
  data class MoveMetadataSource(val id: String, val delta: Int) : LibraryAdministrationCreateEvent
  data class ToggleSchedule(val enabled: Boolean) : LibraryAdministrationCreateEvent
  data class SelectScheduleMode(val mode: LibraryAdministrationScheduleMode) : LibraryAdministrationCreateEvent
  data class SelectScheduleInterval(val interval: LibraryAdministrationScheduleInterval) : LibraryAdministrationCreateEvent
  data class UpdateScheduleHour(val value: String) : LibraryAdministrationCreateEvent
  data class UpdateScheduleMinute(val value: String) : LibraryAdministrationCreateEvent
  data class ToggleScheduleWeekday(val weekday: Int, val selected: Boolean) : LibraryAdministrationCreateEvent
  data class UpdateAdvancedScheduleCron(val value: String) : LibraryAdministrationCreateEvent
  data object ValidateSchedule : LibraryAdministrationCreateEvent
  data class SelectTab(val tab: LibraryAdministrationCreateTab) : LibraryAdministrationCreateEvent
  data class UpdateManualFolder(val value: String) : LibraryAdministrationCreateEvent
  data object AddManualFolder : LibraryAdministrationCreateEvent
  data class SelectFolder(val path: String) : LibraryAdministrationCreateEvent
  data class RemoveFolder(val path: String) : LibraryAdministrationCreateEvent
  data object OpenFilesystem : LibraryAdministrationCreateEvent
  data class OpenFilesystemPath(val path: String) : LibraryAdministrationCreateEvent
  data object CloseFilesystem : LibraryAdministrationCreateEvent
  data object Submit : LibraryAdministrationCreateEvent
  data object RetryLocalSynchronization : LibraryAdministrationCreateEvent
  data object Back : LibraryAdministrationCreateEvent
  data object ConfirmDiscard : LibraryAdministrationCreateEvent
  data object CancelDiscard : LibraryAdministrationCreateEvent
  data object ConsumeNavigation : LibraryAdministrationCreateEvent
  data object ConsumeFocus : LibraryAdministrationCreateEvent
}

enum class LibraryAdministrationFinishThresholdMode {
  TIME_REMAINING,
  PERCENT_COMPLETE,
}
