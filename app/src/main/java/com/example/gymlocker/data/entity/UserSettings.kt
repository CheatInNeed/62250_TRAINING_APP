package com.example.gymlocker.data.entity

import androidx.room.Entity

@Entity(
    tableName = "user_settings",
    primaryKeys = ["userId"]
)
data class UserSettings(
    val userId: Long,

    // Personalization examples (adjust as you like)
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM, // SYSTEM/LIGHT/DARK
    val weightUnit: WeightUnit = WeightUnit.KG,  // KG/LB
    val forceDarkMode: Boolean = false,  // ✅ NEW: if true => always dark


    val restTimerEnabled: Boolean = true,

)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class WeightUnit { KG, LB }

enum class AppTheme {
    DEFAULT, // matches system colors/dynamic
    RED,
    BLUE,
    GREEN
}

