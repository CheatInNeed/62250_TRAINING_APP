package com.example.gymlocker.data.entity.template

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymlocker.data.entity.User

@Entity(
    tableName = "workout_templates",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["name", "userId"], unique = true)
    ]
)
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true)
    val templateId: Long = 0,

    // "Identical to workout" per your requirement
    val date: String,
    val name: String,
    val userId: Long
)