package dev.halim.shelfdroid.di

import dev.halim.shelfdroid.ui.screens.detail.PodcastViewModel
import dev.halim.shelfdroid.ui.screens.home.HomeViewModel
import dev.halim.shelfdroid.ui.screens.login.LoginViewModel
import dev.halim.shelfdroid.ui.screens.player.PlayerViewModel
import dev.halim.shelfdroid.ui.screens.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::PlayerViewModel)
    viewModel { (id: String) ->
        PlayerViewModel(get(), get(), id)
    }
    viewModel { (id: String) ->
        PodcastViewModel(id)
    }

}