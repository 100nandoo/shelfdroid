package dev.halim.shelfdroid.core.data.screen.libraryadmin

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdminScheduleTest {

  @Test
  fun disabledScheduleDoesNotExposeOrSerializeCron() {
    val schedule = LibraryAdminScheduleDraft()

    assertEquals(false, schedule.enabled)
    assertNull(schedule.cronExpression)
    assertNull(schedule.summary)
  }

  @Test
  fun defaultEnablementMatchesWebsiteMondayPreset() {
    val schedule = LibraryAdminScheduleDraft(enabled = true)

    assertEquals("0 0 * * 1", schedule.cronExpression)
    assertEquals("Run every Monday at 00:00", schedule.summary)
  }

  @Test
  fun simpleWeekdayAndTimeProducesCronExpression() {
    val schedule =
      LibraryAdminScheduleDraft(
        enabled = true,
        simple =
          LibraryAdminSimpleSchedule(
            interval = LibraryAdminScheduleInterval.Custom,
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
        LibraryAdminScheduleInterval.Every12Hours to "0 */12 * * *",
        LibraryAdminScheduleInterval.Every6Hours to "0 */6 * * *",
        LibraryAdminScheduleInterval.Every2Hours to "0 */2 * * *",
        LibraryAdminScheduleInterval.EveryHour to "0 * * * *",
        LibraryAdminScheduleInterval.Every30Minutes to "*/30 * * * *",
        LibraryAdminScheduleInterval.Every15Minutes to "*/15 * * * *",
      )

    expected.forEach { (interval, cron) ->
      val schedule =
        LibraryAdminScheduleDraft(
          enabled = true,
          simple = LibraryAdminSimpleSchedule(interval = interval),
        )
      assertEquals(cron, schedule.cronExpression)
      assertTrue(schedule.summary!!.isNotBlank())
    }
  }

  @Test
  fun switchingModesKeepsBothIntentionalDraftsAndUsesOnlyActiveMode() {
    val schedule =
      LibraryAdminScheduleDraft(
        enabled = true,
        mode = LibraryAdminScheduleMode.Advanced,
        advancedCronExpression = "5 4 * * 2",
      )
    val simple = schedule.copy(mode = LibraryAdminScheduleMode.Simple)
    val advancedAgain = simple.copy(mode = LibraryAdminScheduleMode.Advanced)

    assertEquals("0 0 * * 1", simple.cronExpression)
    assertEquals("5 4 * * 2", advancedAgain.cronExpression)
  }

  @Test
  fun advancedModeRequiresFiveFieldsButLeavesServerSemanticValidationToRepository() {
    assertEquals(
      "Enter a five-field cron expression.",
      LibraryAdminScheduleDraft(
        enabled = true,
        mode = LibraryAdminScheduleMode.Advanced,
        advancedCronExpression = "0 0 * *",
      ).localValidationMessage(),
    )
    assertNull(
      LibraryAdminScheduleDraft(
        enabled = true,
        mode = LibraryAdminScheduleMode.Advanced,
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
