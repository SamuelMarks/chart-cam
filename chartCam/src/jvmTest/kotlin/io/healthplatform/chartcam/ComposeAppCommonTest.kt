package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Unit tests for common shared logic.
 */
class ComposeAppCommonTest {

    /**
     * Verifies that the Greeting class returns a non-empty string
     * containing text.
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