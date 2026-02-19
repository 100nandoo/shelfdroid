package dev.halim.shelfdroid.core.data.screen.usersettings.edit

import dev.halim.core.network.request.UpdateUserRequest
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.UserRepo
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser
import javax.inject.Inject

class UserSettingsEditUserRepository @Inject constructor(private val userRepo: UserRepo) {

  suspend fun updateUser(uiState: UserSettingsEditUserUiState): UserSettingsEditUserUiState {
    val request = request(uiState.editUser)
    userRepo.update(uiState.editUser.id, request).getOrElse {
      return uiState.copy(apiState = GenericState.Failure(it.message))
    }
    return uiState.copy(apiState = GenericState.Success)
  }

  fun request(editUser: NavUsersSettingsEditUser): UpdateUserRequest {
    val permissions =
      UpdateUserRequest.Permissions(
        download = editUser.download,
        update = editUser.update,
        delete = editUser.delete,
        upload = editUser.upload,
        createEreader = editUser.createEReader,
        accessExplicitContent = editUser.accessExplicit,
        accessAllLibraries = editUser.accessAllLibraries,
        accessAllTags = editUser.accessAllTags,
        selectedTagsNotAccessible = false,
      )

    val request =
      UpdateUserRequest(
        username = editUser.username,
        email = editUser.email,
        password = editUser.password,
        type = editUser.type.name.lowercase(),
        isActive = editUser.isActive,
        permissions = permissions,
        librariesAccessible = listOf(),
        itemTagsSelected = listOf(),
      )
    return request
  }
}
