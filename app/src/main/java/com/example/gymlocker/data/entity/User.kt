package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Long = 0,

    // ✅ Who owns this profile (1 auth -> many users/profiles)
    val authOwnerId: Long,

    val name: String,
    val height: Int,
    val weight: Int
)
