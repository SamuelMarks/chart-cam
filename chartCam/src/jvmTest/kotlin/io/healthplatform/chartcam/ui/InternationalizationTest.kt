package io.healthplatform.chartcam.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.login_signup
import org.jetbrains.compose.resources.stringResource
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class InternationalizationTest {
    @get:Rule
    val rule = createComposeRule()

    @AfterTest
    fun tearDown() {
        setAppLanguage("en")
    }

    @Test
    fun testLanguageSwitchingToSpanish() {
        setAppLanguage("es")

        rule.setContent {
            Text(stringResource(Res.string.login_signup))
        }

        rule.waitForIdle()
        rule.onNodeWithText("Iniciar sesión / Registrarse").assertExists()
    }

    @Test
    fun testLanguageSwitchingToJapanese() {
        setAppLanguage("ja")

        rule.setContent {
            Text(stringResource(Res.string.login_signup))
        }

        rule.waitForIdle()
        rule.onNodeWithText("ログイン / サインアップ").assertExists()
    }
}
