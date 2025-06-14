package dev.halim.shelfdroid.core.ui.preview

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
      Episode("Episode 1: The Beginning", "10 January 2024", 0.75f),
      Episode("Episode 2: Rising Action", "30 May 2024", 0.5f),
      Episode("Episode 3: The Climax", "10 October 2024", 0.2f),
      Episode(
        "Episode 4: Android 15, Credential Manager, Android Studio Narwhal, Compose for TV, Enhancing  Safety " +
          "and Security",
        "25 April 2025",
        0.0f,
      ),
      Episode(
        "Episode 5: Google I/O, Gemini, and Jetpack Compose 1.8 and more!",
        "8 May 2025",
        1.0f,
      ),
    )

  // Book defaults
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
}
