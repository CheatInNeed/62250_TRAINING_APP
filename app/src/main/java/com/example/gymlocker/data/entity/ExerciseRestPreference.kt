package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "exercise_rest_preference",
    primaryKeys = ["userId", "exerciseId"],
    indices = [Index(value = ["userId", "exerciseId"], unique = true)]
)
data class ExerciseRestPreference(
    val userId: Long,
    val exerciseId: Long,
    val restSeconds: Int
)
