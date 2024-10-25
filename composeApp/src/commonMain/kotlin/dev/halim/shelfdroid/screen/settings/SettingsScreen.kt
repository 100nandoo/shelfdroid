package dev.halim.shelfdroid.screen.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun SettingsScreen(){
    Text("Settings screen")
    TextButton(onClick = { /* Do something! */ }) {
        Text("Logout")
    }
}