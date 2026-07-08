package io.healthplatform.chartcam.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.healthplatform.chartcam.AndroidAppInit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.assertEquals

@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class LanguageSwitcherAndroidTest {
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)
    }

    @After
    fun teardown() {
        val field = AndroidAppInit::class.java.getDeclaredField("context")
        field.isAccessible = true
        field.set(AndroidAppInit, null)
    }

    @Test
    fun testChangeAppLanguageAndroid() {
        val originalLocale = Locale.getDefault()
        try {
            changeAppLanguage("ja")
            assertEquals("ja", Locale.getDefault().language)

            val context = AndroidAppInit.getContext()
            assertEquals(
                "ja",
                context.resources.configuration.locales[0]
                    .language,
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
