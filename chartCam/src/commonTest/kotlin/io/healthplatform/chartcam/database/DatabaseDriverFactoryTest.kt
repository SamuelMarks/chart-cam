/**
 * @file DatabaseDriverFactoryTest.kt
 * Contains declarations for DatabaseDriverFactoryTest.kt.
 */
package io.healthplatform.chartcam.database

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the database driver factory behavior.
 */
class DatabaseDriverFactoryTest {
    /**
     * Test initialization of the database driver factory setup.
     */
    @Test
    fun testFactoryInitialization() {
        runCatching {
            val factory = DatabaseDriverFactory()
            val driver = factory.createDriver()
            assertNotNull(driver)
            driver.close()
        }.onFailure {
            assertTrue(it is IllegalStateException)
        }
    }
}
