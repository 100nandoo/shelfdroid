package dev.halim.shelfdroid.core.ui.screen.book

import ItemDetail
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
import dev.halim.shelfdroid.core.ui.preview.Defaults

@Composable
fun BookScreen(viewModel: BookViewModel = hiltViewModel(), onPlayClicked: (String) -> Unit) {
  val uiState by viewModel.uiState.collectAsState()
  if (uiState.state == GenericState.Success) {
    BookScreenContent(
      cover = uiState.cover,
      title = uiState.title,
      author = uiState.author,
      description = uiState.description,
      subtitle = uiState.subtitle,
      duration = uiState.duration,
      narrator = uiState.narrator,
      publishYear = uiState.publishYear,
      publisher = uiState.publisher,
      genres = uiState.genres,
      language = uiState.language,
      onPlayClicked = { onPlayClicked(viewModel.id) },
    )
  }
}

@Composable
fun BookScreenContent(
  cover: String = Defaults.BOOK_COVER,
  title: String = Defaults.BOOK_TITLE,
  author: String = Defaults.BOOK_AUTHOR,
  description: String = "",
  subtitle: String = "",
  duration: String = Defaults.BOOK_DURATION,
  narrator: String = Defaults.BOOK_NARRATOR,
  publishYear: String = Defaults.BOOK_PUBLISH_YEAR,
  publisher: String = Defaults.BOOK_PUBLISHER,
  genres: String = Defaults.BOOK_GENRES,
  language: String = Defaults.BOOK_LANGUAGE,
  onPlayClicked: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    reverseLayout = true,
    verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.Bottom),
  ) {
    item {
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = { onPlayClicked() },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = "Play",
          modifier = Modifier.padding(end = 8.dp),
        )
        Text("Play")
      }
      ExpandShrinkText(description)
      BookDetail(duration, narrator, publishYear, publisher, genres, language)
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ItemDetail(cover, title, author, subtitle = subtitle)
      }
    }
  }
}

@Composable
private fun BookDetail(
  duration: String,
  narrator: String,
  publishYear: String,
  publisher: String,
  genres: String,
  language: String,
) {
  Column(
    modifier = Modifier.padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    BookDetailRow("Duration", duration)
    BookDetailRow("Narrator", narrator)
    BookDetailRow("Publish Year", publishYear)
    BookDetailRow("Publisher", publisher)
    BookDetailRow("Genre", genres)
    BookDetailRow("Language", language)
  }
}

@Composable
private fun BookDetailRow(label: String, value: String) {
  if (value.isNotEmpty()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "$label: ",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.weight(1f, fill = true),
      )
      Text(text = value, modifier = Modifier.weight(4f, fill = true))
    }
  }
}
