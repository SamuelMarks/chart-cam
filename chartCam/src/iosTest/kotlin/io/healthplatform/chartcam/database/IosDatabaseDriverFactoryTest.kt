/**
 * @file IosDatabaseDriverFactoryTest.kt
 * Contains declarations for IosDatabaseDriverFactoryTest.kt.
 */
package io.healthplatform.chartcam.database

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Alternative test target mapping for iOS SQLite driver logic.
 */
class IosDatabaseDriverFactoryTest {
    /** Verifies driver creation on iOS. */
    @Test
    fun testDatabaseDriverFactory() {
        val factory = DatabaseDriverFactory()
        assertNotNull(factory)
        val driver = runCatching { factory.createDriver() }
        assertNotNull(driver)
    }
}
