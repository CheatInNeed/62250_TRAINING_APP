package com.example.gymlocker.data.entity

import androidx.room.Entity

@Entity(primaryKeys = ["workoutId", "exerciseId"])
data class WorkoutExerciseCrossRef(
    val workoutId: Long,
    val exerciseId: Long
)
