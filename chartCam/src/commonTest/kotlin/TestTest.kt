/**
 * @file TestTest.kt
 * Contains declarations for TestTest.kt.
 */
package test

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Tests for Test.kt.
 */
class TestTest {
    /**
     * Tests that foo() returns a non-null Date.
     */
    @Test
    fun testFoo() {
        val d = foo()
        assertNotNull(d.value)
    }
}
