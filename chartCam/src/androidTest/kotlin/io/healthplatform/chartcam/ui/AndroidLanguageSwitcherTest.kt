/**
 * @file AndroidLanguageSwitcherTest.kt
 * Contains declarations for AndroidLanguageSwitcherTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.healthplatform.chartcam.AndroidAppInit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class AndroidLanguageSwitcherTest {
    @Before
    fun setup() {
        AndroidAppInit.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testChangeAppLanguage() {
        val initialLocale = Locale.getDefault()
        try {
            changeAppLanguage("es")
            // Verify context was called without crash
        } finally {
            // Restore
            changeAppLanguage(initialLocale.language)
        }
    }
}
