/**
 * @file DatabaseDriverFactoryWasmJsTest.kt
 * Contains declarations for DatabaseDriverFactoryWasmJsTest.kt.
 */
package io.healthplatform.chartcam.database

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for DatabaseDriverFactory on WasmJS.
 */
class DatabaseDriverFactoryWasmJsTest {
    /**
     * Test database driver factory initialization on WasmJS.
     */
    @Test
    fun testDatabaseDriverFactoryWasmJs() {
        val factory = DatabaseDriverFactory()
        assertNotNull(factory)
    }
}
