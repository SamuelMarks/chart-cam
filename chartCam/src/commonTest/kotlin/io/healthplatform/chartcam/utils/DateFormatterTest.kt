/**
 * @file DateFormatterTest.kt
 * Contains tests for localized date and time formatting.
 */
package io.healthplatform.chartcam.utils

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [formatLocalizedDate] to ensure proper localized parsing of FHIR dates.
 */
class DateFormatterTest {
    /**
     * Verifies that formatting a raw FHIR date string returns a non-empty,
     * reasonably formatted localized string (as the exact format depends on the test runner's locale).
     */
    @Test
    fun testFormatLocalizedDate() {
        val fhirDate = "1990-01-01T10:00:00Z"
        val formatted = formatLocalizedDate(fhirDate)

        // Since we don't strictly know the environment's locale (could be US, UK, etc.),
        // we assert that it correctly processed the date without crashing and returned something meaningful.
        assertTrue(formatted.isNotEmpty(), "Formatted date should not be empty")

        // In most locales, it will contain the year 1990.
        assertTrue(formatted.contains("1990"), "Formatted date should contain the parsed year '1990'")
    }

    /**
     * Verifies getLocalizedDatePattern for all language branches and default argument.
     */
    @Test
    fun testGetLocalizedDatePattern() {
        assertEquals("YYYY/MM/DD", getLocalizedDatePattern("ja"))
        assertEquals("YYYY/MM/DD", getLocalizedDatePattern("zh"))
        assertEquals("YYYY/MM/DD", getLocalizedDatePattern("zh-TW"))
        assertEquals("DD/MM/YYYY", getLocalizedDatePattern("es"))
        assertEquals("DD/MM/YYYY", getLocalizedDatePattern("he"))
        assertEquals("DD/MM/YYYY", getLocalizedDatePattern("es-ES"))
        assertEquals("DD/MM/YYYY", getLocalizedDatePattern("en-GB"))
        assertEquals("DD/MM/YYYY", getLocalizedDatePattern("en-AU"))
        assertEquals("DD/MM/YYYY", getLocalizedDatePattern("fr"))
        assertEquals("MM/DD/YYYY", getLocalizedDatePattern("en"))
        assertEquals("MM/DD/YYYY", getLocalizedDatePattern("en-US"))
        assertEquals("YYYY-MM-DD", getLocalizedDatePattern("other"))
        assertTrue(getLocalizedDatePattern().isNotEmpty())
    }

    /**
     * Verifies getLocalizedDateTimePattern for all language branches and default argument.
     */
    @Test
    fun testGetLocalizedDateTimePattern() {
        assertEquals("YYYY/MM/DD HH:MM", getLocalizedDateTimePattern("ja"))
        assertEquals("YYYY/MM/DD HH:MM", getLocalizedDateTimePattern("zh"))
        assertEquals("DD/MM/YYYY HH:MM", getLocalizedDateTimePattern("es"))
        assertEquals("DD/MM/YYYY HH:MM", getLocalizedDateTimePattern("he"))
        assertEquals("MM/DD/YYYY HH:MM", getLocalizedDateTimePattern("en"))
        assertTrue(getLocalizedDateTimePattern().isNotEmpty())
    }

