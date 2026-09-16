/**
 * @file DatabaseDriverFactoryAndroidTest.kt
 * Contains declarations for DatabaseDriverFactoryAndroidTest.kt.
 */
package io.healthplatform.chartcam.database

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Android host tests for DatabaseDriverFactory.
 */
class DatabaseDriverFactoryAndroidTest {
    /**
     * Verifies creation of DatabaseDriverFactory instance on Android.
     */
    @Test
    fun testDatabaseDriverFactoryAndroid() {
        val factory = DatabaseDriverFactory()
        assertNotNull(factory)
    }
}
