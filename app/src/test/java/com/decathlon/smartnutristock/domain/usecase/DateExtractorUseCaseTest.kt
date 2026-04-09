package com.decathlon.smartnutristock.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DateExtractorUseCaseTest {

    private lateinit var useCase: DateExtractorUseCase

    @Before
    fun setup() {
        useCase = DateExtractorUseCase()
    }

    @Test
    fun `clean date DD slash MM slash YYYY should return LocalDate`() = runTest {
        val result = useCase("15/03/2026")
        assert(result == LocalDate.of(2026, 3, 15))
    }

    @Test
    fun `clean date DD dash MM dash YYYY should return LocalDate`() = runTest {
        val result = useCase("15-03-2026")
        assert(result == LocalDate.of(2026, 3, 15))
    }

    @Test
    fun `clean date DD dot MM dot YYYY should return LocalDate`() = runTest {
        val result = useCase("15.03.2026")
        assert(result == LocalDate.of(2026, 3, 15))
    }

    @Test
    fun `clean date DD slash MM slash YY should return LocalDate`() = runTest {
        val result = useCase("15/03/26")
        assert(result == LocalDate.of(2026, 3, 15))
    }

    @Test
    fun `MM slash YY september should return end of month`() = runTest {
        val result = useCase("09/26")
        assert(result == LocalDate.of(2026, 9, 30))
    }

    @Test
    fun `MM slash YYYY september should return end of month`() = runTest {
        val result = useCase("09/2026")
        assert(result == LocalDate.of(2026, 9, 30))
    }

    @Test
    fun `MM slash YY february non-leap should return end of month`() = runTest {
        val result = useCase("02/26")
        assert(result == LocalDate.of(2026, 2, 28))
    }

    @Test
    fun `MM slash YY february leap year should return end of month`() = runTest {
        val result = useCase("02/24")
        assert(result == LocalDate.of(2024, 2, 29))
    }

    @Test
    fun `noisy text LOTE CAD extracts MM slash YY`() = runTest {
        val result = useCase("LOTE 1234 CAD: 09/26")
        assert(result == LocalDate.of(2026, 9, 30))
    }

    @Test
    fun `noisy text Consumir preferente extracts DD slash MM slash YYYY`() = runTest {
        val result = useCase("Consumir preferente: 15/03/2026 Lote: A001")
        assert(result == LocalDate.of(2026, 3, 15))
    }

    @Test
    fun `noisy text BEST BEFORE 12-25 treats as MM slash YY end of month`() = runTest {
        val result = useCase("BEST BEFORE 12-25")
        assert(result == LocalDate.of(2025, 12, 31))
    }

    @Test
    fun `noisy text EXP ISO format extracts YYYY dash MM dash DD`() = runTest {
        val result = useCase("EXP 2026-08-15")
        assert(result == LocalDate.of(2026, 8, 15))
    }

    @Test
    fun `invalid date 32 slash 13 should return null`() = runTest {
        val result = useCase("32/13/2026")
        assert(result == null)
    }

    @Test
    fun `invalid leap day in non-leap year should return null`() = runTest {
        val result = useCase("29/02/2026")
        assert(result == null)
    }

    @Test
    fun `empty string should return null`() = runTest {
        val result = useCase("")
        assert(result == null)
    }

    @Test
    fun `blank string should return null`() = runTest {
        val result = useCase("   ")
        assert(result == null)
    }

    @Test
    fun `multiple dates should return first valid match`() = runTest {
        val result = useCase("01/03/2026 some text 15/06/2026")
        assert(result == LocalDate.of(2026, 3, 1))
    }

    @Test
    fun `european DD slash MM priority over MM slash DD`() = runTest {
        val result = useCase("05/06/2026")
        assert(result == LocalDate.of(2026, 6, 5))
    }

    @Test
    fun `past date should be accepted`() = runTest {
        val result = useCase("15/01/2024")
        assert(result == LocalDate.of(2024, 1, 15))
    }

    @Test
    fun `YY pivot less than 50 should be 20XX`() = runTest {
        val result = useCase("15/03/26")
        assert(result == LocalDate.of(2026, 3, 15))
    }

    @Test
    fun `YY pivot greater or equal 50 should be 19XX`() = runTest {
        val result = useCase("15/03/99")
        assert(result == LocalDate.of(1999, 3, 15))
    }
}