    /**
     * Verifies that formatting a raw FHIR datetime string returns a non-empty localized string.
     */
    @Test
    fun testFormatLocalizedDateTime() {
        val fhirDateTime = "2026-09-08T14:30:00Z"
        val formatted = formatLocalizedDateTime(fhirDateTime, "en")
        assertTrue(formatted.isNotEmpty(), "Formatted datetime should not be empty")
        assertTrue(formatted.contains("2026"), "Formatted datetime should contain the year '2026'")

        val emptyFormatted = formatLocalizedDateTime("", "en")
        assertEquals("", emptyFormatted)

        val invalidFormatted = formatLocalizedDateTime("invalid-datetime", "en")
        assertEquals("invalid-datetime", invalidFormatted)

        val esFormatted = formatLocalizedDateTime(fhirDateTime, "es")
        assertTrue(esFormatted.isNotEmpty())

        val jaFormatted = formatLocalizedDateTime(fhirDateTime, "ja")
        assertTrue(jaFormatted.isNotEmpty())

        val utcDt = parseFhirDateTime("2026-09-08T14:30:00Z").getOrThrow()
        assertEquals("09/08/2026 14:30 Z", utcDt.formatLocalizedWithOffset("en").getOrThrow())

        val posDt = parseFhirDateTime("2026-09-08T14:30:00+02:00").getOrThrow()
        assertEquals("09/08/2026 14:30 +02:00", posDt.formatLocalizedWithOffset("en").getOrThrow())

        val negDt = parseFhirDateTime("2026-09-08T14:30:00-05:00").getOrThrow()
        assertEquals("09/08/2026 14:30 -05:00", negDt.formatLocalizedWithOffset("en").getOrThrow())
    }

    /**
     * Verifies formatDateForPattern for all DatePattern variants.
     */
    @Test
    fun testFormatDateForPattern() {
        val date = LocalDate(2026, 9, 12)
        assertEquals("12/09/2026", formatDateForPattern(date, DatePattern.DAY_FIRST))
        assertEquals("09/12/2026", formatDateForPattern(date, DatePattern.MONTH_FIRST))
        assertEquals("2026/09/12", formatDateForPattern(date, DatePattern.YEAR_FIRST))
        assertEquals("2026-09-12", formatDateForPattern(date, DatePattern.ISO_STANDARD))
    }

    /**
     * Verifies safe parsing of ISO date strings into Result.
     */
    @Test
    fun testParseIsoDate() {
        val validResult = parseIsoDate("2026-09-12")
        assertTrue(validResult.isSuccess)
        assertEquals(LocalDate(2026, 9, 12), validResult.getOrNull())

        val invalidResult = parseIsoDate("invalid-date-string")
        assertTrue(invalidResult.isFailure)
    }

    /**
     * Verifies safe parsing of FhirDate and formatting across locales with Result percolation.
     */
    @Test
    fun testParseFhirDateAndFormatLocalized() {
        val yearResult = parseFhirDate("1985")
        assertTrue(yearResult.isSuccess)
        val year = yearResult.getOrNull()
        assertTrue(year is dev.ohs.fhir.model.r4.FhirDate.Year)
        assertEquals("1985", year.formatLocalized().getOrNull())
        assertEquals("1985", year.formatLocalized("en").getOrNull())

        val ymResult = parseFhirDate("1985-03")
        assertTrue(ymResult.isSuccess)
        val ym = ymResult.getOrNull()
        assertTrue(ym is dev.ohs.fhir.model.r4.FhirDate.YearMonth)
        assertEquals("1985/03", ym.formatLocalized("ja").getOrNull())
        assertEquals("03/1985", ym.formatLocalized("es").getOrNull())
        assertEquals("03/1985", ym.formatLocalized("en").getOrNull())
        assertEquals("1985-03", ym.formatLocalized("").getOrNull())

        val fullDateResult = parseFhirDate("1985-03-15")
        assertTrue(fullDateResult.isSuccess)
        val fullDate = fullDateResult.getOrNull()
        assertTrue(fullDate is dev.ohs.fhir.model.r4.FhirDate.Date)
        assertEquals("15/03/1985", fullDate.formatLocalized("es").getOrNull())
        assertEquals("03/15/1985", fullDate.formatLocalized("en").getOrNull())
        assertEquals("1985/03/15", fullDate.formatLocalized("ja").getOrNull())

        val invalidResult = parseFhirDate("not-a-date")
        assertTrue(invalidResult.isFailure)
    }

