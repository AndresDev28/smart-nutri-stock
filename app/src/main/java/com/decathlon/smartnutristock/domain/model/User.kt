package com.decathlon.smartnutristock.domain.model

import java.time.Instant

/**
 * Domain model representing a user in the system.
 *
 * Pure Kotlin data class - NO Room or Supabase annotations.
 * This is used throughout the domain and presentation layers.
 *
 * @property id Unique user identifier (matches Supabase auth.users.id)
 * @property email User's email address
 * @property username Optional username/display name
 * @property storeId Store identifier (default: "1620" for Decathlon Gandía)
 * @property role User role (ADMIN, STAFF)
 * @property deviceId Device identifier for multi-device tracking
 */
data class User(
    val id: String,
    val email: String,
    val username: String? = null,
    val storeId: String = "1620",
    val role: UserRole = UserRole.STAFF,
    val deviceId: String? = null
)

/**
 * User role enumeration.
 */
enum class UserRole {
    ADMIN,
    STAFF
}
