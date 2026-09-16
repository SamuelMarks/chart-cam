/**
 * @file DatabaseDriverFactoryIosTest.kt
 * Contains declarations for DatabaseDriverFactoryIosTest.kt.
 */
package io.healthplatform.chartcam.database

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * iOS specifics for the database driver initialization tests.
 */
class DatabaseDriverFactoryIosTest {
    /**
     * Verifies that DatabaseDriverFactory instantiates cleanly on iOS.
     */
    @Test
    fun testDatabaseDriverFactoryIos() {
        val factory = DatabaseDriverFactory()
        assertNotNull(factory)
    }
}
