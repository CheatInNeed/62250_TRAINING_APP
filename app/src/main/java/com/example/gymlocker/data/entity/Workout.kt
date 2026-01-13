package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["name", "userId"], unique = true)]
)
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val workoutId: Long = 0,

    // Existing: "yyyy-MM-dd HH:mm:ss.SSS"
    val date: String,

    val name: String,
    val userId: Long,

    /**
     * NEW: workout duration in seconds
     * (You said: “Assume workouts have an attribute called time”)
     */
    val time: Long = 0L
)
