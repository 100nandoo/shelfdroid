package dev.halim.shelfdroid.test.app.testdi

import androidx.media3.session.MediaController
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.testing.TestInstallIn
import dev.halim.shelfdroid.media.di.ActivityModule

@Module
@TestInstallIn(
  components = [ActivityRetainedComponent::class],
  replaces = [ActivityModule::class],
)
object FakeMediaModule {

  @Provides
  @ActivityRetainedScoped
  fun provideMediaControllerFuture(): ListenableFuture<MediaController> {
    return Futures.immediateFailedFuture(
      IllegalStateException("Media playback is disabled in tests")
    )
  }
}
