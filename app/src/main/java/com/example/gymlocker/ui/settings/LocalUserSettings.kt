package com.example.gymlocker.ui.settings

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.gymlocker.data.entity.UserSettings

val LocalUserSettings = staticCompositionLocalOf<UserSettings> {
    UserSettings(userId = -1)
}
