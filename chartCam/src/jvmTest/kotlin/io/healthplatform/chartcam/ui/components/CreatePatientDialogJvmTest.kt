/**
 * @file CreatePatientDialogJvmTest.kt
 * Contains declarations for CreatePatientDialogJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.first_name
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

/**
 * Test class for CreatePatientDialog on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class CreatePatientDialogJvmTest {
    /**
     * Test create patient dialog on JVM.
     */
    @Test
    fun testCreatePatientDialogJvm() =
        runTest {
            val firstNameStr = getString(Res.string.first_name)
            runComposeUiTest {
                setContent {
                    CreatePatientDialog(
                        onDismissRequest = {},
                        onConfirm = { _, _, _, _, _ -> },
                    )
                }
                onNodeWithText(firstNameStr, useUnmergedTree = true).assertExists()
            }
        }
}
