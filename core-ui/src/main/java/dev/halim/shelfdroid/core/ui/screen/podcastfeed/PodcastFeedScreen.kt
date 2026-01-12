package dev.halim.shelfdroid.core.ui.screen.podcastfeed

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component3
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component4
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component5
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component6
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component7
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.podcastfeed.PodcastFeedState
import dev.halim.shelfdroid.core.data.screen.podcastfeed.PodcastFeedUiState
import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.VisibilityUp
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@Composable
fun PodcastFeedScreen(
  viewModel: PodcastFeedViewModel = hiltViewModel(),
  onCreateSuccess: (CreatePodcastNavResult) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  PodcastFeedScreenContent(uiState = uiState, viewModel::onEvent, onCreateSuccess)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PodcastFeedScreenContent(
  uiState: PodcastFeedUiState,
  onEvent: (PodcastFeedEvent) -> Unit = {},
  onCreateSuccess: (CreatePodcastNavResult) -> Unit = { _ -> },
) {
  val focusManager = LocalFocusManager.current
  val (titleRef, authorRef, feedUrlRef, descriptionRef, languageRef, pathRef, genreRef) =
    remember { FocusRequester.createRefs() }

  VisibilityDown(uiState.state is PodcastFeedState.Loading) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
  }

  VisibilityUp(visible = uiState.state is PodcastFeedState.ApiFeedSuccess) {
    var title by remember { mutableStateOf(TextFieldValue(uiState.title)) }
    var author by remember { mutableStateOf(TextFieldValue(uiState.author)) }
    var feedUrl by remember { mutableStateOf(TextFieldValue(uiState.feedUrl)) }
    var description by remember { mutableStateOf(TextFieldValue(uiState.description)) }
    var language by remember { mutableStateOf(TextFieldValue(uiState.language)) }
    var path by remember { mutableStateOf(TextFieldValue(uiState.path)) }
    var folderDropdownExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Column(
      modifier = Modifier.padding(16.dp).imePadding().fillMaxSize().verticalScroll(scrollState)
    ) {
      MyOutlinedTextField(
        value = title,
        onValueChange = {
          title = it
          onEvent(PodcastFeedEvent.TitleChanged(it.text))
        },
        label = stringResource(R.string.title),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).focusRequester(titleRef),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )
      MyOutlinedTextField(
        value = author,
        onValueChange = {
          author = it
          onEvent(PodcastFeedEvent.AuthorChanged(it.text))
        },
        label = stringResource(R.string.author),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).focusRequester(authorRef),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )
      MyOutlinedTextField(
        value = feedUrl,
        onValueChange = {
          feedUrl = it
          onEvent(PodcastFeedEvent.FeedUrlChanged(it.text))
        },
        label = stringResource(R.string.feed_url),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).focusRequester(feedUrlRef),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )
      MyOutlinedTextField(
        value = description,
        label = stringResource(R.string.description),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).focusRequester(descriptionRef),
        singleLine = false,
        maxLines = 3,
        onValueChange = {
          description = it
          onEvent(PodcastFeedEvent.DescriptionChanged(it.text))
        },
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )
      MyOutlinedTextField(
        value = language,
        onValueChange = {
          language = it
          onEvent(PodcastFeedEvent.LanguageChanged(it.text))
        },
        label = stringResource(R.string.language),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).focusRequester(languageRef),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )

      MyOutlinedTextField(
        value = path,
        onValueChange = {
          path = it
          onEvent(PodcastFeedEvent.PathChanged(it.text))
        },
        label = stringResource(R.string.path),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).focusRequester(pathRef),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
        prefix = {
          PathDropdown(uiState, folderDropdownExpanded, onEvent, { folderDropdownExpanded = it })
        },
      )

      GenreSection(uiState, scrollState, genreRef, onEvent)

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp),
      ) {
        Checkbox(
          checked = uiState.autoDownload,
          onCheckedChange = { onEvent(PodcastFeedEvent.AutoDownloadChanged(it)) },
        )
        Text(text = stringResource(R.string.auto_download_episodes))

        Button(
          onClick = { onEvent(PodcastFeedEvent.SubmitButtonPressed) },
          modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
        ) {
          Text(stringResource(R.string.submit))
        }
      }
    }
  }

  LaunchedEffect(uiState.state) {
    val state = uiState.state
    if (state is PodcastFeedState.ApiCreateSuccess) {
      onCreateSuccess(state.result)
    }
  }
}

