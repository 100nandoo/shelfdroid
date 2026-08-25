package dev.halim.shelfdroid.core.data.screen.libraryadministration

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdministrationScheduleTest {

  @Test
  fun disabledScheduleDoesNotExposeOrSerializeCron() {
    val schedule = LibraryAdministrationScheduleDraft()

    assertEquals(false, schedule.enabled)
    assertNull(schedule.cronExpression)
    assertNull(schedule.summary)
  }

  @Test
  fun defaultEnablementMatchesWebsiteMondayPreset() {
    val schedule = LibraryAdministrationScheduleDraft(enabled = true)

    assertEquals("0 0 * * 1", schedule.cronExpression)
    assertEquals("Run every Monday at 00:00", schedule.summary)
  }

  @Test
  fun simpleWeekdayAndTimeProducesCronExpression() {
    val schedule =
      LibraryAdministrationScheduleDraft(
        enabled = true,
        simple =
          LibraryAdministrationSimpleSchedule(
            interval = LibraryAdministrationScheduleInterval.Custom,
            hour = "23",
            minute = "15",
            weekdays = setOf(1, 3, 5),
          ),
      )

    assertEquals("15 23 * * 1,3,5", schedule.cronExpression)
    assertEquals("Run every Monday, Wednesday, Friday at 23:15", schedule.summary)
  }

  @Test
  fun intervalPresetsMatchAudiobookshelfWebsite() {
    val expected =
      mapOf(
        LibraryAdministrationScheduleInterval.Every12Hours to "0 */12 * * *",
        LibraryAdministrationScheduleInterval.Every6Hours to "0 */6 * * *",
        LibraryAdministrationScheduleInterval.Every2Hours to "0 */2 * * *",
        LibraryAdministrationScheduleInterval.EveryHour to "0 * * * *",
        LibraryAdministrationScheduleInterval.Every30Minutes to "*/30 * * * *",
        LibraryAdministrationScheduleInterval.Every15Minutes to "*/15 * * * *",
      )

    expected.forEach { (interval, cron) ->
      val schedule =
        LibraryAdministrationScheduleDraft(
          enabled = true,
          simple = LibraryAdministrationSimpleSchedule(interval = interval),
        )
      assertEquals(cron, schedule.cronExpression)
      assertTrue(schedule.summary!!.isNotBlank())
    }
  }

  @Test
  fun switchingModesKeepsBothIntentionalDraftsAndUsesOnlyActiveMode() {
    val schedule =
      LibraryAdministrationScheduleDraft(
        enabled = true,
        mode = LibraryAdministrationScheduleMode.Advanced,
        advancedCronExpression = "5 4 * * 2",
      )
    val simple = schedule.copy(mode = LibraryAdministrationScheduleMode.Simple)
    val advancedAgain = simple.copy(mode = LibraryAdministrationScheduleMode.Advanced)

    assertEquals("0 0 * * 1", simple.cronExpression)
    assertEquals("5 4 * * 2", advancedAgain.cronExpression)
  }

  @Test
  fun advancedModeRequiresFiveFieldsButLeavesServerSemanticValidationToRepository() {
    assertEquals(
      "Enter a five-field cron expression.",
      LibraryAdministrationScheduleDraft(
        enabled = true,
        mode = LibraryAdministrationScheduleMode.Advanced,
        advancedCronExpression = "0 0 * *",
      ).localValidationMessage(),
    )
    assertNull(
      LibraryAdministrationScheduleDraft(
        enabled = true,
        mode = LibraryAdministrationScheduleMode.Advanced,
        advancedCronExpression = "0 0 * * 1",
      ).localValidationMessage()
    )
  }

  @Test
  fun nextRunSupportsPresetAndWeekdaySchedules() {
    val now = ZonedDateTime.of(2026, 8, 24, 23, 59, 0, 0, ZoneId.of("UTC"))

    assertEquals("2026-08-25 00:00", nextLibraryScheduleRun("0 0 * * *", now))
    assertEquals("2026-08-31 00:00", nextLibraryScheduleRun("0 0 * * 1", now))
    assertEquals("2026-08-25 00:00", nextLibraryScheduleRun("*/30 * * * *", now))
  }
}
