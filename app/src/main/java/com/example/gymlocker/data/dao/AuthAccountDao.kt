package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymlocker.data.entity.AuthAccount

@Dao
interface AuthAccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AuthAccount): Long

    @Query("SELECT * FROM auth_accounts WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): AuthAccount?

    @Query("DELETE FROM auth_accounts WHERE authId = :authId")
    suspend fun deleteById(authId: Long)
}
