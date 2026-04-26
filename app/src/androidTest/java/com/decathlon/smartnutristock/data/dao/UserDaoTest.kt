package com.decathlon.smartnutristock.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.decathlon.smartnutristock.data.entity.UserEntity
import com.decathlon.smartnutristock.data.local.SmartNutriStockDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Integration tests for UserDao using an in-memory Room database.
 *
 * Tests verify CRUD operations for the users table including:
 * - Insert/Replace users
 * - Find users by ID and email
 * - Delete users
 * - Query users by store
 */
@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: SmartNutriStockDatabase
    private lateinit var userDao: UserDao

    private val testNow = Instant.parse("2024-01-01T00:00:00Z")

    @Before
    fun setup() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmartNutriStockDatabase::class.java
        ).build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Test insert creates new user and returns valid row ID.
     */
    @Test
    fun insert_creates_new_user_and_returns_valid_rowId() = runTest {
        // Given
        val user = UserEntity(
            id = "user-123",
            email = "test@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-abc",
            createdAt = testNow
        )

        // When
        val rowId = userDao.insert(user)

        // Then
        assertThat(rowId).isNotEqualTo(-1L)

        val found = userDao.findById("user-123")
        assertThat(found).isNotNull()
        assertThat(found!!.id).isEqualTo("user-123")
        assertThat(found.email).isEqualTo("test@decathlon.com")
    }

    /**
     * Test insert replaces existing user when ID conflicts.
     */
    @Test
    fun insert_replaces_existing_user_when_ID_conflicts() = runTest {
        // Given
        val originalUser = UserEntity(
            id = "user-123",
            email = "test@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-abc",
            createdAt = testNow
        )
        userDao.insert(originalUser)

        // When - insert same ID with different email
        val updatedUser = UserEntity(
            id = "user-123",
            email = "updated@decathlon.com",
            storeId = "1621", // Changed store
            role = "ADMIN", // Changed role
            deviceId = "device-xyz", // Changed device
            createdAt = testNow.plusSeconds(3600) // Changed time
        )
        userDao.insert(updatedUser)

        // Then - should only have one record with updated values
        val allUsers = userDao.findAllByStore("1621")
        assertThat(allUsers).hasSize(1)
        assertThat(allUsers[0].email).isEqualTo("updated@decathlon.com")
        assertThat(allUsers[0].storeId).isEqualTo("1621")
        assertThat(allUsers[0].role).isEqualTo("ADMIN")
        assertThat(allUsers[0].deviceId).isEqualTo("device-xyz")

        // Verify old email no longer exists in store 1620
        val store1620Users = userDao.findAllByStore("1620")
        assertThat(store1620Users).isEmpty()
    }

    /**
     * Test findById returns user when exists.
     */
    @Test
    fun findById_returns_user_when_exists() = runTest {
        // Given
        val user = UserEntity(
            id = "user-123",
            email = "test@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-abc",
            createdAt = testNow
        )
        userDao.insert(user)

        // When
        val result = userDao.findById("user-123")

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("user-123")
        assertThat(result.email).isEqualTo("test@decathlon.com")
        assertThat(result.storeId).isEqualTo("1620")
        assertThat(result.role).isEqualTo("STAFF")
        assertThat(result.deviceId).isEqualTo("device-abc")
        assertThat(result.createdAt).isEqualTo(testNow)
    }

    /**
     * Test findById returns null when not exists.
     */
    @Test
    fun findById_returns_null_when_not_exists() = runTest {
        // Given - empty database

        // When
        val result = userDao.findById("non-existent-user")

        // Then
        assertThat(result).isNull()
    }

    /**
     * Test findByEmail returns user when exists.
     */
    @Test
    fun findByEmail_returns_user_when_exists() = runTest {
        // Given
        val user = UserEntity(
            id = "user-123",
            email = "test@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-abc",
            createdAt = testNow
        )
        userDao.insert(user)

        // When
        val result = userDao.findByEmail("test@decathlon.com")

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("user-123")
        assertThat(result.email).isEqualTo("test@decathlon.com")
    }

    /**
     * Test findByEmail returns null when not exists.
     */
    @Test
    fun findByEmail_returns_null_when_not_exists() = runTest {
        // Given - empty database

        // When
        val result = userDao.findByEmail("nonexistent@decathlon.com")

        // Then
        assertThat(result).isNull()
    }

    /**
     * Test findByEmail returns correct user when multiple users exist.
     */
    @Test
    fun findByEmail_returns_correct_user_when_multiple_users_exist() = runTest {
        // Given
        val user1 = UserEntity(
            id = "user-1",
            email = "user1@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-1",
            createdAt = testNow
        )
        userDao.insert(user1)

        val user2 = UserEntity(
            id = "user-2",
            email = "user2@decathlon.com",
            storeId = "1620",
            role = "ADMIN",
            deviceId = "device-2",
            createdAt = testNow
        )
        userDao.insert(user2)

        val user3 = UserEntity(
            id = "user-3",
            email = "user3@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-3",
            createdAt = testNow
        )
        userDao.insert(user3)

        // When
        val result = userDao.findByEmail("user2@decathlon.com")

        // Then - should return the correct user
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("user-2")
        assertThat(result.email).isEqualTo("user2@decathlon.com")
        assertThat(result.role).isEqualTo("ADMIN")
    }

    /**
     * Test deleteById deletes user and returns count of affected rows.
     */
    @Test
    fun deleteById_deletes_user_and_returns_count_of_affected_rows() = runTest {
        // Given
        val user = UserEntity(
            id = "user-123",
            email = "test@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-abc",
            createdAt = testNow
        )
        userDao.insert(user)

        // When
        val count = userDao.deleteById("user-123")

        // Then
        assertThat(count).isEqualTo(1)

        val found = userDao.findById("user-123")
        assertThat(found).isNull()
    }

    /**
     * Test deleteById returns 0 when user not found.
     */
    @Test
    fun deleteById_returns_0_when_user_not_found() = runTest {
        // Given - empty database

        // When
        val count = userDao.deleteById("non-existent-user")

        // Then
        assertThat(count).isEqualTo(0)
    }

    /**
     * Test deleteById deletes only specified user when multiple users exist.
     */
    @Test
    fun deleteById_deletes_only_specified_user_when_multiple_users_exist() = runTest {
        // Given
        val user1 = UserEntity(
            id = "user-1",
            email = "user1@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-1",
            createdAt = testNow
        )
        userDao.insert(user1)

        val user2 = UserEntity(
            id = "user-2",
            email = "user2@decathlon.com",
            storeId = "1620",
            role = "ADMIN",
            deviceId = "device-2",
            createdAt = testNow
        )
        userDao.insert(user2)

        // When - delete only user1
        val count = userDao.deleteById("user-1")

        // Then
        assertThat(count).isEqualTo(1)

        val remainingUsers = userDao.findAllByStore("1620")
        assertThat(remainingUsers).hasSize(1)
        assertThat(remainingUsers[0].id).isEqualTo("user-2")

        // Verify user1 is deleted
        val deletedUser = userDao.findById("user-1")
        assertThat(deletedUser).isNull()
    }

    /**
     * Test findAllByStore returns all users for given storeId.
     */
    @Test
    fun findAllByStore_returns_all_users_for_given_storeId() = runTest {
        // Given
        val store1620User1 = UserEntity(
            id = "user-1",
            email = "user1@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-1",
            createdAt = testNow
        )
        userDao.insert(store1620User1)

        val store1620User2 = UserEntity(
            id = "user-2",
            email = "user2@decathlon.com",
            storeId = "1620",
            role = "ADMIN",
            deviceId = "device-2",
            createdAt = testNow
        )
        userDao.insert(store1620User2)

        val store1621User = UserEntity(
            id = "user-3",
            email = "user3@decathlon.com",
            storeId = "1621",
            role = "STAFF",
            deviceId = "device-3",
            createdAt = testNow
        )
        userDao.insert(store1621User)

        // When - query for store 1620
        val store1620Users = userDao.findAllByStore("1620")

        // Then
        assertThat(store1620Users).hasSize(2)
        assertThat(store1620Users.map { it.id }).containsExactly("user-1", "user-2")

        // When - query for store 1621
        val store1621Users = userDao.findAllByStore("1621")

        // Then
        assertThat(store1621Users).hasSize(1)
        assertThat(store1621Users[0].id).isEqualTo("user-3")
    }

    /**
     * Test findAllByStore returns empty list when no users in store.
     */
    @Test
    fun findAllByStore_returns_empty_list_when_no_users_in_store() = runTest {
        // Given
        val store1621User = UserEntity(
            id = "user-1",
            email = "user1@decathlon.com",
            storeId = "1621",
            role = "STAFF",
            deviceId = "device-1",
            createdAt = testNow
        )
        userDao.insert(store1621User)

        // When - query for different store
        val result = userDao.findAllByStore("1620")

        // Then
        assertThat(result).isEmpty()
    }

    /**
     * Test findAllByStore returns empty list when database is empty.
     */
    @Test
    fun findAllByStore_returns_empty_list_when_database_is_empty() = runTest {
        // Given - empty database

        // When
        val result = userDao.findAllByStore("1620")

        // Then
        assertThat(result).isEmpty()
    }

    /**
     * Test that insert preserves all user fields correctly.
     */
    @Test
    fun insert_preserves_all_user_fields_correctly() = runTest {
        // Given
        val user = UserEntity(
            id = "user-with-all-fields",
            email = "allfields@decathlon.com",
            storeId = "1620",
            role = "ADMIN",
            deviceId = "device-with-long-id-12345",
            createdAt = testNow
        )

        // When
        userDao.insert(user)

        // Then
        val found = userDao.findById("user-with-all-fields")
        assertThat(found).isNotNull()
        assertThat(found!!.id).isEqualTo("user-with-all-fields")
        assertThat(found.email).isEqualTo("allfields@decathlon.com")
        assertThat(found.storeId).isEqualTo("1620")
        assertThat(found.role).isEqualTo("ADMIN")
        assertThat(found.deviceId).isEqualTo("device-with-long-id-12345")
        assertThat(found.createdAt).isEqualTo(testNow)
    }

    /**
     * Test that multiple deletes work correctly.
     */
    @Test
    fun multiple_deletes_work_correctly() = runTest {
        // Given
        val user1 = UserEntity(
            id = "user-1",
            email = "user1@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-1",
            createdAt = testNow
        )
        userDao.insert(user1)

        val user2 = UserEntity(
            id = "user-2",
            email = "user2@decathlon.com",
            storeId = "1620",
            role = "ADMIN",
            deviceId = "device-2",
            createdAt = testNow
        )
        userDao.insert(user2)

        val user3 = UserEntity(
            id = "user-3",
            email = "user3@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-3",
            createdAt = testNow
        )
        userDao.insert(user3)

        // When - delete multiple users
        val count1 = userDao.deleteById("user-1")
        val count2 = userDao.deleteById("user-2")

        // Then
        assertThat(count1).isEqualTo(1)
        assertThat(count2).isEqualTo(1)

        val remainingUsers = userDao.findAllByStore("1620")
        assertThat(remainingUsers).hasSize(1)
        assertThat(remainingUsers[0].id).isEqualTo("user-3")
    }

    /**
     * Test that findByEmail is case-sensitive.
     */
    @Test
    fun findByEmail_is_case_sensitive() = runTest {
        // Given
        val user = UserEntity(
            id = "user-123",
            email = "Test@Decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-abc",
            createdAt = testNow
        )
        userDao.insert(user)

        // When - search with different case
        val resultLowerCase = userDao.findByEmail("test@decathlon.com")
        val resultUpperCase = userDao.findByEmail("Test@Decathlon.com")

        // Then - should return null for different case (SQLite is case-sensitive by default)
        assertThat(resultLowerCase).isNull()
        assertThat(resultUpperCase).isNotNull()
    }

    /**
     * Test that findById is case-sensitive for user IDs.
     */
    @Test
    fun findById_is_case_sensitive_for_user_IDs() = runTest {
        // Given
        val user = UserEntity(
            id = "User-123",
            email = "test@decathlon.com",
            storeId = "1620",
            role = "STAFF",
            deviceId = "device-abc",
            createdAt = testNow
        )
        userDao.insert(user)

        // When - search with different case
        val resultLowerCase = userDao.findById("user-123")
        val resultMixedCase = userDao.findById("User-123")

        // Then
        assertThat(resultLowerCase).isNull()
        assertThat(resultMixedCase).isNotNull()
    }
}
