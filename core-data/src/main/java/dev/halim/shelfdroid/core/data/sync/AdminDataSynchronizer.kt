package dev.halim.shelfdroid.core.data.sync

import dev.halim.shelfdroid.core.data.tags.TagRepository
import dev.halim.shelfdroid.core.data.users.UserRepository
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AdminDataSynchronizer
@Inject
constructor(
  private val userRepository: UserRepository,
  private val tagRepository: TagRepository,
  @Named("io") private val ioScope: CoroutineScope,
) {

  fun refreshIfAdmin(isAdmin: Boolean) {
    if (!isAdmin) return
    ioScope.launch { userRepository.refreshUsers() }
    ioScope.launch { tagRepository.refreshTags() }
  }
}
