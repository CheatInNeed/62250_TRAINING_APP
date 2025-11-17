package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "muscle_groups")
data class MuscleGroup(
    @PrimaryKey(autoGenerate = true)
    val muscleGroupId: Long = 0,
    val name: String
)
