package com.example.gymlocker.data.entity.template

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_set",
    foreignKeys = [
        ForeignKey(
            entity = TemplateExercise::class,
            parentColumns = ["id"],
            childColumns = ["templateExerciseId"],
            onDelete = ForeignKey.Companion.CASCADE,
            onUpdate = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index(value = ["templateExerciseId"]),
        Index(value = ["templateExerciseId", "setNumber"], unique = true)
    ]
)
data class TemplateSet(
    @PrimaryKey(autoGenerate = true)
    val sid: Long = 0L,

    val templateExerciseId: Long,
    val setNumber: Int,

    val weight: Float,
    val reps: Int
)