/**
 * @file GreetingTest.kt
 * Contains declarations for GreetingTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertTrue

class GreetingTest {
    @Test
    fun testGreeting() {
        val greeting = Greeting()
        val text = greeting.greet()
        assertTrue(text.startsWith("Hello, "))
        assertTrue(text.endsWith("!"))
    }
}
