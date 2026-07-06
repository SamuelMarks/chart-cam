package io.healthplatform.chartcam.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import org.mockito.Mockito
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LoginScreenTest {
    @Test
    fun testLoginScreenRenders() =
        runComposeUiTest {
            val mockAuthRepo = Mockito.mock(AuthRepository::class.java)
            val viewModel = LoginViewModel(mockAuthRepo)

            setContent {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {},
                )
            }

            onRoot().assertExists()
        }
}
