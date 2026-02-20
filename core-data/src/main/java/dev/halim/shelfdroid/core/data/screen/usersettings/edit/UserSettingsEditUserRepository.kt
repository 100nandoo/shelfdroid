package dev.halim.shelfdroid.core.data.screen.usersettings.edit

import dev.halim.core.network.request.UpdateUserRequest
import dev.halim.core.network.response.Permissions as NetworkPermissions
import dev.halim.shelfdroid.core.Permissions
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.TagRepo
import dev.halim.shelfdroid.core.data.response.UserRepo
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser
import javax.inject.Inject
import kotlinx.serialization.json.Json

class UserSettingsEditUserRepository
@Inject
constructor(private val userRepo: UserRepo, private val tagRepo: TagRepo, private val json: Json) {

  fun item(editUser: NavUsersSettingsEditUser): UserSettingsEditUserUiState {
    return UserSettingsEditUserUiState(
      state = GenericState.Success,
      editUser = editUser,
      tags = tagRepo.localList(),
      permissions = permissions(editUser.permissions),
    )
  }

  suspend fun updateUser(uiState: UserSettingsEditUserUiState): UserSettingsEditUserUiState {
    val request = request(uiState)
    userRepo.update(uiState.editUser.id, request).getOrElse {
      return uiState.copy(apiState = GenericState.Failure(it.message))
    }
    return uiState.copy(apiState = GenericState.Success)
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

  private fun request(uiState: UserSettingsEditUserUiState): UpdateUserRequest {
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
