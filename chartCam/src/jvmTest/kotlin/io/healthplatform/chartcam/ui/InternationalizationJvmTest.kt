/**
 * @file InternationalizationJvmTest.kt
 * Contains declarations for InternationalizationJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.login_signup
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.stringResource
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Test class for Internationalization on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class InternationalizationJvmTest {
    /**
     * The compose rule.
     */
    @get:Rule
    val rule = createComposeRule()

    private var originalLocale: java.util.Locale? = null

    @kotlin.test.BeforeTest
    fun setup() {
        originalLocale = java.util.Locale.getDefault()
    }

    /**
     * Tears down the test by resetting the app language to the original locale.
     */
    @AfterTest
    fun tearDown() {
        originalLocale?.let {
            java.util.Locale.setDefault(it)
            currentLanguageState.value = it.language
        } ?: setAppLanguage("en")
    }

    /**
     * Tests language switching to Spanish.
     */
    @Test
    fun testLanguageSwitchingToSpanish() =
        runTest {
            setAppLanguage("es")

            rule.setContent {
                Text(stringResource(Res.string.login_signup))
            }

            rule.waitForIdle()
            // Instead of hardcoding "Iniciar sesión / Registrarse", we could use a tag,
            // but for this specific i18n test, verifying the hardcoded text IS the test's purpose.
            // We will leave these specific assertions in this specific test because testing i18n means testing the actual translated output.
            rule.onNodeWithText("Iniciar sesión / Registrarse").assertExists()
        }

    /**
     * Tests language switching to Japanese.
     */
    @Test
    fun testLanguageSwitchingToJapanese() =
        runTest {
            setAppLanguage("ja")

            rule.setContent {
                Text(stringResource(Res.string.login_signup))
            }

            rule.waitForIdle()
            rule.onNodeWithText("ログイン / サインアップ").assertExists()
        }
}
