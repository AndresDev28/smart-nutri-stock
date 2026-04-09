package com.decathlon.smartnutristock.domain.usecase

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DateExtractorUseCase @Inject constructor() {

    private val ddMmYyyySlash = Regex("""\b(\d{2})/(\d{2})/(\d{4})\b""")
    private val ddMmYyyyDash = Regex("""\b(\d{2})-(\d{2})-(\d{4})\b""")
    private val ddMmYyyyDot = Regex("""\b(\d{2})\.(\d{2})\.(\d{4})\b""")
    private val ddMmYySlash = Regex("""\b(\d{2})/(\d{2})/(\d{2})\b""")
    private val isoYyyyMmDd = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
    private val mmYyyySlash = Regex("""(?<!\d/)(\d{2})/(\d{4})(?!\d)""")
    private val mmYySlash = Regex("""(?<!\d/)(\d{2})/(\d{2})(?!\d)(?!/)""")
    private val mmYyyyDash = Regex("""(?<!\d-)(\d{2})-(\d{4})(?!\d)""")
    private val mmYyDash = Regex("""(?<!\d-)(\d{2})-(\d{2})(?!\d)(?!-)""")

    private val formatterDdMmYyyy = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private fun resolveYear(yy: Int): Int =
        if (yy < 50) 2000 + yy else 1900 + yy

    private fun isValidDate(day: Int, month: Int, year: Int): Boolean {
        if (month !in 1..12) return false
        if (day !in 1..31) return false
        return try {
            val date = LocalDate.of(year, month, day)
            formatterDdMmYyyy.format(date) == String.format("%02d/%02d/%04d", day, month, year)
        } catch (e: Exception) {
            false
        }
    }

    private fun tryDdMmYyyy(text: String, regex: Regex): LocalDate? {
        for (match in regex.findAll(text)) {
            val (dd, mm, yyyy) = match.destructured
            val day = dd.toInt()
            val month = mm.toInt()
            val year = yyyy.toInt()
            if (isValidDate(day, month, year)) {
                return LocalDate.of(year, month, day)
            }
        }
        return null
    }

    private fun tryDdMmYy(text: String): LocalDate? {
        for (match in ddMmYySlash.findAll(text)) {
            val (dd, mm, yy) = match.destructured
            val day = dd.toInt()
            val month = mm.toInt()
            val year = resolveYear(yy.toInt())
            if (isValidDate(day, month, year)) {
                return LocalDate.of(year, month, day)
            }
        }
        return null
    }

    private fun tryIso(text: String): LocalDate? {
        for (match in isoYyyyMmDd.findAll(text)) {
            val (yyyy, mm, dd) = match.destructured
            val year = yyyy.toInt()
            val month = mm.toInt()
            val day = dd.toInt()
            if (isValidDate(day, month, year)) {
                return LocalDate.of(year, month, day)
            }
        }
        return null
    }

    private fun tryMmYyyy(text: String): LocalDate? {
        for (match in mmYyyySlash.findAll(text)) {
            val (mm, yyyy) = match.destructured
            val month = mm.toInt()
            val year = yyyy.toInt()
            if (month in 1..12) {
                return YearMonth.of(year, month).atEndOfMonth()
            }
        }
        for (match in mmYyyyDash.findAll(text)) {
            val (mm, yyyy) = match.destructured
            val month = mm.toInt()
            val year = yyyy.toInt()
            if (month in 1..12) {
                return YearMonth.of(year, month).atEndOfMonth()
            }
        }
        return null
    }

    private fun tryMmYy(text: String): LocalDate? {
        for (match in mmYySlash.findAll(text)) {
            val (a, b) = match.destructured
            val first = a.toInt()
            val second = b.toInt()
            val month = first
            val year = resolveYear(second)
            if (month in 1..12 && second in 0..99) {
                return YearMonth.of(year, month).atEndOfMonth()
            }
        }
        for (match in mmYyDash.findAll(text)) {
            val (a, b) = match.destructured
            val first = a.toInt()
            val second = b.toInt()
            val month = first
            val year = resolveYear(second)
            if (month in 1..12 && second in 0..99) {
                return YearMonth.of(year, month).atEndOfMonth()
            }
        }
        return null
    }

    operator fun invoke(rawText: String): LocalDate? {
        if (rawText.isBlank()) return null

        tryDdMmYyyy(rawText, ddMmYyyySlash)?.let { return it }
        tryDdMmYyyy(rawText, ddMmYyyyDash)?.let { return it }
        tryDdMmYyyy(rawText, ddMmYyyyDot)?.let { return it }
        tryIso(rawText)?.let { return it }
        tryDdMmYy(rawText)?.let { return it }
        tryMmYyyy(rawText)?.let { return it }
        tryMmYy(rawText)?.let { return it }

        return null
    }
}
