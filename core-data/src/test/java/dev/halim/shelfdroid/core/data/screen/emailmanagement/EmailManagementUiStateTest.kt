package dev.halim.shelfdroid.core.data.screen.emailmanagement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailManagementUiStateTest {

  @Test
  fun canSendTest_isTrueOnlyWhenFormIsCleanAndHostIsPresent() {
    val cleanState =
      EmailManagementUiState(
        savedSettings = EmailSettingsForm(host = "smtp.example.com"),
        draftSettings = EmailSettingsForm(host = "smtp.example.com"),
      )
    val dirtyState =
      EmailManagementUiState(
        savedSettings = EmailSettingsForm(host = "smtp.example.com"),
        draftSettings =
          EmailSettingsForm(host = "smtp.example.com", testAddress = "test@example.com"),
      )
    val missingHostState =
      EmailManagementUiState(
        savedSettings = EmailSettingsForm(),
        draftSettings = EmailSettingsForm(),
      )

    assertTrue(cleanState.canSendTest)
    assertFalse(dirtyState.canSendTest)
    assertFalse(missingHostState.canSendTest)
  }

  @Test
  fun validateDeviceEditor_requiresSpecificUsersAndRejectsDuplicateNames() {
    val existingDevices = listOf(EreaderDeviceItem(name = "Kindle", email = "kindle@example.com"))
    val duplicateState =
      DeviceEditorState(
        isVisible = true,
        name = "Kindle",
        email = "other@example.com",
        availabilityOption = DeviceAvailabilityOption.AdminOrUp,
      )
    val specificUsersState =
      DeviceEditorState(
        isVisible = true,
        name = "Kobo",
        email = "kobo@example.com",
        availabilityOption = DeviceAvailabilityOption.SpecificUsers,
      )

    val duplicateValidation = validateDeviceEditor(duplicateState, existingDevices)
    val specificUsersValidation = validateDeviceEditor(specificUsersState, existingDevices)

    assertTrue(duplicateValidation.duplicateName)
    assertTrue(specificUsersValidation.specificUsersEmpty)
  }

  @Test
  fun buildUpdatedDeviceList_replacesOnlyEditedDeviceAndPreservesOthers() {
    val devices =
      listOf(
        EreaderDeviceItem(name = "Kindle", email = "kindle@example.com"),
        EreaderDeviceItem(name = "Kobo", email = "kobo@example.com"),
      )

    val updated =
      buildUpdatedDeviceList(
        existingDevices = devices,
        originalName = "Kindle",
        updatedDevice =
          EreaderDeviceItem(
            name = "Kindle Paperwhite",
            email = "paperwhite@example.com",
            availabilityOption = DeviceAvailabilityOption.UserOrUp,
          ),
      )

    assertEquals(2, updated.size)
    assertEquals("Kobo", updated.first().name)
    assertEquals("Kindle Paperwhite", updated.last().name)
    assertEquals("paperwhite@example.com", updated.last().email)
  }

  @Test
  fun buildDeletedDeviceList_removesOnlyRequestedDevice() {
    val devices =
      listOf(
        EreaderDeviceItem(name = "Kindle", email = "kindle@example.com"),
        EreaderDeviceItem(name = "Kobo", email = "kobo@example.com"),
      )

    val updated = buildDeletedDeviceList(devices, "Kindle")

    assertEquals(listOf("Kobo"), updated.map { it.name })
  }
}
