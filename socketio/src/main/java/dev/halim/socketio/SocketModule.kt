package dev.halim.socketio

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.socket.client.IO
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SocketModule {
  @Provides
  @Singleton
  fun provideSocketClientFactory(): SocketClientFactory = SocketClientFactory { url, options ->
    IoSocketClient(IO.socket(url, options))
  }
}
