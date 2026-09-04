/**
 * @file FormValidatorTest.kt
 * Contains tests for [FormValidator].
 */
package io.healthplatform.chartcam.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [FormValidator] profiles including regex, ranges, and required fields.
 */
class FormValidatorTest {
    /**
     * Verifies that the required field constraint correctly rejects null or blank inputs,
     * while accepting them when not required.
     */
    @Test
    fun testRequiredFieldValidation() {
        assertFalse(FormValidator.validateText(null, required = true), "Null should fail if required")
        assertFalse(FormValidator.validateText("", required = true), "Empty string should fail if required")
        assertTrue(FormValidator.validateText(null, required = false), "Null should pass if not required")
        assertTrue(FormValidator.validateText("Valid", required = true), "Valid text should pass")
    }

    /**
     * Verifies that the regex constraint correctly matches text inputs against a pattern.
     */
    @Test
    fun testRegexValidation() {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        assertTrue(FormValidator.validateText("test@example.com", required = true, regex = emailRegex), "Valid email should pass regex")
        assertFalse(FormValidator.validateText("invalid-email", required = true, regex = emailRegex), "Invalid email should fail regex")
        assertTrue(
            FormValidator.validateText(null, required = false, regex = emailRegex),
            "Null should pass if not required, even with regex",
        )

        val phoneRegex = "^\\+?[1-9]\\d{1,14}\$"
        assertTrue(FormValidator.validateText("+1234567890", required = true, regex = phoneRegex), "Valid phone should pass")
        assertFalse(FormValidator.validateText("not-a-phone", required = true, regex = phoneRegex), "Invalid phone should fail")

        val identifierRegex = "^[A-Z]{3}-\\d{4}\$"
        assertTrue(FormValidator.validateText("ABC-1234", required = true, regex = identifierRegex), "Valid identifier should pass")
        assertFalse(FormValidator.validateText("abc-1234", required = true, regex = identifierRegex), "Invalid identifier should fail")
    }

    /**
     * Verifies boundary and edge cases in form inputs (e.g., extremely long strings).
     */
    @Test
    fun testBoundaryTextValidation() {
        val extremelyLongString = "A".repeat(10000)
        assertTrue(FormValidator.validateText(extremelyLongString, required = true), "Extremely long string should pass if no length limit")

        val customLengthRegex = "^.{1,10}\$"
        assertFalse(
            FormValidator.validateText(extremelyLongString, required = true, regex = customLengthRegex),
            "Extremely long string should fail length regex",
        )
        assertTrue(
            FormValidator.validateText("1234567890", required = true, regex = customLengthRegex),
            "10 character string should pass length regex",
        )
    }

    /**
     * Verifies that numeric range constraints (min/max) correctly accept and reject inputs, including edge boundaries.
     */
    @Test
    fun testNumericRangeValidation() {
        assertTrue(FormValidator.validateNumber(15.0, required = true, min = 10.0, max = 20.0), "Value within range should pass")
        assertFalse(FormValidator.validateNumber(5.0, required = true, min = 10.0, max = 20.0), "Value below min should fail")
        assertFalse(FormValidator.validateNumber(25.0, required = true, min = 10.0, max = 20.0), "Value above max should fail")

        // Unbounded ends
        assertTrue(FormValidator.validateNumber(25.0, required = true, min = 10.0), "Value above min should pass if no max")
        assertFalse(FormValidator.validateNumber(5.0, required = true, min = 10.0), "Value below min should fail if no max")

        // Exact boundary conditions
        assertTrue(FormValidator.validateNumber(10.0, required = true, min = 10.0, max = 20.0), "Value equal to min should pass")
        assertTrue(FormValidator.validateNumber(20.0, required = true, min = 10.0, max = 20.0), "Value equal to max should pass")
        assertFalse(FormValidator.validateNumber(9.99999, required = true, min = 10.0, max = 20.0), "Value just below min should fail")
        assertFalse(FormValidator.validateNumber(20.00001, required = true, min = 10.0, max = 20.0), "Value just above max should fail")

        // Out of bound numbers
        assertFalse(
            FormValidator.validateNumber(Double.MAX_VALUE, required = true, max = 100.0),
            "Double.MAX_VALUE should fail max constraint",
        )
        assertFalse(
            FormValidator.validateNumber(-Double.MAX_VALUE, required = true, min = 0.0),
            "-Double.MAX_VALUE should fail min constraint",
        )
    }
}
