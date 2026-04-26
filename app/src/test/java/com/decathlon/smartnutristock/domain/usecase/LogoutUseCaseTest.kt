package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LogoutUseCase.
 *
 * Tests verify logout clears session via repository.
 */
class LogoutUseCaseTest {

    private lateinit var useCase: LogoutUseCase
    private lateinit var mockAuthRepository: AuthRepository

    @Before
    fun setup() {
        mockAuthRepository = mockk()
        useCase = LogoutUseCase(mockAuthRepository)
    }

    // TEST 1: Logout calls authRepository
    @Test
    fun `should call authRepository logout`() = runTest {
        // Given
        coEvery { mockAuthRepository.logout() } returns Result.success(Unit)

        // When
        useCase.logout()

        // Then
        coVerify { mockAuthRepository.logout() }
    }

    // TEST 2: Logout returns success when repository succeeds
    @Test
    fun `should return success when logout succeeds`() = runTest {
        // Given
        coEvery { mockAuthRepository.logout() } returns Result.success(Unit)

        // When
        val result = useCase.logout()

        // Then
        assert(result.isSuccess)
    }

    // TEST 3: Logout handles failure gracefully
    @Test
    fun `should handle logout failure gracefully`() = runTest {
        // Given
        val exception = Exception("Logout failed")
        coEvery { mockAuthRepository.logout() } returns Result.failure(exception)

        // When
        val result = useCase.logout()

        // Then
        assert(result.isFailure)
        assert(result.exceptionOrNull() == exception)
    }

    // TEST 4: Logout propagates repository exception
    @Test
    fun `should propagate repository exception on logout failure`() = runTest {
        // Given
        val exception = RuntimeException("Network error")
        coEvery { mockAuthRepository.logout() } throws exception

        // When
        val thrown = runCatching { useCase.logout() }

        // Then
        assert(thrown.isFailure)
        assert(thrown.exceptionOrNull() is RuntimeException)
        assert(thrown.exceptionOrNull()?.message == "Network error")
    }
}
