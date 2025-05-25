package dev.halim.shelfdroid.core.ui.components.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BigPlayerViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {

  private val id: String = checkNotNull(savedStateHandle.get<String>("id"))
}
