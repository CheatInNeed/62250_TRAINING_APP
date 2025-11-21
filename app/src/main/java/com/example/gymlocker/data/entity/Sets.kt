package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "sets",
        foreignKeys = [ForeignKey(entity = Exercises::class,
                                  parentColumns = ["exerciseId"],
                                  childColumns = ["exerciseId"],
                                  onDelete = ForeignKey.CASCADE)])
data class Sets(
    @PrimaryKey(autoGenerate = true)
    val setId: Long = 0,
    val setNumber: Int,
    val previous: String,
    val kg: Int,
    val reps: Int,
    val isCompleted: Boolean,
    val exerciseId: Long
)
