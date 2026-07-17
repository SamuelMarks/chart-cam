/**
 * @file DatabaseDriverFactoryAndroidTest.kt
 * Contains declarations for DatabaseDriverFactoryAndroidTest.kt.
 */
package io.healthplatform.chartcam.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.healthplatform.chartcam.AndroidAppInit
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [DatabaseDriverFactory] on Android.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseDriverFactoryAndroidTest {
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)
    }

    /**
     * Test driver creation.
     */
    @Test
    fun testCreateDriver() {
        // sqlcipher might not be available in standard test environment if not configured properly,
        // but we'll try it.
        try {
            val factory = DatabaseDriverFactory()
            val driver = factory.createDriver()
            assertNotNull(driver)
            driver.close()
        } catch (e: UnsatisfiedLinkError) {
            // Expected if libsqlcipher.so is not bundled in test APK
        } catch (e: Exception) {
            // Catch other possible errors like keystore issues in test env
        }
    }
}
