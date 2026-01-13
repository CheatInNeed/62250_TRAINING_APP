package com.example.gymlocker.data.entity.template

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymlocker.data.entity.template.TemplateSet

data class TemplateExerciseWithSets(
    @Embedded val templateExercise: TemplateExercise,

    @Relation(
        parentColumn = "id",
        entityColumn = "templateExerciseId"
    )
    val sets: List<TemplateSet>
)