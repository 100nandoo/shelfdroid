package dev.halim.shelfdroid.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.core.data.screen.login.OpenIdCallbackCoordinator
import dev.halim.shelfdroid.core.ui.screen.MainActivity
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OpenIdCallbackActivity : ComponentActivity() {

  @Inject lateinit var openIdCallbackCoordinator: OpenIdCallbackCoordinator

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
      openIdCallbackCoordinator.handleCallback(
        callbackUrl = intent.dataString,
        redirectUri = redirectUri,
      )
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
