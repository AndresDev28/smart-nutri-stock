package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.AuthState
import com.decathlon.smartnutristock.domain.repository.AuthRepository
import com.decathlon.smartnutristock.domain.validation.EmailValidator
import javax.inject.Inject

/**
 * Use case for user login with email and password.
 *
 * This use case performs fail-fast validation before calling the repository:
 * - Email must not be empty
 * - Email must match valid format
 * - Password must not be empty
 *
 * If validation fails, returns AuthState.Error immediately (no network call).
 * If validation passes, delegates to AuthRepository.login().
 *
 * @property authRepository Repository for authentication operations
 * @property emailValidator Validator for email format
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val emailValidator: EmailValidator
) {

    /**
     * Execute login with email and password.
     *
     * Validation rules (fail-fast):
     * 1. Email must not be empty or blank
     * 2. Email must match valid email format (android.util.Patterns.EMAIL_ADDRESS)
     * 3. Password must not be empty or blank
     *
     * If any validation fails, returns Result.failure() with descriptive message.
     * If all validations pass, calls AuthRepository.login().
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing AuthState on success, or error on failure
     */
    suspend fun login(email: String, password: String): Result<AuthState> {
        // Validation 1: Email must not be empty
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("El email no puede estar vacío"))
        }

        // Validation 2: Email must match valid format
        if (!emailValidator.isValid(email)) {
            return Result.failure(IllegalArgumentException("El formato del email no es válido"))
        }

        // Validation 3: Password must not be empty
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("La contraseña no puede estar vacía"))
        }

        // All validations passed, call repository
        return try {
            val userResult = authRepository.login(email, password)
            userResult.fold(
                onSuccess = { user ->
                    Result.success(AuthState.Authenticated(user))
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
