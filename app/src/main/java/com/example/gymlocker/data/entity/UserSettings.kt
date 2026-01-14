package com.example.gymlocker.data.entity

import androidx.room.Entity

@Entity(
    tableName = "user_settings",
    primaryKeys = ["userId"]
)
data class UserSettings(
    val userId: Long,

    // Personalization examples (adjust as you like)
    val themeMode: ThemeMode = ThemeMode.SYSTEM, // SYSTEM/LIGHT/DARK
    val weightUnit: WeightUnit = WeightUnit.KG,  // KG/LB
    val defaultRestSeconds: Int = 120,
    val hapticsEnabled: Boolean = true
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class WeightUnit { KG, LB }
