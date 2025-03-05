package dev.halim.shelfdroid.core.data.navigation

import dev.halim.shelfdroid.core.datastore.DataStoreManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class NavigationRepository @Inject constructor(private val dataStoreManager: DataStoreManager) {
    val token = runBlocking { dataStoreManager.token.firstOrNull() }
    val baseUrl = runBlocking { dataStoreManager.baseUrl.firstOrNull() }

}