package dev.halim.shelfdroid.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import kotlinx.serialization.serializer

class ShelfNavigator(private val backStack: NavBackStack<ShelfNavKey>) {
  val current: ShelfNavKey?
    get() = backStack.lastOrNull()

  fun navigate(key: ShelfNavKey) {
    backStack.add(key)
  }

  fun pop(): Boolean {
    if (backStack.size <= 1) return false
    return backStack.removeLastOrNull() != null
  }

  fun popToRoot() {
    if (backStack.size <= 1) return
    val root = backStack.first()
    backStack.clear()
    backStack.add(root)
  }

  fun replaceStack(vararg keys: ShelfNavKey) {
    replaceStack(keys.toList())
  }

  fun replaceStack(keys: List<ShelfNavKey>) {
    replaceStack(keys, preserveHomeRoot = false)
  }

  fun replaceStackPreservingHome(keys: List<ShelfNavKey>) {
    replaceStack(keys, preserveHomeRoot = true)
  }

  private fun replaceStack(keys: List<ShelfNavKey>, preserveHomeRoot: Boolean) {
    var commonPrefixLength = 0
    while (
      commonPrefixLength < backStack.size &&
        commonPrefixLength < keys.size &&
        (backStack[commonPrefixLength] == keys[commonPrefixLength] ||
          (preserveHomeRoot &&
            commonPrefixLength == 0 &&
            backStack[commonPrefixLength] is Home &&
            keys[commonPrefixLength] is Home))
    ) {
      commonPrefixLength++
    }

    while (backStack.size > commonPrefixLength) {
      backStack.removeAt(backStack.lastIndex)
    }
    for (index in commonPrefixLength until keys.size) {
      backStack.add(keys[index])
    }
  }
}

@Composable
fun rememberShelfNavBackStack(startKey: ShelfNavKey): NavBackStack<ShelfNavKey> {
  return rememberSerializable(serializer = serializer()) { NavBackStack(startKey) }
}

@Composable
fun rememberShelfNavigator(backStack: NavBackStack<ShelfNavKey>): ShelfNavigator {
  return remember(backStack) { ShelfNavigator(backStack) }
}

fun enforceAuthRestorePolicy(
  navigator: ShelfNavigator,
  isLoggedIn: Boolean,
  loginKey: Login = Login(),
) {
  if (isLoggedIn && navigator.current is Login) {
    navigator.replaceStack(Home(fromLogin = true))
    return
  }
  if (!isLoggedIn && (navigator.current !is Login || navigator.current != loginKey)) {
    navigator.replaceStack(loginKey)
  }
}
