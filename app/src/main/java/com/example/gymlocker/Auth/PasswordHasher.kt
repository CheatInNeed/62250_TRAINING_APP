package com.example.gymlocker.data.auth

import java.security.MessageDigest

object PasswordHasher {

    // NOTE: For a school project. Real apps should use bcrypt/argon2.
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
