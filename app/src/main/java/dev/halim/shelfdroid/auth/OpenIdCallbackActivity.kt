package dev.halim.shelfdroid.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.core.data.screen.login.OpenIdCallbackCoordinator
import dev.halim.shelfdroid.core.data.screen.login.OpenIdCallbackHandlingResult
import dev.halim.shelfdroid.core.data.screen.login.LoginRepository
import dev.halim.shelfdroid.core.ui.screen.MainActivity
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OpenIdCallbackActivity : ComponentActivity() {

  @Inject lateinit var openIdCallbackCoordinator: OpenIdCallbackCoordinator
  @Inject lateinit var loginRepository: LoginRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleCallbackIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleCallbackIntent(intent)
  }

  private fun handleCallbackIntent(intent: Intent) {
    val redirectUri = "${packageName}://oauth"
    lifecycleScope.launch {
      val result =
        openIdCallbackCoordinator.handleCallback(
          callbackUrl = intent.dataString,
          redirectUri = redirectUri,
        )
      if (result == OpenIdCallbackHandlingResult.Continue) {
        loginRepository.completeOpenIdLogin()
      }
      launchMainActivity()
      finish()
    }
  }

  private fun launchMainActivity() {
    startActivity(
      Intent(this, MainActivity::class.java).addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      )
    )
  }
}
