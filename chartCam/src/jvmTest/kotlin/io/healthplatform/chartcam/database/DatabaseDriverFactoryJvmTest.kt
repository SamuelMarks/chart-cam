/**
 * @file DatabaseDriverFactoryJvmTest.kt
 * Contains declarations for DatabaseDriverFactoryJvmTest.kt.
 */
package io.healthplatform.chartcam.database

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for DatabaseDriverFactory on JVM.
 */
class DatabaseDriverFactoryJvmTest {
    /**
     * Verifies SQLite driver creation on JVM.
     */
    @Test
    fun testDatabaseDriverFactoryJvm() {
        val factory = DatabaseDriverFactory()
        val driver = factory.createDriver()
        assertNotNull(driver)
        driver.close()
    }
}
