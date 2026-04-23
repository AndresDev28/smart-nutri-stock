package com.decathlon.smartnutristock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.decathlon.smartnutristock.data.entity.UserEntity

/**
 * DAO for User entity operations.
 */
@Dao
interface UserDao {
    /**
     * Insert or replace a user.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    /**
     * Find a user by ID.
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?

    /**
     * Find a user by email.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    /**
     * Delete a user by ID.
     */
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String): Int

    /**
     * Find all users in a specific store.
     */
    @Query("SELECT * FROM users WHERE storeId = :storeId")
    suspend fun findAllByStore(storeId: String): List<UserEntity>
}
