package com.example.gymlocker.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.gymlocker.data.entity.UserSettings
import com.example.gymlocker.data.repo.SettingsRepository
import com.example.gymlocker.ui.settings.LocalUserSettings

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }

    val settingsOrNull by repo.activeSettings.collectAsState(initial = null)

    val settings = settingsOrNull ?: UserSettings(userId = -1)

    CompositionLocalProvider(
        LocalUserSettings provides settings
    ) {
        AppNavigation()
    }
}
