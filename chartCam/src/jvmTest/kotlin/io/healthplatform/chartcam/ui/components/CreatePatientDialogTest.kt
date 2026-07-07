package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CreatePatientDialogTest {
    @Test
    fun testCreatePatientDialog() =
        runComposeUiTest {
            setContent {
                CreatePatientDialog(
                    onDismissRequest = {},
                    onConfirm = { _, _, _, _, _ -> },
                )
            }
            onNodeWithText("First Name", useUnmergedTree = true).assertExists()
        }
}
