package com.example.gymlocker.data.database

import androidx.room.TypeConverter
import com.example.gymlocker.data.entity.ThemeMode
import com.example.gymlocker.data.entity.WeightUnit

class Converters {

    @TypeConverter fun themeModeToString(v: ThemeMode): String = v.name
    @TypeConverter fun stringToThemeMode(v: String): ThemeMode = ThemeMode.valueOf(v)

    @TypeConverter fun weightUnitToString(v: WeightUnit): String = v.name
    @TypeConverter fun stringToWeightUnit(v: String): WeightUnit = WeightUnit.valueOf(v)
}
