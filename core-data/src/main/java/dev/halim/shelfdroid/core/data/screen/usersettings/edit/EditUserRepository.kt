package dev.halim.shelfdroid.core.data.screen.usersettings.edit

import dev.halim.core.network.request.UpdateUserRequest
import dev.halim.core.network.response.Permissions as NetworkPermissions
import dev.halim.shelfdroid.core.Permissions
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.data.response.TagRepo
import dev.halim.shelfdroid.core.data.response.UserRepo
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.navigation.NavEditUser
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json

class EditUserRepository
@Inject
constructor(
  private val userRepo: UserRepo,
  private val tagRepo: TagRepo,
  private val libraryRepo: LibraryRepo,
) {

  fun item(editUser: NavEditUser): EditUserUiState {
    return EditUserUiState(
      state = EditUserState.Success,
      editUser = editUser,
      permissions = permissions(editUser.permissions),
      tags = tagRepo.localList(),
      libraries = libraries(libraryRepo.local()),
    )
  }

  fun libraries(entities: List<LibraryEntity>): List<EditUserUiState.Library> {
    return entities.map { EditUserUiState.Library(it.id, it.name) }
  }

  suspend fun updateUser(
    uiState: EditUserUiState,
    event: MutableSharedFlow<GenericUiEvent>,
  ): EditUserUiState {
    val request = request(uiState)
    userRepo.update(uiState.editUser.id, request).getOrElse {
      event.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
      return uiState.copy(state = EditUserState.ApiUpdateError)
    }
    event.emit(GenericUiEvent.ShowSuccessSnackbar())
    event.emit(GenericUiEvent.NavigateBack)
    return uiState.copy(state = EditUserState.ApiUpdateSuccess)
  }

  private fun permissions(permissionsString: String): Permissions {
    val permissions = Json.decodeFromString(NetworkPermissions.serializer(), permissionsString)
    return Permissions(
      download = permissions.download,
      update = permissions.update,
      delete = permissions.delete,
      upload = permissions.upload,
      createEReader = permissions.createEreader,
      accessExplicit = permissions.accessExplicitContent,
      accessAllLibraries = permissions.accessAllLibraries,
      accessAllTags = permissions.accessAllTags,
    )
  }

  private fun request(uiState: EditUserUiState): UpdateUserRequest {
    val permissions =
      UpdateUserRequest.Permissions(
        download = uiState.permissions.download,
        update = uiState.permissions.update,
        delete = uiState.permissions.delete,
        upload = uiState.permissions.upload,
        createEreader = uiState.permissions.createEReader,
        accessExplicitContent = uiState.permissions.accessExplicit,
        accessAllLibraries = uiState.permissions.accessAllLibraries,
        accessAllTags = uiState.permissions.accessAllTags,
        selectedTagsNotAccessible = uiState.editUser.invert,
      )

    val request =
      UpdateUserRequest(
        username = uiState.editUser.username,
        email = uiState.editUser.email,
        password = uiState.editUser.password,
        type = uiState.editUser.type.name.lowercase(),
        isActive = uiState.editUser.isActive,
        permissions = permissions,
        librariesAccessible = uiState.editUser.librariesAccessible,
        itemTagsSelected = uiState.editUser.itemTagsAccessible,
      )
    return request
  }
}
