package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.screen.login.DefaultLoginSuccessHandler
import dev.halim.shelfdroid.core.data.screen.login.LoginSuccessHandler

@Module
@InstallIn(SingletonComponent::class)
abstract class LoginModule {

  @Binds
  abstract fun bindLoginSuccessHandler(handler: DefaultLoginSuccessHandler): LoginSuccessHandler
}