    /**
     * Verifies safe parsing of FhirDateTime and formatting across locales with Result percolation.
     */
    @Test
    fun testParseFhirDateTimeAndFormatLocalized() {
        val yearDtResult = parseFhirDateTime("1985")
        assertTrue(yearDtResult.isSuccess)
        val yearDt = yearDtResult.getOrNull()
        assertTrue(yearDt is dev.ohs.fhir.model.r4.FhirDateTime.Year)
        assertEquals("1985", yearDt.formatLocalized().getOrNull())

        val ymDtResult = parseFhirDateTime("1985-03")
        assertTrue(ymDtResult.isSuccess)
        val ymDt = ymDtResult.getOrNull()
        assertTrue(ymDt is dev.ohs.fhir.model.r4.FhirDateTime.YearMonth)
        assertEquals("1985/03", ymDt.formatLocalized("ja").getOrNull())
        assertEquals("03/1985", ymDt.formatLocalized("es").getOrNull())
        assertEquals("03/1985", ymDt.formatLocalized("en").getOrNull())
        assertEquals("1985-03", ymDt.formatLocalized("").getOrNull())

        val dateDtResult = parseFhirDateTime("1985-03-15")
        assertTrue(dateDtResult.isSuccess)
        val dateDt = dateDtResult.getOrNull()
        assertTrue(dateDt is dev.ohs.fhir.model.r4.FhirDateTime.Date)
        assertEquals("15/03/1985", dateDt.formatLocalized("es").getOrNull())

        val dtResult = parseFhirDateTime("2026-09-08T14:30:00Z")
        assertTrue(dtResult.isSuccess)
        val dt = dtResult.getOrNull()
        assertTrue(dt is dev.ohs.fhir.model.r4.FhirDateTime.DateTime)
        assertEquals("08/09/2026 14:30", dt.formatLocalized("es").getOrNull())
        assertEquals("09/08/2026 14:30", dt.formatLocalized("en").getOrNull())
        assertEquals("2026/09/08 14:30", dt.formatLocalized("ja").getOrNull())
        assertNotNull(dt.formatLocalized().getOrNull())

        val invalidResult = parseFhirDateTime("bad-datetime")
        assertTrue(invalidResult.isFailure)
    }

    /**
     * Verifies formatLocalizedDateCatching and formatLocalizedDateTimeCatching return Result.
     */
    @Test
    fun testFormatLocalizedCatching() {
        val dateRes = formatLocalizedDateCatching("1990-05-20", "en")
        assertTrue(dateRes.isSuccess)
        assertEquals("05/20/1990", dateRes.getOrNull())

        val dateResDefaultLang = formatLocalizedDateCatching("1990-05-20")
        assertTrue(dateResDefaultLang.isSuccess)

        val dtInDateRes = formatLocalizedDateCatching("2026-09-08T14:30:00Z", "en")
        assertTrue(dtInDateRes.isSuccess)
        assertEquals("09/08/2026 14:30", dtInDateRes.getOrNull())

        val dtRes = formatLocalizedDateTimeCatching("2026-09-08T14:30:00Z", "en")
        assertTrue(dtRes.isSuccess)
        assertEquals("09/08/2026 14:30", dtRes.getOrNull())

        val dtResDefaultLang = formatLocalizedDateTimeCatching("2026-09-08T14:30:00Z")
        assertTrue(dtResDefaultLang.isSuccess)

        val emptyRes = formatLocalizedDateCatching("", "en")
        assertTrue(emptyRes.isSuccess)
        assertEquals("", emptyRes.getOrNull())

        val spacesRes = formatLocalizedDateCatching("   ", "en")
        assertTrue(spacesRes.isSuccess)
        assertEquals("", spacesRes.getOrNull())

        val badRes = formatLocalizedDateCatching("invalid-date", "en")
        assertTrue(badRes.isFailure)

        val dt = parseFhirDateTime("2026-09-08T14:30:00Z").getOrThrow()
        assertNotNull(dt.formatLocalizedWithOffset().getOrNull())

        val dtWithSec = parseFhirDateTime("2026-09-08T14:30:45Z").getOrThrow()
        val secFormatted = dtWithSec.formatLocalized("en").getOrThrow()
        assertEquals("09/08/2026 14:30:45", secFormatted)
    }
}
