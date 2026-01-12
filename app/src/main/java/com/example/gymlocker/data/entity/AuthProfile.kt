package com.example.gymlocker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "auth_profiles",
    primaryKeys = ["authId", "userId"],
    indices = [
        Index(value = ["authId"]),
        Index(value = ["userId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AuthAccount::class,
            parentColumns = ["authId"],
            childColumns = ["authId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AuthProfile(
    val authId: Long,
    val userId: Long
)
