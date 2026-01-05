package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_logs",
    foreignKeys = [
        ForeignKey(
            entity = Exercises::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,
    val exerciseId: Long,
    val sessionId: Long,
    val setNumber: Int,
    val reps: Int,
    val weight: Int,
    val date: String
)
