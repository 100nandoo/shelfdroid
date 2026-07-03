package dev.halim.shelfdroid.core.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import dev.halim.shelfdroid.core.AuthPromptReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfNavigatorTest {
  @Test
  fun replace_stack_resets_to_login_success_home() {
    val backStack = NavBackStack<ShelfNavKey>(Login())
    val navigator = ShelfNavigator(backStack)

    navigator.replaceStack(Home(fromLogin = true))

    assertEquals(listOf(Home(fromLogin = true)), backStack.toList())
  }

  @Test
  fun pop_to_root_keeps_only_first_entry() {
    val backStack = NavBackStack<ShelfNavKey>(Home(false), Settings, SettingsPlayer)
    val navigator = ShelfNavigator(backStack)

    navigator.popToRoot()

    assertEquals(listOf(Home(false)), backStack.toList())
  }

  @Test
  fun enforce_auth_restore_policy_replaces_protected_stack_when_logged_out() {
    val backStack = NavBackStack<ShelfNavKey>(Home(false), Settings)
    val navigator = ShelfNavigator(backStack)

    enforceAuthRestorePolicy(navigator, isLoggedIn = false)

    assertEquals(listOf(Login()), backStack.toList())
  }

  @Test
  fun enforce_auth_restore_policy_keeps_login_stack_when_logged_out() {
    val loginKey = Login(reLogin = true, reason = AuthPromptReason.ManualReLogin)
    val backStack = NavBackStack<ShelfNavKey>(loginKey)
    val navigator = ShelfNavigator(backStack)

    enforceAuthRestorePolicy(navigator, isLoggedIn = false, loginKey = loginKey)

    assertEquals(listOf(loginKey), backStack.toList())
  }

  @Test
  fun enforce_auth_restore_policy_uses_reasoned_login_key_when_logged_out() {
    val backStack = NavBackStack<ShelfNavKey>(Home(false))
    val navigator = ShelfNavigator(backStack)
    val loginKey = Login(reLogin = true, reason = AuthPromptReason.RefreshFailed)

    enforceAuthRestorePolicy(navigator, isLoggedIn = false, loginKey = loginKey)

    assertEquals(listOf(loginKey), backStack.toList())
  }

  @Test
  fun enforce_auth_restore_policy_replaces_existing_login_when_reason_changes() {
    val backStack = NavBackStack<ShelfNavKey>(Login())
    val navigator = ShelfNavigator(backStack)
    val loginKey = Login(reLogin = true, reason = AuthPromptReason.ManualReLogin)

    enforceAuthRestorePolicy(navigator, isLoggedIn = false, loginKey = loginKey)

    assertEquals(listOf(loginKey), backStack.toList())
  }

  @Test
  fun pop_returns_false_at_root() {
    val backStack = NavBackStack<ShelfNavKey>(Home(false))
    val navigator = ShelfNavigator(backStack)

    val popped = navigator.pop()

    assertTrue(!popped)
    assertEquals(listOf(Home(false)), backStack.toList())
  }
}
