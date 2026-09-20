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
        val dbFile = java.io.File("chartcam_desktop.db")
        if (dbFile.exists()) dbFile.delete()

        val factory = DatabaseDriverFactory()
        val driver1 = factory.createDriver()
        assertNotNull(driver1)
        driver1.close()

        val driver2 = factory.createDriver()
        assertNotNull(driver2)
        driver2.close()

        if (dbFile.exists()) dbFile.delete()
    }
}
