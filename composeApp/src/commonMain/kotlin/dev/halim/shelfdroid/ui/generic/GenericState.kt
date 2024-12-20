package dev.halim.shelfdroid.ui.generic

sealed class GenericState {
    data object Loading : GenericState()
    data object Success : GenericState()
    data class Failure(val errorMessage: String?) : GenericState()
}
