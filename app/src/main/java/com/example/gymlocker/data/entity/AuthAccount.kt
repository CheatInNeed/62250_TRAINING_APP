package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auth_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class AuthAccount(
    @PrimaryKey(autoGenerate = true)
    val authId: Long = 0,
    val email: String,
    val passwordHash: String
)
