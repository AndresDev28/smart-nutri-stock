package com.decathlon.smartnutristock.domain.validation

/**
 * Interface for email validation.
 *
 * Provides a clean architecture boundary between domain logic and
 * platform-specific email validation implementation.
 */
interface EmailValidator {
    /**
     * Check if the provided email address is valid.
     *
     * @param email Email address to validate
     * @return true if email format is valid, false otherwise
     */
    fun isValid(email: String): Boolean
}
