package com.example.gymlocker.data.dao

data class WorkoutSummary(
    val workoutId: Long,
    val date: String,
    val name: String,
    val exerciseCount: Int
)
