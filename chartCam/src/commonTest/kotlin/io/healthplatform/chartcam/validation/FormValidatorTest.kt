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
    }

    /**
     * Verifies that numeric range constraints (min/max) correctly accept and reject inputs.
     */
    @Test
    fun testNumericRangeValidation() {
        assertTrue(FormValidator.validateNumber(15.0, required = true, min = 10.0, max = 20.0), "Value within range should pass")
        assertFalse(FormValidator.validateNumber(5.0, required = true, min = 10.0, max = 20.0), "Value below min should fail")
        assertFalse(FormValidator.validateNumber(25.0, required = true, min = 10.0, max = 20.0), "Value above max should fail")

        // Unbounded ends
        assertTrue(FormValidator.validateNumber(25.0, required = true, min = 10.0), "Value above min should pass if no max")
        assertFalse(FormValidator.validateNumber(5.0, required = true, min = 10.0), "Value below min should fail if no max")
    }
}
