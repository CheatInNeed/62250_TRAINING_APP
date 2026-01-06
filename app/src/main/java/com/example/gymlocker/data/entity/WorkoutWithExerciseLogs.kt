package com.example.gymlocker.data.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymlocker.data.entity.ExerciseLog
import com.example.gymlocker.data.entity.Workout

data class WorkoutWithExerciseLogs(
    @Embedded val workout: Workout,

    @Relation(
        parentColumn = "workoutId",
        entityColumn = "workoutId"
    )
    val logs: List<ExerciseLog>
)
