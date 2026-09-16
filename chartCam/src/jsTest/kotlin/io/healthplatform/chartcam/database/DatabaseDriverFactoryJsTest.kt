/**
 * @file DatabaseDriverFactoryJsTest.kt
 * Contains declarations for DatabaseDriverFactoryJsTest.kt.
 */
package io.healthplatform.chartcam.database

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for DatabaseDriverFactory on JS.
 */
class DatabaseDriverFactoryJsTest {
    /**
     * Test database driver factory initialization on JS.
     */
    @Test
    fun testDatabaseDriverFactoryJs() {
        val factory = DatabaseDriverFactory()
        assertNotNull(factory)
    }
}
