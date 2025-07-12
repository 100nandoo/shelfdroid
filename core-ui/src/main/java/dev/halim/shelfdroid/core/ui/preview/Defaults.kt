package dev.halim.shelfdroid.core.ui.preview

import dev.halim.shelfdroid.core.data.screen.player.PlayerBookmark
import dev.halim.shelfdroid.core.data.screen.player.PlayerChapter
import dev.halim.shelfdroid.core.data.screen.podcast.Episode

object Defaults {
  const val IMAGE_URL = ""
  const val TITLE = "Chapter 26"
  const val AUTHOR_NAME = "Adam"
  const val DESCRIPTION =
    """
        Join Adam in Chapter 26 as we delve deep into the world of quantum computing. 
        Explore the mind-bending principles of superposition and entanglement, and understand how these phenomena could revolutionize computation as we know it. 
        We discuss the current state of quantum hardware development, the challenges faced by researchers, and the potential impact on fields like medicine, materials science, and artificial intelligence. 
        Featuring interviews with leading experts and demystifying complex concepts, this episode is a must-listen for anyone curious about the future of technology.
    """

  val EPISODES =
    listOf(
      Episode("1", "Episode 1: The Beginning", "10 January 2024", 0.75f, false),
      Episode("2", "Episode 2: Rising Action", "30 May 2024", 0.5f, false),
      Episode("3", "Episode 3: The Climax", "10 October 2024", 0.2f, false),
      Episode(
        "4",
        "Episode 4: Android 15, Credential Manager, Android Studio Narwhal, Compose for TV, Enhancing  Safety " +
          "and Security",
        "25 April 2025",
        0.0f,
        false,
      ),
      Episode(
        "5",
        "Episode 5: Google I/O, Gemini, and Jetpack Compose 1.8 and more!",
        "8 May 2025",
        1.0f,
        true,
      ),
    )

  // Book
  const val BOOK_ID = "1234567890"
  const val BOOK_AUTHOR = "J. R. R. Tolkien"
  const val BOOK_TITLE = "The Fellowship of the Ring"
  const val BOOK_COVER = ""
  const val PROGRESS: Float = 0.5f
  const val PROGRESS_PERCENT: String = "12"
  const val BOOK_DESCRIPTION =
    """
        In a quiet village in the Shire, a young hobbit named Frodo Baggins inherits a Ring of great power.
        He learns that it is the One Ring, an instrument of absolute evil created by the Dark Lord Sauron.
        To prevent the Ring from falling into Sauron's hands, Frodo must undertake a perilous quest to destroy it.
        Accompanied by a fellowship of heroes including Gandalf, Aragorn, Legolas, and Gimli, Frodo journeys towards the fires of Mount Doom.
        Their path is fraught with danger, testing their courage and friendship against the growing darkness threatening Middle-earth.
    """
  const val BOOK_SUBTITLE = "Lord of the Rings, Book 1"
  const val BOOK_DURATION = "10 hour 20 minutes"
  const val BOOK_REMAINING = "2 hours 45 minutes"
  const val BOOK_NARRATOR = "Andy Serkis"
  const val BOOK_PUBLISH_YEAR = "2021"
  const val BOOK_PUBLISHER = "Tolkien Publishing"
  const val BOOK_GENRES = "Fantasy, Adventure"
  const val BOOK_LANGUAGE = "English"

  // Episode
  const val EPISODE_ID = "223344"
  const val EPISODE_TITLE =
    "Episode 4: Android 15, Credential Manager, Android Studio Narwhal, Compose for TV, Enhancing  Safety and Security"
  const val EPISODE_PODCAST = "Now in Android"
  const val EPISODE_PUBLISHED_AT = "19 June 2025"
  const val EPISODE_DESCRIPTION =
    """<p>Welcome to Now in Android, your ongoing guide to what's new and notable in the world of Android development. In this episode, Dan Galpin covers part one of the biggest announcements from Google I/O 2025. From Material Design’s latest evolution, to building with on-device and cloud AI, to updates for wearables, automotive, XR, and more.</p> <p>Stay tuned for part 2, where Dan covers Android Jetpack, Jetpack Compose, and Android Studio. </p> <p>Resources:</p> <p>Google I/O '25 Developer Keynote → <a href="https://goo.gle/4keiQ3b">https://goo.gle/4keiQ3b</a> </p> <p>16 things to know for Android developers at Google I/O 2025 → <a href="https://goo.gle/43Sx5Fe">https://goo.gle/43Sx5Fe</a> </p> <p>Start building with Material 3 Expressive → <a href="https://goo.gle/4dCUvlj">https://goo.gle/4dCUvlj</a> </p> <p>What’s new in Wear OS 6 → <a href="https://goo.gle/3FvI6TF">https://goo.gle/3FvI6TF</a> </p> <p>New in-car app experiences → <a href="https://goo.gle/3Zzh0li">https://goo.gle/3Zzh0li</a> </p> <p>Engage users on Google TV with excellent TV apps → <a href="https://goo.gle/4mR5M5Q">https://goo.gle/4mR5M5Q</a> </p> <p>Build adaptive Android apps that shine across form factors → <a href="https://goo.gle/4jqMOQA">https://goo.gle/4jqMOQA</a> </p> <p>On-device GenAI APIs as part of ML Kit help you easily build with Gemini Nano → </p> <p><a href="https://goo.gle/4dAUXQV">https://goo.gle/4dAUXQV</a> </p> <p>Updates to the Android XR SDK: Introducing Developer Preview 2 → <a href="https://goo.gle/4dz28ck">https://goo.gle/4dz28ck</a> </p> <p> </p>"""

  // PlayerChapter
  val DEFAULT_PLAYER_CHAPTER =
    PlayerChapter(
      id = 1,
      title = "Chapter 1: The Unexpected Journey",
      startFormattedTime = "00:00",
      endFormattedTime = "00:10",
    )
  val DEFAULT_PLAYER_CHAPTER_LIST =
    listOf(
      DEFAULT_PLAYER_CHAPTER,
      PlayerChapter(
        id = 2,
        title = "Chapter 2: Whispers of the Past",
        startFormattedTime = "00:10",
        endFormattedTime = "00:20",
      ),
      PlayerChapter(
        id = 3,
        title = "Chapter 3: The Council of Elrond",
        startFormattedTime = "00:20",
        endFormattedTime = "00:30",
      ),
    )

  val MANY_PLAYER_CHAPTERS_LIST =
    (1..25).map {
      PlayerChapter(
        id = it,
        title = "Chapter $it: A Long Title to Test Ellipsis and Wrapping Behavior in UI Components",
        startFormattedTime = String.format("%02d:%02d", (it - 1) * 5 / 60, (it - 1) * 5 % 60),
        endFormattedTime = String.format("%02d:%02d", it * 5 / 60, it * 5 % 60),
      )
    }

  val DEFAULT_PLAYER_BOOKMARK =
    PlayerBookmark("A very long bookmark title that should be truncated", "01:23", 83)
}
