package dev.halim.shelfdroid.core.data.screen.serversettings

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.UpdateServerSettingsRequest
import dev.halim.core.network.response.ServerSettings
import dev.halim.shelfdroid.core.data.GenericState
import javax.inject.Inject

class ServerSettingsRepository @Inject constructor(private val api: ApiService) {

  suspend fun loadSettings(): ServerSettingsUiState {
    val settingsResult =
      api.authorize().getOrElse {
        return ServerSettingsUiState(state = GenericState.Failure(it.message))
      }

    val providers =
      api
        .searchProviders()
        .getOrElse { null }
        ?.providers
        ?.booksCovers
        ?.map { CoverProvider(it.value, it.text) } ?: emptyList()

    return mapToUiState(settingsResult.serverSettings, providers)
  }

  suspend fun saveSettings(uiState: ServerSettingsUiState): ServerSettingsUiState {
    val saved = uiState.savedState
    val current = uiState.currentState
    val request =
      buildRequest(saved, current) ?: return uiState.copy(apiState = ServerSettingsApiState.Idle)

    val response =
      api.updateSettings(request).getOrElse {
        return uiState.copy(apiState = ServerSettingsApiState.SettingsFailure(it.message))
      }
    return mapToUiState(response.serverSettings, uiState.coverProviders)
      .copy(apiState = ServerSettingsApiState.SettingsSuccess)
  }

  suspend fun purgeCache(uiState: ServerSettingsUiState): ServerSettingsUiState {
    api.purgeCache().getOrElse {
      return uiState.copy(apiState = ServerSettingsApiState.PurgeCacheFailure(it.message))
    }
    return uiState.copy(apiState = ServerSettingsApiState.PurgeCacheSuccess)
  }

  suspend fun purgeItemsCache(uiState: ServerSettingsUiState): ServerSettingsUiState {
    api.purgeItemsCache().getOrElse {
      return uiState.copy(apiState = ServerSettingsApiState.PurgeItemsCacheFailure(it.message))
    }
    return uiState.copy(apiState = ServerSettingsApiState.PurgeItemsCacheSuccess)
  }

  private fun buildRequest(
    saved: ServerSettingsData,
    current: ServerSettingsData,
  ): UpdateServerSettingsRequest? {
    if (saved == current) return null
    return UpdateServerSettingsRequest(
      storeCoverWithItem = current.storeCoverWithItem.takeIf { it != saved.storeCoverWithItem },
      storeMetadataWithItem =
        current.storeMetadataWithItem.takeIf { it != saved.storeMetadataWithItem },
      sortingIgnorePrefix = current.sortingIgnorePrefix.takeIf { it != saved.sortingIgnorePrefix },
      scannerParseSubtitle =
        current.scannerParseSubtitle.takeIf { it != saved.scannerParseSubtitle },
      scannerFindCovers = current.scannerFindCovers.takeIf { it != saved.scannerFindCovers },
      scannerCoverProvider =
        current.scannerCoverProvider.takeIf { it != saved.scannerCoverProvider },
      scannerPreferMatchedMetadata =
        current.scannerPreferMatchedMetadata.takeIf { it != saved.scannerPreferMatchedMetadata },
      scannerDisableWatcher =
        (!current.watchForChanges).takeIf { current.watchForChanges != saved.watchForChanges },
      chromecastEnabled = current.chromecastEnabled.takeIf { it != saved.chromecastEnabled },
      allowIframe = current.allowIframe.takeIf { it != saved.allowIframe },
      homeBookshelfView =
        (if (current.homeBookshelfView) 1 else 0).takeIf {
          current.homeBookshelfView != saved.homeBookshelfView
        },
      bookshelfView =
        (if (current.bookshelfView) 1 else 0).takeIf {
          current.bookshelfView != saved.bookshelfView
        },
      dateFormat = current.dateFormat.takeIf { it != saved.dateFormat },
      timeFormat = current.timeFormat.takeIf { it != saved.timeFormat },
      language = current.language.takeIf { it != saved.language },
    )
  }

  private fun mapToUiState(
    s: ServerSettings,
    providers: List<CoverProvider>,
  ): ServerSettingsUiState {
    val data =
      ServerSettingsData(
        storeCoverWithItem = s.storeCoverWithItem,
        storeMetadataWithItem = s.storeMetadataWithItem,
        sortingIgnorePrefix = s.sortingIgnorePrefix,
        scannerParseSubtitle = s.scannerParseSubtitle,
        scannerFindCovers = s.scannerFindCovers,
        scannerCoverProvider = s.scannerCoverProvider,
        scannerPreferMatchedMetadata = s.scannerPreferMatchedMetadata,
        watchForChanges = !s.scannerDisableWatcher,
        chromecastEnabled = s.chromecastEnabled,
        allowIframe = s.allowIframe,
        homeBookshelfView = s.homeBookshelfView == 1,
        bookshelfView = s.bookshelfView == 1,
        dateFormat = s.dateFormat,
        timeFormat = s.timeFormat,
        language = s.language,
      )
    return ServerSettingsUiState(
      state = GenericState.Success,
      savedState = data,
      currentState = data,
      coverProviders = providers,
    )
  }
}
