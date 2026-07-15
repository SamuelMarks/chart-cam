/**
 * Provides common unit tests for the Compose application logic.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for common shared logic across all targets.
 */
class ComposeAppCommonJvmTest {
    /**
     * Verifies that the [Greeting] class generates a valid, non-empty string
     * containing expected greeting text structure.
     */
    @Test
    fun testGreetingGeneration() {
        val classUnderTest = Greeting()
        val result = classUnderTest.greet()

        // Assert the result is not null and not empty
        assertNotNull(result, "Greeting result should not be null")
        assertTrue(result.isNotEmpty(), "Greeting result should not be empty")

        // Platform name depends on the test runner (JVM usually), but we verify structure
        assertTrue(result.startsWith("Hello, "), "Greeting should start with 'Hello, '")
    }
}
