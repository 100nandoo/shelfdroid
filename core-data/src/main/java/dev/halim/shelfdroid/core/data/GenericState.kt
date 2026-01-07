package dev.halim.shelfdroid.core.data

sealed class GenericState {
  data object Idle : GenericState()

  data object Loading : GenericState()

  data object Success : GenericState()

  data class Failure(val errorMessage: String?) : GenericState()
}
