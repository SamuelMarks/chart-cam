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

@OptIn(ExperimentalTestApi::class)
class InternationalizationJvmTest {
    @get:Rule
    val rule = createComposeRule()

    @AfterTest
    fun tearDown() {
        setAppLanguage("en")
    }

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
