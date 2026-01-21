package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = MuscleGroup::class,
            parentColumns = ["muscleGroupId"],
            childColumns = ["muscleGroupId"],
            onDelete = ForeignKey.CASCADE
        )
        // NOTE: you *can* add a ForeignKey to User here later:
        //
        // ,ForeignKey(
        //     entity = User::class,
        //     parentColumns = ["userId"],
        //     childColumns = ["ownerUserId"],
        //     onDelete = ForeignKey.CASCADE
        // )
        //
        // but that will also require a DB migration that updates existing rows.
    ],
    indices = [
        // Before: Index(value = ["name"], unique = true)
        // Now: unique per (ownerUserId, name)
        Index(value = ["ownerUserId", "name"], unique = true),
        Index(value = ["name"]),
        Index(value = ["ownerUserId"])
    ]
)
data class Exercises(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Long = 0,
    val name: String,
    val startWeight: Int,
    val startReps: Int,
    val isRecent: Boolean,
    val muscleGroupId: Long,

    /**
     * null  = global / seeded exercise (visible to all profiles)
     * non-null = custom exercise owned by that user/profile
     */
    val ownerUserId: Long? = null
)
