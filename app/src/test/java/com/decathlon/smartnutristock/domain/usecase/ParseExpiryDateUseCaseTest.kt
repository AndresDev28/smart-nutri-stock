package com.decathlon.smartnutristock.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ParseExpiryDateUseCaseTest {

    private lateinit var useCase: ParseExpiryDateUseCase

    @Before
    fun setup() {
        useCase = ParseExpiryDateUseCase()
    }

    @Test
    fun `parseExpiryDate with valid DD slash MM slash YYYY should return Instant`() = runTest {
        val dateStr = "15/03/2026"
        val expectedInstant = ZonedDateTime.of(
            LocalDate.of(2026, 3, 15),
            LocalTime.MIDNIGHT,
            ZoneId.of("UTC")
        ).toInstant()

        val result = useCase(dateStr)

        assert(result == expectedInstant)
    }

    @Test
    fun `parseExpiryDate with valid YYYY-MM-DD should return Instant`() = runTest {
        val dateStr = "2026-03-15"
        val expectedInstant = ZonedDateTime.of(
            LocalDate.of(2026, 3, 15),
            LocalTime.MIDNIGHT,
            ZoneId.of("UTC")
        ).toInstant()

        val result = useCase(dateStr)

        assert(result == expectedInstant)
    }

    @Test
    fun `parseExpiryDate with invalid separator should return null`() = runTest {
        val dateStr = "15.03.2026"

        val result = useCase(dateStr)

        assert(result == null)
    }

    @Test
    fun `parseExpiryDate with invalid day should return null`() = runTest {
        val dateStr = "32/03/2026"

        val result = useCase(dateStr)

        assert(result == null)
    }

    @Test
    fun `parseExpiryDate with invalid month should return null`() = runTest {
        val dateStr = "15/13/2026"

        val result = useCase(dateStr)

        assert(result == null)
    }

    @Test
    fun `parseExpiryDate with invalid year should return null`() = runTest {
        val dateStr = "15/03/26"

        val result = useCase(dateStr)

        assert(result == null)
    }

    @Test
    fun `parseExpiryDate with future date should return null`() = runTest {
        val dateStr = "15/03/2030"

        val result = useCase(dateStr)

        assert(result == null)
    }

    @Test
    fun `parseExpiryDate with valid leap day should return Instant`() = runTest {
        val dateStr = "29/02/2024"
        val expectedInstant = ZonedDateTime.of(
            LocalDate.of(2024, 2, 29),
            LocalTime.MIDNIGHT,
            ZoneId.of("UTC")
        ).toInstant()

        val result = useCase(dateStr)

        assert(result == expectedInstant)
    }

    @Test
    fun `parseExpiryDate with invalid leap day in non-leap year should return null`() = runTest {
        val dateStr = "29/02/2026"

        val result = useCase(dateStr)

        assert(result == null)
    }

    @Test
    fun `parseExpiryDate with empty string should return null`() = runTest {
        val dateStr = ""

        val result = useCase(dateStr)

        assert(result == null)
    }

    @Test
    fun `parseExpiryDate with null input should return null`() = runTest {
        val dateStr: String? = null

        val result = useCase(dateStr)

        assert(result == null)
    }

    @Test
    fun `parseExpiryDate with blank string should return null`() = runTest {
        val dateStr = "   "

        val result = useCase(dateStr)

        assert(result == null)
    }

    @Test
    fun `parseExpiryDate with extra spaces should parse correctly`() = runTest {
        val dateStr = "  15/03/2026  "
        val expectedInstant = ZonedDateTime.of(
            LocalDate.of(2026, 3, 15),
            LocalTime.MIDNIGHT,
            ZoneId.of("UTC")
        ).toInstant()

        val result = useCase(dateStr)

        assert(result == expectedInstant)
    }

    @Test
    fun `parseExpiryDate with today date should be valid`() = runTest {
        val today = LocalDate.now()
        val dateStr = String.format("%02d/%02d/%04d", today.dayOfMonth, today.monthValue, today.year)
        val expectedInstant = ZonedDateTime.of(
            today,
            LocalTime.MIDNIGHT,
            ZoneId.of("UTC")
        ).toInstant()

        val result = useCase(dateStr)

        assert(result == expectedInstant)
    }

    @Test
    fun `parseExpiryDate with yesterday date should be valid`() = runTest {
        val yesterday = LocalDate.now().minusDays(1)
        val dateStr = String.format("%02d/%02d/%04d", yesterday.dayOfMonth, yesterday.monthValue, yesterday.year)
        val expectedInstant = ZonedDateTime.of(
            yesterday,
            LocalTime.MIDNIGHT,
            ZoneId.of("UTC")
        ).toInstant()

        val result = useCase(dateStr)

        assert(result == expectedInstant)
    }
}
