package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "workout_exercise_cross_ref",
        primaryKeys = ["workoutId", "exerciseId"],
        foreignKeys = [
            ForeignKey(entity = Workout::class,
                       parentColumns = ["workoutId"],
                       childColumns = ["workoutId"],
                       onDelete = ForeignKey.CASCADE),
            ForeignKey(entity = Exercises::class,
                       parentColumns = ["exerciseId"],
                       childColumns = ["exerciseId"],
                       onDelete = ForeignKey.CASCADE)
        ])
data class WorkoutExerciseCrossRef(
    val workoutId: Long,
    val exerciseId: Long
)
