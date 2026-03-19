package com.decathlon.smartnutristock.domain.usecase

import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Use case for parsing expiry dates from user input.
 *
 * Supported Formats:
 * - DD/MM/YYYY (Spanish format: 15/03/2026)
 * - YYYY-MM-DD (ISO format: 2026-03-15)
 *
 * Validation:
 * - Reject invalid dates (e.g., 32/13/2026)
 * - Reject future dates
 * - Handle leap years correctly (e.g., 29/02/2024 is valid, 29/02/2026 is invalid)
 *
 * @return Parsed Instant (UTC) or null if invalid
 */
class ParseExpiryDateUseCase @Inject constructor() {

    private val formatterDdMmYyyy = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val formatterYyyyMmDd = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Validates that a parsed date matches the original input string.
     * This catches invalid dates like "29/02/2026" (Feb 29 in a non-leap year)
     * that would otherwise be adjusted to "01/03/2026" by SMART resolver.
     */
    private fun isValidParsedDate(date: LocalDate, originalStr: String, formatter: DateTimeFormatter): Boolean {
        val reformatted = formatter.format(date)
        return reformatted == originalStr.trim()
    }

    /**
     * Parse date string to Instant (UTC).
     *
     * @param dateStr Date string in DD/MM/YYYY or YYYY-MM-DD format
     * @return Instant at midnight UTC, or null if invalid
     */
    operator fun invoke(dateStr: String?): Instant? {
        // Return null for empty or blank input
        if (dateStr.isNullOrBlank()) {
            return null
        }

        return try {
            // Try DD/MM/YYYY format first (Spanish format)
            val trimmed = dateStr.trim()
            val temporal = formatterDdMmYyyy.parse(trimmed)
            val date = LocalDate.from(temporal)

            // Validate: Date must not have been adjusted (e.g., Feb 29 -> Mar 1 in non-leap year)
            if (!isValidParsedDate(date, trimmed, formatterDdMmYyyy)) {
                return null
            }

            // Validate: Must not be in the future
            if (date.isAfter(LocalDate.now())) {
                return null
            }

            // Convert to Instant at midnight UTC
            return ZonedDateTime.of(date, java.time.LocalTime.MIDNIGHT, ZoneId.of("UTC")).toInstant()
        } catch (e: Exception) {
            // If DD/MM/YYYY fails, try YYYY-MM-DD format
            return try {
                val trimmed = dateStr.trim()
                val temporal = formatterYyyyMmDd.parse(trimmed)
                val date = LocalDate.from(temporal)

                // Validate: Date must not have been adjusted
                if (!isValidParsedDate(date, trimmed, formatterYyyyMmDd)) {
                    return null
                }

                // Validate: Must not be in the future
                if (date.isAfter(LocalDate.now())) {
                    return null
                }

                // Convert to Instant at midnight UTC
                return ZonedDateTime.of(date, java.time.LocalTime.MIDNIGHT, ZoneId.of("UTC")).toInstant()
            } catch (inner: Exception) {
                // Both formats failed - invalid date
                null
            }
        }
    }
}
