package com.example.gymlocker.data.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymlocker.data.entity.ExerciseLog
import com.example.gymlocker.data.entity.PerformedSet

data class ExerciseLogWithSets(
    @Embedded val log: ExerciseLog,

    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseLogId"
    )
    val sets: List<PerformedSet>
)
