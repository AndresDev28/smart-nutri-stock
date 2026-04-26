package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.AuthState
import com.decathlon.smartnutristock.domain.model.User
import com.decathlon.smartnutristock.domain.model.UserRole
import com.decathlon.smartnutristock.domain.repository.AuthRepository
import com.decathlon.smartnutristock.domain.validation.EmailValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LoginUseCase.
 *
 * Tests verify email/password validation and repository interaction.
 */
class LoginUseCaseTest {

    private lateinit var useCase: LoginUseCase
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockEmailValidator: EmailValidator

    @Before
    fun setup() {
        mockAuthRepository = mockk()
        mockEmailValidator = mockk()

        // Stub email validator to accept valid emails by default
        every { mockEmailValidator.isValid(any()) } answers {
            val email = firstArg<String>()
            email.contains("@") && email.contains(".")
        }

        // Specifically stub invalid emails
        every { mockEmailValidator.isValid("invalid-email") } returns false
        every { mockEmailValidator.isValid("   ") } returns false
        every { mockEmailValidator.isValid("") } returns false
        every { mockEmailValidator.isValid("invalidemail") } returns false

        useCase = LoginUseCase(mockAuthRepository, mockEmailValidator)
    }

    // TEST 1: Empty email returns error
    @Test
    fun `should return error when email is empty`() = runTest {
        // Given
        val email = ""
        val password = "validPassword123"

        // When
        val result = useCase.login(email, password)

        // Then
        assert(result.isFailure)
        val exception = result.exceptionOrNull()
        assert(exception is IllegalArgumentException)
        coVerify(exactly = 0) { mockAuthRepository.login(any(), any()) }
    }

    // TEST 2: Blank email returns error
    @Test
    fun `should return error when email is blank`() = runTest {
        // Given
        val email = "   "
        val password = "validPassword123"

        // When
        val result = useCase.login(email, password)

        // Then
        assert(result.isFailure)
        coVerify(exactly = 0) { mockAuthRepository.login(any(), any()) }
    }

    // TEST 3: Invalid email format returns error
    @Test
    fun `should return error when email has invalid format`() = runTest {
        // Given
        val email = "invalid-email"
        val password = "validPassword123"

        // When
        val result = useCase.login(email, password)

        // Then
        assert(result.isFailure)
        coVerify(exactly = 0) { mockAuthRepository.login(any(), any()) }
    }

    // TEST 4: Empty password returns error
    @Test
    fun `should return error when password is empty`() = runTest {
        // Given
        val email = "test@decathlon.com"
        val password = ""

        // When
        val result = useCase.login(email, password)

        // Then
        assert(result.isFailure)
        coVerify(exactly = 0) { mockAuthRepository.login(any(), any()) }
    }

    // TEST 5: Blank password returns error
    @Test
    fun `should return error when password is blank`() = runTest {
        // Given
        val email = "test@decathlon.com"
        val password = "   "

        // When
        val result = useCase.login(email, password)

        // Then
        assert(result.isFailure)
        coVerify(exactly = 0) { mockAuthRepository.login(any(), any()) }
    }

    // TEST 6: Valid credentials call authRepository
    @Test
    fun `should call authRepository login when credentials are valid`() = runTest {
        // Given
        val email = "test@decathlon.com"
        val password = "validPassword123"
        val user = User(
            id = "user-123",
            email = email,
            username = "Test User",
            storeId = "1620",
            role = UserRole.STAFF
        )

        coEvery { mockAuthRepository.login(email, password) } returns Result.success(user)

        // When
        useCase.login(email, password)

        // Then
        coVerify { mockAuthRepository.login(email, password) }
    }

    // TEST 7: Valid credentials return authenticated state
    @Test
    fun `should return authenticated state on successful login`() = runTest {
        // Given
        val email = "test@decathlon.com"
        val password = "validPassword123"
        val user = User(
            id = "user-123",
            email = email,
            username = "Test User",
            storeId = "1620",
            role = UserRole.STAFF
        )

        coEvery { mockAuthRepository.login(email, password) } returns Result.success(user)

        // When
        val result = useCase.login(email, password)

        // Then
        assert(result.isSuccess)
        val authState = result.getOrNull()
        assert(authState is AuthState.Authenticated)
        assert((authState as AuthState.Authenticated).user.id == user.id)
        assert((authState as AuthState.Authenticated).user.email == user.email)
    }

    // TEST 8: Invalid credentials return error state
    @Test
    fun `should return error state on login failure`() = runTest {
        // Given
        val email = "test@decathlon.com"
        val password = "wrongPassword"
        val exception = Exception("Invalid credentials")

        coEvery { mockAuthRepository.login(email, password) } returns Result.failure(exception)

        // When
        val result = useCase.login(email, password)

        // Then
        assert(result.isFailure)
        assert(result.exceptionOrNull()?.message == "Invalid credentials")
    }

    // TEST 9: Repository exception propagates to result
    @Test
    fun `should propagate repository exception to result`() = runTest {
        // Given
        val email = "test@decathlon.com"
        val password = "validPassword123"
        val exception = RuntimeException("Network error")

        coEvery { mockAuthRepository.login(email, password) } throws exception

        // When
        val result = useCase.login(email, password)

        // Then
        assert(result.isFailure)
        assert(result.exceptionOrNull() == exception)
    }

    // TEST 10: Email with valid format passes validation
    @Test
    fun `should accept valid email formats`() = runTest {
        // Given
        val validEmails = listOf(
            "test@decathlon.com",
            "user.name@decathlon.co.uk",
            "user+tag@example.com",
            "user_name123@example.com"
        )
        val password = "validPassword123"
        val user = User(
            id = "user-123",
            email = validEmails[0],
            storeId = "1620",
            role = UserRole.STAFF
        )

        coEvery { mockAuthRepository.login(any(), password) } returns Result.success(user)

        // When & Then
        validEmails.forEach { email ->
            val result = useCase.login(email, password)
            assert(result.isSuccess) { "Email $email should be valid" }
            coVerify { mockAuthRepository.login(email, password) }
        }
    }
}
