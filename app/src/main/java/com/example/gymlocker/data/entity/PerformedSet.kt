package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "performed_set",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLog::class,
            parentColumns = ["id"],
            childColumns = ["exerciseLogId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["exerciseLogId"]),
        // Make set numbers unique within a given exercise log:
        Index(value = ["exerciseLogId", "setNumber"], unique = true)
    ]
)
data class PerformedSet(
    @PrimaryKey(autoGenerate = true)
    val sid: Long = 0L,

    val exerciseLogId: Long,

    val setNumber: Int,

    val weight: Float,
    val reps: Int,

    // Useful for UI checkboxes
    val isCompleted: Boolean = false
)
