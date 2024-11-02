package dev.halim.shelfdroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SharedObject {
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> get() = _isDarkMode

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
    }
}