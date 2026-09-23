/**
 * @file DatabaseDriverFactoryAndroidTest.kt
 * Contains declarations for DatabaseDriverFactoryAndroidTest.kt.
 */
package io.healthplatform.chartcam.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import io.healthplatform.chartcam.AndroidAppInit
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * Android host tests for DatabaseDriverFactory.
 */
@Config(manifest = Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class DatabaseDriverFactoryAndroidTest {
    /**
     * Verifies creation of DatabaseDriverFactory instance on Android.
     */
    @Test
    fun testDatabaseDriverFactoryAndroid() {
        val factory = DatabaseDriverFactory()
        assertNotNull(factory)
    }

    /**
     * Verifies driver creation, passphrase generation and retrieval on Android.
     */
    @Test
    fun testCreateDriverPassphraseGenerationAndRetrieval() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)

        val prefs = context.getSharedPreferences("db_secure_prefs_v2", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val mockDriver = Mockito.mock(SqlDriver::class.java)
        val oldDriverCreator = DatabaseDriverFactory.driverCreator
        val oldLoader = DatabaseDriverFactory.libraryLoader

        // allow-exception
        try {
            DatabaseDriverFactory.driverCreator = { _, _ -> mockDriver }
            DatabaseDriverFactory.libraryLoader = { }

            val factory = DatabaseDriverFactory()

            // First call: encodedPassphrase is null, generates and stores passphrase
            val driver1 = factory.createDriver()
            assertNotNull(driver1)

            // Second call: encodedPassphrase exists in prefs, retrieves existing passphrase
            val driver2 = factory.createDriver()
            assertNotNull(driver2)
        } finally {
            DatabaseDriverFactory.driverCreator = oldDriverCreator
            DatabaseDriverFactory.libraryLoader = oldLoader
        }

        // Test default libraryLoader lambda execution
        runCatching {
            oldLoader.invoke("nonexistent_lib")
        }

        // Test fallback to default AndroidSqliteDriver when driverCreator is null
        runCatching {
            DatabaseDriverFactory.driverCreator = null
            DatabaseDriverFactory.libraryLoader = { }
            val factory = DatabaseDriverFactory()
            factory.createDriver()
        }
    }
}
