package com.example.gymlocker.data.entity.template

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.template.WorkoutTemplate

@Entity(
    tableName = "template_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.Companion.CASCADE,
            onUpdate = ForeignKey.Companion.CASCADE
        ),
        ForeignKey(
            entity = Exercises::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.Companion.CASCADE,
            onUpdate = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["exerciseId"]),
        Index(value = ["templateId", "exerciseId"], unique = true)
    ]
)
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val templateId: Long,
    val exerciseId: Long
)