@Composable
private fun GenreSection(
  uiState: PodcastFeedUiState,
  scrollState: ScrollState,
  genreRef: FocusRequester,
  onEvent: (PodcastFeedEvent) -> Unit,
) {
  var genreInput by remember { mutableStateOf(TextFieldValue("")) }
  val coroutineScope = rememberCoroutineScope()

  MyOutlinedTextField(
    value = genreInput,
    onValueChange = { genreInput = it },
    label = stringResource(R.string.genres),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
    modifier = Modifier.fillMaxWidth().focusRequester(genreRef),
    onNext = {
      val text = genreInput.text.trim()
      if (text.isNotBlank()) {
        onEvent(PodcastFeedEvent.GenreAdded(text))
        coroutineScope.launch { scrollState.scrollTo(scrollState.maxValue) }
        genreInput = TextFieldValue("")
      }
    },
    supportingText = {
      FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        uiState.genres.forEach { genre ->
          GenreChip(
            genre = genre,
            onRemove = { onEvent(PodcastFeedEvent.GenreRemoved(it)) },
            onEdit = {
              if (genreInput.text.isNotEmpty()) {
                onEvent(PodcastFeedEvent.GenreAdded(genreInput.text))
              }
              genreInput = TextFieldValue(it)
              genreRef.requestFocus()
              onEvent(PodcastFeedEvent.GenreRemoved(it))
            },
          )
        }
      }
    },
  )
}

@Composable
private fun GenreChip(genre: String, onRemove: (String) -> Unit, onEdit: (String) -> Unit) {
  InputChip(
    selected = false,
    onClick = { onEdit(genre) },
    label = { Text(genre) },
    modifier = Modifier.padding(end = 4.dp),
    trailingIcon = {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = stringResource(R.string.remove_genre, genre),
        modifier = Modifier.clickable { onRemove(genre) },
      )
    },
  )
}

@Composable
private fun PathDropdown(
  uiState: PodcastFeedUiState,
  expanded: Boolean,
  onEvent: (PodcastFeedEvent) -> Unit,
  onExpandedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.clickable { onExpandedChange(true) },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Default.ArrowDropDown,
      contentDescription = stringResource(R.string.select_folder),
    )
    Text(uiState.selectedFolder.path + "/", color = MaterialTheme.colorScheme.primary)
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { onExpandedChange(false) },
      modifier = Modifier.wrapContentSize(),
    ) {
      uiState.folders.forEach { folder ->
        DropdownMenuItem(
          text = { Text(folder.path) },
          onClick = {
            onEvent(PodcastFeedEvent.FolderSelected(folder))
            onExpandedChange(false)
          },
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun PodcastFeedScreenPreview() {
  PreviewWrapper(dynamicColor = false) {
    PodcastFeedScreenContent(
      uiState =
        PodcastFeedUiState(
          state = PodcastFeedState.ApiFeedSuccess,
          title = "My Awesome Podcast",
          author = "John Doe",
          feedUrl = "https://example.com/feed.xml",
          description = "This is a great podcast about stuff and things.",
          folders = listOf(Defaults.DEFAULT_PODCAST_FOLDER, Defaults.DEFAULT_PODCAST_FOLDER_2),
          selectedFolder = Defaults.DEFAULT_PODCAST_FOLDER_2,
          language = "en",
          path = "My Awesome Podcast",
          genres = listOf("Technology", "Science", "Comedy"),
        )
    )
  }
}
