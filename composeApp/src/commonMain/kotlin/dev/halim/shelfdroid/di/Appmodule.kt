package dev.halim.shelfdroid.di

import dev.halim.shelfdroid.login.LoginViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


val appModule = module {
    viewModelOf(::LoginViewModel)
}