package com.example.gymlocker.data.entity.template

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutTemplateWithExercises(
    @Embedded val template: WorkoutTemplate,

    @Relation(
        entity = TemplateExercise::class,
        parentColumn = "templateId",
        entityColumn = "templateId"
    )
    val exercises: List<TemplateExerciseWithSets>
)