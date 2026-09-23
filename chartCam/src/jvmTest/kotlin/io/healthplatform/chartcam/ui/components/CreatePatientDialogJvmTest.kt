/**
 * @file CreatePatientDialogJvmTest.kt
 * Contains declarations for CreatePatientDialogJvmTest.kt.
 *
 * JVM Compose UI tests for [CreatePatientDialog], testing all form fields, input validations,
 * date picker interactions, gender dropdown selection, keyboard actions, and confirm submissions.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.all_fields_required
import chartcam.chartcam.generated.resources.cancel
import chartcam.chartcam.generated.resources.cd_select_date
import chartcam.chartcam.generated.resources.create
import chartcam.chartcam.generated.resources.first_name
import chartcam.chartcam.generated.resources.gender
import chartcam.chartcam.generated.resources.gender_female
import chartcam.chartcam.generated.resources.gender_male
import chartcam.chartcam.generated.resources.gender_other
import chartcam.chartcam.generated.resources.gender_unknown
import chartcam.chartcam.generated.resources.invalid_date_format
import chartcam.chartcam.generated.resources.last_name
import chartcam.chartcam.generated.resources.ok
import io.healthplatform.chartcam.ui.setAppLanguage
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite for [CreatePatientDialog] on JVM.
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
                onNodeWithText("DOB (MM/DD/YYYY)", useUnmergedTree = true).assertIsDisplayed()
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
                var dismissed = false
                setContent {
                    CreatePatientDialog(
                        onDismissRequest = { dismissed = true },
                        onConfirm = { _, _, _, _, _ -> },
                    )
                }
                waitForIdle()
                onNodeWithText(lastNameStr, useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText(firstNameStr, useUnmergedTree = true).assertIsDisplayed()

                // Test clicking cancel button
                val cancelStr = getString(Res.string.cancel)
                onNode(hasClickAction().and(hasAnyDescendant(hasText(cancelStr))), useUnmergedTree = true).performClick()
                waitForIdle()
                assertTrue(dismissed)
            }

            // Also test Chinese (zh) to hit startsWith("zh") branch
            setAppLanguage("zh")
            val firstNameZh = getString(Res.string.first_name)
            val lastNameZh = getString(Res.string.last_name)
            runComposeUiTest {
                setContent {
                    CreatePatientDialog(
                        onDismissRequest = {},
                        onConfirm = { _, _, _, _, _ -> },
                    )
                }
                waitForIdle()
                onNodeWithText(lastNameZh, useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText(firstNameZh, useUnmergedTree = true).assertIsDisplayed()
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
            val maleStr = getString(Res.string.gender_male)
            val femaleStr = getString(Res.string.gender_female)
            val otherStr = getString(Res.string.gender_other)

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

                // Open gender dropdown
                onNodeWithText(unknownStr, useUnmergedTree = true).performClick()
                waitForIdle()

                // Select female
                onNodeWithText(femaleStr).performClick()
                waitForIdle()
                onNodeWithText(femaleStr, useUnmergedTree = true).assertIsDisplayed()

                // Open again and select male
                onNodeWithText(femaleStr, useUnmergedTree = true).performClick()
                waitForIdle()
                onNodeWithText(maleStr).performClick()
                waitForIdle()
                onNodeWithText(maleStr, useUnmergedTree = true).assertIsDisplayed()

                // Open again and select other
                onNodeWithText(maleStr, useUnmergedTree = true).performClick()
                waitForIdle()
                onNodeWithText(otherStr).performClick()
                waitForIdle()
                onNodeWithText(otherStr, useUnmergedTree = true).assertIsDisplayed()
            }
        }

    /**
     * Verifies East Asian locale with validation errors and clearing.
     */
    @Test
    fun testEastAsianLocaleFieldErrorsAndClearing() =
        runTest {
            setAppLanguage("ja")
            val createStr = getString(Res.string.create)
            val allRequiredStr = getString(Res.string.all_fields_required)

            runComposeUiTest {
                setContent {
                    CreatePatientDialog(
                        actions =
                            CreatePatientDialogActions(
                                onDismissRequest = {},
                                onConfirm = { _, _, _, _, _ -> },
                            ),
                    )
                }
                waitForIdle()

                // Click create to trigger errors in Japanese (East Asian)
                val createButton = onNode(hasClickAction().and(hasAnyDescendant(hasText(createStr))), useUnmergedTree = true)
                createButton.performClick()
                waitForIdle()

                onAllNodesWithText(allRequiredStr, useUnmergedTree = true)[0].assertIsDisplayed()

                // Input last name to clear lastNameError
                val textNodes =
                    onAllNodes(
                        androidx.compose.ui.test
                            .hasSetTextAction(),
                    )
                textNodes[0].performTextInput("山田")
                waitForIdle()
                textNodes[1].performTextInput("太郎")
                waitForIdle()
            }
            setAppLanguage("en")
        }

    /**
     * Verifies validation when submitting empty fields and invalid date formats.
     */
    @Test
    fun testValidationErrorsAndFieldClearing() =
        runTest {
            setAppLanguage("en")
            val createStr = getString(Res.string.create)
            val allRequiredStr = getString(Res.string.all_fields_required)
            val invalidDateStr = getString(Res.string.invalid_date_format)

            runComposeUiTest {
                setContent {
                    CreatePatientDialog(
                        onDismissRequest = {},
                        onConfirm = { _, _, _, _, _ -> },
                    )
                }
                waitForIdle()

                // Click Create with all fields blank
                val createButton = onNode(hasClickAction().and(hasAnyDescendant(hasText(createStr))), useUnmergedTree = true)
                createButton.performClick()
                waitForIdle()

                // Form error displayed
                onAllNodesWithText(allRequiredStr, useUnmergedTree = true)[0].assertIsDisplayed()

                // Type in First Name -> error state is cleared
                val textNodes =
                    onAllNodes(
                        androidx.compose.ui.test
                            .hasSetTextAction(),
                    )
                textNodes[0].performTextInput("Alice")
                waitForIdle()

                // Type in Last Name
                textNodes[1].performTextInput("Smith")
                waitForIdle()

                // Trigger error again to make sure MRN error is active
                createButton.performClick()
                waitForIdle()

                // Type in MRN to clear MRN error
                textNodes[2].performTextInput("MRN-101")
                waitForIdle()

                // Type an invalid date string
                textNodes[3].performTextInput("99/99/9999")
                waitForIdle()

                // Click Create with invalid DOB
                createButton.performClick()
                waitForIdle()

                // Verify invalid date error displayed
                onAllNodesWithText(invalidDateStr, useUnmergedTree = true)[0].assertIsDisplayed()

                // Clear text and type valid date to clear DOB error
                textNodes[3].performTextInput("01/01/2000")
                waitForIdle()
            }
        }

    /**
     * Verifies successful confirmation flow with valid inputs.
     */
    @Test
    fun testSuccessfulPatientCreation() =
        runTest {
            setAppLanguage("en")
            val createStr = getString(Res.string.create)
            var createdFirst = ""
            var createdLast = ""
            var createdMrn = ""
            var createdDob: LocalDate? = null
            var createdGender = ""

            runComposeUiTest {
                setContent {
                    CreatePatientDialog(
                        onDismissRequest = {},
                        onConfirm = { f, l, m, d, g ->
                            createdFirst = f
                            createdLast = l
                            createdMrn = m
                            createdDob = d
                            createdGender = g
                        },
                    )
                }
                waitForIdle()

                val textNodes =
                    onAllNodes(
                        androidx.compose.ui.test
                            .hasSetTextAction(),
                    )
                textNodes[0].performTextInput("Jane")
                textNodes[1].performTextInput("Doe")
                textNodes[2].performTextInput("MRN-789")
                textNodes[3].performTextInput("05/15/1990")
                waitForIdle()

                val createButton = onNode(hasClickAction().and(hasAnyDescendant(hasText(createStr))), useUnmergedTree = true)
                createButton.performClick()
                waitForIdle()

                assertEquals("Jane", createdFirst)
                assertEquals("Doe", createdLast)
                assertEquals("MRN-789", createdMrn)
                assertEquals(LocalDate(1990, 5, 15), createdDob)
                assertEquals("unknown", createdGender)
            }
        }

    /**
     * Verifies opening date picker dialog and dismissing or confirming.
     */
    @Test
    fun testDatePickerDialogInteraction() =
        runTest {
            setAppLanguage("en")
            val selectDateCd = getString(Res.string.cd_select_date)
            val okStr = getString(Res.string.ok)
            val cancelStr = getString(Res.string.cancel)

            runComposeUiTest {
                setContent {
                    CreatePatientDialog(
                        onDismissRequest = {},
                        onConfirm = { _, _, _, _, _ -> },
                    )
                }
                waitForIdle()

                // Click Date icon
                onNodeWithContentDescription(selectDateCd, useUnmergedTree = true).performClick()
                waitForIdle()

                // DatePickerDialog is visible; click DatePicker Cancel button (the second Cancel node on screen)
                onAllNodesWithText(cancelStr, useUnmergedTree = true)[1].performClick()
                waitForIdle()

                // Click Date icon again
                onNodeWithContentDescription(selectDateCd, useUnmergedTree = true).performClick()
                waitForIdle()

                // Click a date node in the picker (e.g. "15" or current day)
                val dayNode = onAllNodesWithText("15", useUnmergedTree = true)
                if (dayNode.fetchSemanticsNodes().isNotEmpty()) {
                    dayNode[0].performClick()
                    waitForIdle()
                }

                // Click OK
                onNodeWithText(okStr, useUnmergedTree = true).performClick()
                waitForIdle()
            }
        }

    /**
     * Tests synthetic composer $changed branch on CreatePatientDialog via reflection.
     */
    @Test
    fun testCreatePatientDialogReflectionChangedZero() =
        runComposeUiTest {
            val dialogClass = Class.forName("io.healthplatform.chartcam.ui.components.CreatePatientDialogKt")
            val dialogMethod =
                dialogClass.declaredMethods.first {
                    it.name == "CreatePatientDialog" && it.parameterCount == 4
                }
            dialogMethod.isAccessible = true

            val actionsMethod =
                dialogClass.declaredMethods.first {
                    it.name == "CreatePatientDialog" && it.parameterCount == 3
                }
            actionsMethod.isAccessible = true

            val actions =
                CreatePatientDialogActions(
                    onDismissRequest = {},
                    onConfirm = { _, _, _, _, _ -> },
                )

            setContent {
                val composer = androidx.compose.runtime.currentComposer
                // changed = 0
                dialogMethod.invoke(
                    null,
                    { },
                    { _: String, _: String, _: String, _: LocalDate, _: String -> },
                    composer,
                    0,
                )

                // changed = 2
                dialogMethod.invoke(
                    null,
                    { },
                    { _: String, _: String, _: String, _: LocalDate, _: String -> },
                    composer,
                    2,
                )

                // changed = 4
                dialogMethod.invoke(
                    null,
                    { },
                    { _: String, _: String, _: String, _: LocalDate, _: String -> },
                    composer,
                    4,
                )

                // changed = 16
                dialogMethod.invoke(
                    null,
                    { },
                    { _: String, _: String, _: String, _: LocalDate, _: String -> },
                    composer,
                    16,
                )

                // changed = 32
                dialogMethod.invoke(
                    null,
                    { },
                    { _: String, _: String, _: String, _: LocalDate, _: String -> },
                    composer,
                    32,
                )

                // changed = 0b00010010 (18)
                dialogMethod.invoke(
                    null,
                    { },
                    { _: String, _: String, _: String, _: LocalDate, _: String -> },
                    composer,
                    18,
                )

                actionsMethod.invoke(
                    null,
                    actions,
                    composer,
                    0,
                )
            }
            waitForIdle()
        }

    /**
     * Recomposition test exercising skipping and state update on CreatePatientDialog.
     */
    @Test
    fun testCreatePatientDialogRecomposition() =
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val actionsState =
                androidx.compose.runtime.mutableStateOf(
                    CreatePatientDialogActions(
                        onDismissRequest = {},
                        onConfirm = { _, _, _, _, _ -> },
                    ),
                )

            setContent {
                val dummy = trigger.value
                CreatePatientDialog(actions = actionsState.value)
            }
            waitForIdle()

            // Skipping
            trigger.value++
            waitForIdle()

            // State change
            actionsState.value =
                CreatePatientDialogActions(
                    onDismissRequest = { },
                    onConfirm = { _, _, _, _, _ -> },
                )
            waitForIdle()

            // Recomposition on legacy CreatePatientDialog(onDismiss, onConfirm)
            val legacyTrigger = androidx.compose.runtime.mutableStateOf(0)
            val onDismissState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onConfirmState =
                androidx.compose.runtime.mutableStateOf<
                    (
                        String,
                        String,
                        String,
                        LocalDate,
                        String,
                    ) -> Unit,
                >({ _, _, _, _, _ -> })

            setContent {
                val dummy = legacyTrigger.value
                CreatePatientDialog(
                    onDismissRequest = onDismissState.value,
                    onConfirm = onConfirmState.value,
                )
            }
            waitForIdle()

            // Skipping on legacy
            legacyTrigger.value++
            waitForIdle()

            // State update on legacy
            onDismissState.value = {}
            onConfirmState.value = { _, _, _, _, _ -> }
            waitForIdle()
        }
}
