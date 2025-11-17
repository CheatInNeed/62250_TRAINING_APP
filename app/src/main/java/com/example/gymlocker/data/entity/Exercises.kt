package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises",
        foreignKeys = [ForeignKey(entity = MuscleGroup::class,
                                  parentColumns = ["muscleGroupId"],
                                  childColumns = ["muscleGroupId"],
                                  onDelete = ForeignKey.CASCADE)],
        indices = [Index(value = ["name"], unique = true)])
data class Exercises(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Long = 0,
    val name: String,
    val startWeight: Int,
    val startReps: Int,
    val isRecent: Boolean,
    val muscleGroupId: Long
)
