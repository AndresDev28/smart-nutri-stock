package com.decathlon.smartnutristock.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing a user in the local database.
 *
 * Synced with Supabase app_users table. Contains user information
 * including storeId for multitenancy.
 *
 * @property id User ID (same as Supabase auth.users.id)
 * @property email User email address
 * @property storeId Store ID for multitenancy (default "1620" for Decathlon Gandía)
 * @property role User role (ADMIN or STAFF)
 * @property deviceId Device ID the user is logged in from
 * @property createdAt Timestamp when user was created
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"]),
        Index(value = ["storeId"])
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,

    val email: String,
    val storeId: String = "1620",
    val role: String = "STAFF",
    val deviceId: String,
    val createdAt: Instant
)
