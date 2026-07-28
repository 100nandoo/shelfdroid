package dev.halim.shelfdroid.core.ui.screen.apprisenotificationsettings.notificationruleeditor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationRuleEditorScreenTest {
  @Test
  fun extracts_plain_variable_name_for_chip_label() {
    assertEquals("backupName", notificationRuleVariableName("{{backupName}}"))
    assertEquals("error", notificationRuleVariableName(" error "))
  }

  @Test
  fun formats_server_variable_for_template_insertion() {
    assertEquals("{{ backupName }}", formatNotificationRuleTemplateVariable("{{backupName}}"))
    assertEquals("{{ error }}", formatNotificationRuleTemplateVariable(" error "))
  }

  @Test
  fun inserts_variable_at_cursor_position() {
    val variable = formatNotificationRuleTemplateVariable("{{backupName}}")

    val updatedValue =
      insertNotificationRuleVariable(
        currentValue = TextFieldValue(text = "Backup failed", selection = TextRange(6)),
        variable = variable,
      )

    assertEquals("Backup$variable failed", updatedValue.text)
    assertEquals(TextRange(6 + variable.length), updatedValue.selection)
  }

  @Test
  fun replaces_selected_text_with_variable() {
    val variable = formatNotificationRuleTemplateVariable("{{error}}")

    val updatedValue =
      insertNotificationRuleVariable(
        currentValue = TextFieldValue(text = "Backup failed body", selection = TextRange(14, 18)),
        variable = variable,
      )

    assertEquals("Backup failed $variable", updatedValue.text)
    assertEquals(TextRange(14 + variable.length), updatedValue.selection)
  }
}
