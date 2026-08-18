package dev.halim.shelfdroid.core.data.admin

import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Shared destination-level authorization for server administration screens. */
class AdminDestinationGuard @Inject constructor(private val prefsRepository: PrefsRepository) {

  suspend fun canAccess(): Boolean = prefsRepository.userPrefs.first().isAdmin
}
