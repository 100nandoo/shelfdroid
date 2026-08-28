package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleDraft
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleInterval
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleMode
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminBookSettings
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateContract
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateNavigation
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateResult
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateSubmissionState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminDraft
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminPodcastSettings
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminProviderState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminScheduleValidationException
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminScheduleValidationState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.normalizeLibraryFolderPath
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.validateLibraryAdminDraft
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryAdminCreateViewModel
@Inject
constructor(private val repository: LibraryAdminCreateContract) : ViewModel() {

  private val _uiState = MutableStateFlow(LibraryAdminCreateUiState())
  val uiState: StateFlow<LibraryAdminCreateUiState> =
    _uiState
      .onStart { loadProviders(MediaType.BOOK) }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LibraryAdminCreateUiState(),
      )

  fun onEvent(event: LibraryAdminCreateEvent) {
    when (event) {
      LibraryAdminCreateEvent.Load -> loadProviders(_uiState.value.draft.mediaType)
      LibraryAdminCreateEvent.RetryProviders -> loadProviders(_uiState.value.draft.mediaType)
      is LibraryAdminCreateEvent.SelectMediaType -> selectMediaType(event.mediaType)
      is LibraryAdminCreateEvent.UpdateName -> updateDraft { copy(name = event.value) }
      is LibraryAdminCreateEvent.SelectIcon -> updateDraft { copy(icon = event.icon) }
      is LibraryAdminCreateEvent.SelectProvider -> updateDraft { withProvider(event.providerId) }
      is LibraryAdminCreateEvent.UpdateCoverAspectRatio ->
        updateCommonSettings(
          updateBook = { settings -> settings.copy(coverAspectRatio = event.value) },
          updatePodcast = { settings -> settings.copy(coverAspectRatio = event.value) },
        )
      is LibraryAdminCreateEvent.UpdateWatcher ->
        updateCommonSettings(
          updateBook = { settings -> settings.copy(disableWatcher = !event.enabled) },
          updatePodcast = { settings -> settings.copy(disableWatcher = !event.enabled) },
        )
      is LibraryAdminCreateEvent.UpdateAudiobooksOnly ->
        updateBookSettings { it.copy(audiobooksOnly = event.enabled) }
      is LibraryAdminCreateEvent.UpdateSkipMatchingAsin ->
        updateBookSettings { it.copy(skipMatchingMediaWithAsin = event.enabled) }
      is LibraryAdminCreateEvent.UpdateSkipMatchingIsbn ->
        updateBookSettings { it.copy(skipMatchingMediaWithIsbn = event.enabled) }
      is LibraryAdminCreateEvent.UpdateHideSingleBookSeries ->
        updateBookSettings { it.copy(hideSingleBookSeries = event.enabled) }
      is LibraryAdminCreateEvent.UpdateOnlyShowLaterBooks ->
        updateBookSettings { it.copy(onlyShowLaterBooksInContinueSeries = event.enabled) }
      is LibraryAdminCreateEvent.UpdateScriptedEpubs ->
        updateBookSettings { it.copy(epubsAllowScriptedContent = event.enabled) }
      is LibraryAdminCreateEvent.UpdatePodcastSearchRegion ->
        updatePodcastSettings { it.copy(podcastSearchRegion = event.value) }
      is LibraryAdminCreateEvent.SelectFinishThresholdMode -> selectFinishThresholdMode(event.mode)
      is LibraryAdminCreateEvent.UpdateFinishThresholdValue ->
        updateFinishThresholdValue(event.value)
      is LibraryAdminCreateEvent.ToggleMetadataSource ->
        updateDraft { withMetadataSource(event.id, event.enabled) }
      is LibraryAdminCreateEvent.MoveMetadataSource ->
        updateDraft { moveMetadataSource(event.id, event.delta) }
      is LibraryAdminCreateEvent.ToggleSchedule ->
        updateDraft { copy(schedule = schedule.copy(enabled = event.enabled)) }
      is LibraryAdminCreateEvent.SelectScheduleMode -> updateSchedule { copy(mode = event.mode) }
      is LibraryAdminCreateEvent.SelectScheduleInterval ->
        updateSchedule { copy(simple = simple.copy(interval = event.interval)) }
      is LibraryAdminCreateEvent.UpdateScheduleHour ->
        updateSchedule { copy(simple = simple.copy(hour = event.value)) }
      is LibraryAdminCreateEvent.UpdateScheduleMinute ->
        updateSchedule { copy(simple = simple.copy(minute = event.value)) }
      is LibraryAdminCreateEvent.ToggleScheduleWeekday ->
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
      is LibraryAdminCreateEvent.UpdateAdvancedScheduleCron ->
        updateSchedule { copy(advancedCronExpression = event.value) }
      LibraryAdminCreateEvent.ValidateSchedule -> validateSchedule()
      is LibraryAdminCreateEvent.SelectTab -> _uiState.update { it.copy(selectedTab = event.tab) }
      is LibraryAdminCreateEvent.UpdateManualFolder ->
        updateForm { copy(manualFolderDraft = event.value) }
      LibraryAdminCreateEvent.AddManualFolder -> addManualFolder()
      is LibraryAdminCreateEvent.SelectFolder -> addFolder(event.path)
      is LibraryAdminCreateEvent.RemoveFolder ->
        updateDraft { copy(folders = folders.filterNot { it == event.path }) }
      LibraryAdminCreateEvent.OpenFilesystem -> browseFilesystem(null)
      is LibraryAdminCreateEvent.OpenFilesystemPath -> browseFilesystem(event.path)
      LibraryAdminCreateEvent.CloseFilesystem ->
        _uiState.update { it.copy(filesystemState = LibraryAdminFilesystemState.Closed) }
      LibraryAdminCreateEvent.Submit -> submit()
      LibraryAdminCreateEvent.RetryLocalSynchronization -> retryLocalSynchronization()
      LibraryAdminCreateEvent.Back -> handleBack()
      LibraryAdminCreateEvent.ConfirmDiscard ->
        _uiState.update {
          it.copy(navigation = LibraryAdminCreateNavigation.Back, discardDialog = false)
        }
      LibraryAdminCreateEvent.CancelDiscard -> _uiState.update { it.copy(discardDialog = false) }
      LibraryAdminCreateEvent.ConsumeNavigation -> _uiState.update { it.copy(navigation = null) }
      LibraryAdminCreateEvent.ConsumeFocus -> _uiState.update { it.copy(focusField = null) }
    }
  }

  private fun selectMediaType(mediaType: MediaType) {
    if (mediaType == _uiState.value.draft.mediaType) return
    val selectedTab =
      if (
        mediaType == MediaType.PODCAST &&
          _uiState.value.selectedTab == LibraryAdminCreateTab.SCANNER
      ) {
        LibraryAdminCreateTab.DETAILS
      } else {
        _uiState.value.selectedTab
      }
    updateDraft(
      updateState = {
        copy(providerState = LibraryAdminProviderState.Loading, selectedTab = selectedTab)
      },
      update = { withMediaType(mediaType) },
    )
    loadProviders(mediaType)
  }

  private fun loadProviders(mediaType: MediaType) {
    if (_uiState.value.isSubmitting) return
    _uiState.update { it.copy(providerState = LibraryAdminProviderState.Loading) }
    viewModelScope.launch {
      repository
        .loadLibraryProviders(mediaType)
        .fold(
          onSuccess = { providers ->
            _uiState.update { state ->
              if (state.draft.mediaType != mediaType) state
              else {
                val selected = state.draft.provider
                val provider =
                  selected?.takeIf { value -> providers.any { it.id == value } }
                    ?: providers.firstOrNull()?.id
                val draft = state.draft.withProvider(provider)
                state.copy(
                  draft = draft,
                  providerState = LibraryAdminProviderState.Success(providers),
                  validation =
                    state.validation.copy(
                      errors = state.validation.errors - LibraryAdminCreateField.PROVIDER
                    ),
                  scheduleValidation =
                    if (draft == state.draft) state.scheduleValidation
                    else LibraryAdminScheduleValidationState.Idle,
                )
              }
            }
          },
          onFailure = { _ ->
            _uiState.update { state ->
              if (state.draft.mediaType == mediaType) {
                state.copy(providerState = LibraryAdminProviderState.Failure(null))
              } else state
            }
          },
        )
    }
  }

  private fun browseFilesystem(path: String?) {
    if (_uiState.value.isSubmitting) return
    _uiState.update { it.copy(filesystemState = LibraryAdminFilesystemState.Loading(path)) }
    viewModelScope.launch {
      repository
        .browseLibraryFilesystem(path)
        .fold(
          onSuccess = { filesystem ->
            _uiState.update {
              it.copy(filesystemState = LibraryAdminFilesystemState.Success(path, filesystem))
            }
          },
          onFailure = { _ ->
            _uiState.update {
              it.copy(filesystemState = LibraryAdminFilesystemState.Failure(path, null))
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

  /**
   * Single boundary for edits made by the create form. Every edit marks the form dirty, clears
   * errors that describe an older draft, and invalidates any server schedule validation tied to
   * that older draft.
   */
  private fun updateForm(update: LibraryAdminCreateUiState.() -> LibraryAdminCreateUiState) {
    _uiState.update {
      update(
        it.copy(
          isDirty = true,
          validation = it.validation.copy(errors = emptyMap()),
          scheduleValidation = LibraryAdminScheduleValidationState.Idle,
        )
      )
    }
  }

  private fun updateDraft(
    updateState: LibraryAdminCreateUiState.() -> LibraryAdminCreateUiState = {
      this
    },
    update: LibraryAdminDraft.() -> LibraryAdminDraft,
  ) {
    updateForm { updateState(copy(draft = update(draft))) }
  }

  private fun updateSchedule(update: LibraryAdminScheduleDraft.() -> LibraryAdminScheduleDraft) {
    updateDraft { copy(schedule = update(schedule)) }
  }

  private fun updateCommonSettings(
    updateBook: (LibraryAdminBookSettings) -> LibraryAdminBookSettings,
    updatePodcast: (LibraryAdminPodcastSettings) -> LibraryAdminPodcastSettings,
  ) {
    updateDraft {
      if (mediaType == MediaType.BOOK) {
        copy(bookSettings = updateBook(bookSettings))
      } else {
        copy(podcastSettings = updatePodcast(podcastSettings))
      }
    }
  }

  private fun updateBookSettings(update: (LibraryAdminBookSettings) -> LibraryAdminBookSettings) {
    updateDraft { copy(bookSettings = update(bookSettings)) }
  }

  private fun updatePodcastSettings(
    update: (LibraryAdminPodcastSettings) -> LibraryAdminPodcastSettings
  ) {
    updateDraft { copy(podcastSettings = update(podcastSettings)) }
  }

  private fun selectFinishThresholdMode(mode: LibraryAdminFinishThresholdMode) {
    updateDraft {
      withFinishThreshold {
        it.selectMode(mode == LibraryAdminFinishThresholdMode.PERCENT_COMPLETE)
      }
    }
  }

  private fun updateFinishThresholdValue(value: Int) {
    updateDraft { withFinishThreshold { it.updateValue(value) } }
  }

  private fun submit() {
    val state = _uiState.value
    if (state.isBusy) return
    val validation =
      validateLibraryAdminDraft(
        draft = state.draft,
        providers = (state.providerState as? LibraryAdminProviderState.Success)?.providers,
      )
    if (!validation.isValid) {
      _uiState.update {
        it.copy(
          selectedTab =
            when (validation.firstInvalidField) {
              LibraryAdminCreateField.SETTINGS_FINISH_THRESHOLD -> LibraryAdminCreateTab.SETTINGS
              LibraryAdminCreateField.SCANNER_PRECEDENCE -> LibraryAdminCreateTab.SCANNER
              LibraryAdminCreateField.SCHEDULE -> LibraryAdminCreateTab.SCHEDULE
              else -> LibraryAdminCreateTab.DETAILS
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
          selectedTab = LibraryAdminCreateTab.SCHEDULE,
          validation =
            it.validation.copy(
              errors =
                it.validation.errors +
                  (LibraryAdminCreateField.SCHEDULE to
                    listOf(LibraryAdminCreateError.SCHEDULE_INVALID))
            ),
          focusField = LibraryAdminCreateField.SCHEDULE,
        )
      }
      return
    }
    if (schedule.enabled && schedule.mode == LibraryAdminScheduleMode.Advanced) {
      if (state.scheduleValidation is LibraryAdminScheduleValidationState.Valid) {
        createLibrary(state.draft)
      } else {
        validateAdvancedSchedule(state, createOnSuccess = true)
      }
      return
    }
    _uiState.update {
      it.copy(scheduleValidation = LibraryAdminScheduleValidationState.Valid)
    }
    createLibrary(state.draft)
  }

  private fun validateSchedule() {
    val state = _uiState.value
    if (state.isBusy) return
    val schedule = state.draft.schedule
    if (!schedule.enabled || schedule.mode != LibraryAdminScheduleMode.Advanced) return
    val localValidation = schedule.localValidationMessage()
    if (localValidation != null) {
      val error =
        if (localValidation == "Enter a five-field cron expression.") {
          LibraryAdminCreateError.SCHEDULE_REQUIRED
        } else {
          LibraryAdminCreateError.SCHEDULE_INVALID
        }
      _uiState.update {
        it.copy(
          selectedTab = LibraryAdminCreateTab.SCHEDULE,
          scheduleValidation = LibraryAdminScheduleValidationState.Invalid(localValidation),
          validation =
            it.validation.copy(
              errors = it.validation.errors + (LibraryAdminCreateField.SCHEDULE to listOf(error))
            ),
          focusField = LibraryAdminCreateField.SCHEDULE,
        )
      }
      return
    }
    validateAdvancedSchedule(state, createOnSuccess = false)
  }

  private fun validateAdvancedSchedule(
    state: LibraryAdminCreateUiState,
    createOnSuccess: Boolean,
  ) {
    val expression = state.draft.schedule.cronExpression ?: return
    _uiState.update {
      it.copy(
        scheduleValidation = LibraryAdminScheduleValidationState.Validating,
        validation =
          it.validation.copy(errors = it.validation.errors - LibraryAdminCreateField.SCHEDULE),
      )
    }
    viewModelScope.launch {
      repository
        .validateLibrarySchedule(expression)
        .fold(
          onSuccess = {
            if (_uiState.value.draft != state.draft) return@fold
            _uiState.update {
              it.copy(scheduleValidation = LibraryAdminScheduleValidationState.Valid)
            }
            if (createOnSuccess) createLibrary(state.draft)
          },
          onFailure = { error ->
            if (_uiState.value.draft != state.draft) return@fold
            val unavailable =
              error is LibraryAdminScheduleValidationException.Unavailable ||
                error !is LibraryAdminScheduleValidationException.Invalid
            val validationState =
              if (unavailable) {
                LibraryAdminScheduleValidationState.Unavailable(error.message)
              } else {
                LibraryAdminScheduleValidationState.Invalid(error.message)
              }
            _uiState.update {
              it.copy(
                selectedTab = LibraryAdminCreateTab.SCHEDULE,
                scheduleValidation = validationState,
                validation =
                  it.validation.copy(
                    errors =
                      it.validation.errors +
                        (LibraryAdminCreateField.SCHEDULE to
                          listOf(
                            if (unavailable) {
                              LibraryAdminCreateError.SCHEDULE_VALIDATION_UNAVAILABLE
                            } else {
                              LibraryAdminCreateError.SCHEDULE_INVALID
                            }
                          ))
                  ),
                focusField = LibraryAdminCreateField.SCHEDULE,
              )
            }
          },
        )
    }
  }

  private fun createLibrary(draft: LibraryAdminDraft) {
    if (_uiState.value.isSubmitting) return
    _uiState.update {
      it.copy(submissionState = LibraryAdminCreateSubmissionState.Submitting)
    }
    viewModelScope.launch {
      repository
        .createLibrary(draft)
        .fold(
          onSuccess = { result ->
            _uiState.update {
              when (result) {
                is LibraryAdminCreateResult.Created ->
                  it.copy(
                    submissionState = LibraryAdminCreateSubmissionState.Idle,
                    navigation = LibraryAdminCreateNavigation.Created(result.library),
                  )
                is LibraryAdminCreateResult.CreatedButNotSynchronized ->
                  it.copy(
                    submissionState =
                      LibraryAdminCreateSubmissionState.LocalSyncFailure(
                        result.library,
                        null,
                      )
                  )
              }
            }
          },
          onFailure = { _ ->
            _uiState.update {
              it.copy(submissionState = LibraryAdminCreateSubmissionState.ServerFailure(null))
            }
          },
        )
    }
  }

  private fun retryLocalSynchronization() {
    if (_uiState.value.isSubmitting) return
    val localFailure =
      _uiState.value.submissionState as? LibraryAdminCreateSubmissionState.LocalSyncFailure
        ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(submissionState = LibraryAdminCreateSubmissionState.Submitting) }
      repository
        .synchronizeLibraries()
        .fold(
          onSuccess = {
            _uiState.update {
              it.copy(
                submissionState = LibraryAdminCreateSubmissionState.Idle,
                navigation = LibraryAdminCreateNavigation.Created(localFailure.library),
              )
            }
          },
          onFailure = { _ ->
            _uiState.update {
              it.copy(
                submissionState =
                  LibraryAdminCreateSubmissionState.LocalSyncFailure(
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
      _uiState.update { it.copy(navigation = LibraryAdminCreateNavigation.Back) }
    }
  }
}

sealed interface LibraryAdminCreateEvent {
  data object Load : LibraryAdminCreateEvent

  data object RetryProviders : LibraryAdminCreateEvent

  data class SelectMediaType(val mediaType: MediaType) : LibraryAdminCreateEvent

  data class UpdateName(val value: String) : LibraryAdminCreateEvent

  data class SelectIcon(val icon: String) : LibraryAdminCreateEvent

  data class SelectProvider(val providerId: String) : LibraryAdminCreateEvent

  data class UpdateCoverAspectRatio(val value: Int) : LibraryAdminCreateEvent

  data class UpdateWatcher(val enabled: Boolean) : LibraryAdminCreateEvent

  data class UpdateAudiobooksOnly(val enabled: Boolean) : LibraryAdminCreateEvent

  data class UpdateSkipMatchingAsin(val enabled: Boolean) : LibraryAdminCreateEvent

  data class UpdateSkipMatchingIsbn(val enabled: Boolean) : LibraryAdminCreateEvent

  data class UpdateHideSingleBookSeries(val enabled: Boolean) : LibraryAdminCreateEvent

  data class UpdateOnlyShowLaterBooks(val enabled: Boolean) : LibraryAdminCreateEvent

  data class UpdateScriptedEpubs(val enabled: Boolean) : LibraryAdminCreateEvent

  data class UpdatePodcastSearchRegion(val value: String) : LibraryAdminCreateEvent

  data class SelectFinishThresholdMode(val mode: LibraryAdminFinishThresholdMode) :
    LibraryAdminCreateEvent

  data class UpdateFinishThresholdValue(val value: Int) : LibraryAdminCreateEvent

  data class ToggleMetadataSource(val id: String, val enabled: Boolean) : LibraryAdminCreateEvent

  data class MoveMetadataSource(val id: String, val delta: Int) : LibraryAdminCreateEvent

  data class ToggleSchedule(val enabled: Boolean) : LibraryAdminCreateEvent

  data class SelectScheduleMode(val mode: LibraryAdminScheduleMode) : LibraryAdminCreateEvent

  data class SelectScheduleInterval(val interval: LibraryAdminScheduleInterval) :
    LibraryAdminCreateEvent

  data class UpdateScheduleHour(val value: String) : LibraryAdminCreateEvent

  data class UpdateScheduleMinute(val value: String) : LibraryAdminCreateEvent

  data class ToggleScheduleWeekday(val weekday: Int, val selected: Boolean) :
    LibraryAdminCreateEvent

  data class UpdateAdvancedScheduleCron(val value: String) : LibraryAdminCreateEvent

  data object ValidateSchedule : LibraryAdminCreateEvent

  data class SelectTab(val tab: LibraryAdminCreateTab) : LibraryAdminCreateEvent

  data class UpdateManualFolder(val value: String) : LibraryAdminCreateEvent

  data object AddManualFolder : LibraryAdminCreateEvent

  data class SelectFolder(val path: String) : LibraryAdminCreateEvent

  data class RemoveFolder(val path: String) : LibraryAdminCreateEvent

  data object OpenFilesystem : LibraryAdminCreateEvent

  data class OpenFilesystemPath(val path: String) : LibraryAdminCreateEvent

  data object CloseFilesystem : LibraryAdminCreateEvent

  data object Submit : LibraryAdminCreateEvent

  data object RetryLocalSynchronization : LibraryAdminCreateEvent

  data object Back : LibraryAdminCreateEvent

  data object ConfirmDiscard : LibraryAdminCreateEvent

  data object CancelDiscard : LibraryAdminCreateEvent

  data object ConsumeNavigation : LibraryAdminCreateEvent

  data object ConsumeFocus : LibraryAdminCreateEvent
}

enum class LibraryAdminFinishThresholdMode {
  TIME_REMAINING,
  PERCENT_COMPLETE,
}
