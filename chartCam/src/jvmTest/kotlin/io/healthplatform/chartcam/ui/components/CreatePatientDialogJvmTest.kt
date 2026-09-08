/**
 * @file CreatePatientDialogJvmTest.kt
 * Contains declarations for CreatePatientDialogJvmTest.kt.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.first_name
import chartcam.chartcam.generated.resources.gender
import chartcam.chartcam.generated.resources.gender_unknown
import chartcam.chartcam.generated.resources.last_name
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

/**
 * Test class for CreatePatientDialog on JVM.
 */
@OptIn(ExperimentalTestApi::class)
class CreatePatientDialogJvmTest {
    /**
     * Test create patient dialog on JVM in Western ordering (English).
     */
    @Test
    fun testCreatePatientDialogJvm() =
        runTest {
            setAppLanguage("en")
            val firstNameStr = getString(Res.string.first_name)
            val lastNameStr = getString(Res.string.last_name)
            runComposeUiTest {
                setContent {
                    CreatePatientDialog(
                        onDismissRequest = {},
                        onConfirm = { _, _, _, _, _ -> },
                    )
                }
                waitForIdle()
                onNodeWithText(firstNameStr, useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText(lastNameStr, useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText("DOB (YYYY-MM-DD)", useUnmergedTree = true).assertIsDisplayed()
            }
        }

    /**
     * Test create patient dialog on JVM in East Asian ordering (Japanese / Chinese).
     */
    @Test
    fun testCreatePatientDialogEastAsianOrder() =
        runTest {
            setAppLanguage("ja")
            val firstNameStr = getString(Res.string.first_name)
            val lastNameStr = getString(Res.string.last_name)
            runComposeUiTest {
                setContent {
                    CreatePatientDialog(
                        onDismissRequest = {},
                        onConfirm = { _, _, _, _, _ -> },
                    )
                }
                waitForIdle()
                onNodeWithText(lastNameStr, useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText(firstNameStr, useUnmergedTree = true).assertIsDisplayed()
            }
            // Reset to English
            setAppLanguage("en")
        }

    /**
     * Test create patient dialog gender selection on JVM.
     */
    @Test
    fun testCreatePatientDialogGenderSelection() =
        runTest {
            setAppLanguage("en")
            val genderStr = getString(Res.string.gender)
            val unknownStr = getString(Res.string.gender_unknown)
            runComposeUiTest {
                setContent {
                    CreatePatientDialog(
                        onDismissRequest = {},
                        onConfirm = { _, _, _, _, _ -> },
                    )
                }
                waitForIdle()
                onNodeWithText(genderStr, useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText(unknownStr, useUnmergedTree = true).assertIsDisplayed()
            }
        }
}
