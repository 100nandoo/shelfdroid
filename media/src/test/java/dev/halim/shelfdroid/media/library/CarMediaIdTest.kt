package dev.halim.shelfdroid.media.library

import dev.halim.shelfdroid.media.mediaitem.MediaIdWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarMediaIdTest {
  @Test
  fun typedIdsUseStableWireValues() {
    val ids =
      mapOf(
        CarMediaId.Root to "car:root",
        CarMediaId.ContinueListening to "car:continue",
        CarMediaId.Books to "car:books",
        CarMediaId.Podcasts to "car:podcasts",
        CarMediaId.Downloads to "car:downloads",
        CarMediaId.Library("lib_1") to "car:library:lib_1",
        CarMediaId.Book("book-1") to "car:book:book-1",
        CarMediaId.Podcast("podcast-1") to "car:podcast:podcast-1",
        CarMediaId.Episode("podcast-1", "episode-1") to "car:episode:podcast-1:episode-1",
      )

    for ((id, wireValue) in ids) {
      assertEquals(wireValue, id.value)
      assertEquals(id, CarMediaId.parse(wireValue))
    }
  }

  @Test
  fun existingPlayableIdsKeepTheirSecondaryId() {
    for (id in
      listOf(
        MediaIdWrapper("book"),
        MediaIdWrapper("book", "0"),
        MediaIdWrapper("podcast", "episode"),
      )) {
      val wireValue = id.toMediaId()
      assertEquals(CarMediaId.LegacyPlayable(id), CarMediaId.parse(wireValue))
      assertEquals(wireValue, CarMediaId.parse(wireValue)?.value)
    }
  }

  @Test
  fun malformedIdsAreRejected() {
    val malformedIds =
      listOf(
        "",
        " ",
        "|x",
        "x|",
        "x|y|z",
        "car:",
        "car:unknown",
        "car:root:extra",
        "car:book:",
        "car:book:x|y",
        "car:episode:x",
        "car:episode::y",
        "car:episode:x:y:z",
        "car:library: ",
        "car:podcast:x\n",
      )
    for (id in malformedIds) {
      assertNull(id, CarMediaId.parse(id))
    }
  }
}
