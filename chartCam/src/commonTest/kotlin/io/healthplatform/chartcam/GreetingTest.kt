/**
 * @file GreetingTest.kt
 * Contains declarations for GreetingTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for the greeting service.
 */
class GreetingTest {
    /**
     * Test generating a greeting message.
     */
    @Test
    fun testGreeting() {
        val greeting = Greeting()
        val text = greeting.greet()
        assertTrue(text.startsWith("Hello, "))
        assertTrue(text.endsWith("!"))
    }
}
