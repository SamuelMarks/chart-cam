/**
 * @file FacialSeriesBuilderTest.kt
 * Contains tests for QuestionnaireBuilderViewModel with FACIAL_PROFILE_SERIES widget.
 */
package io.healthplatform.chartcam.viewmodel

import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.camera.SilhouetteType
import io.healthplatform.chartcam.fhir.getItemControl
import io.healthplatform.chartcam.fhir.getSilhouetteType
import io.healthplatform.chartcam.fhir.isFacialProfileSeries
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests verifying the Questionnaire Builder properly creates, configures, and maps FACIAL_PROFILE_SERIES widgets.
 */
class FacialSeriesBuilderTest {
    @Test
    fun testFacialProfileSeriesBuilderSerialization() =
        runTest {
            val repository = QuestionnaireRepository()
            val viewModel = QuestionnaireBuilderViewModel(repository)

            viewModel.updateTitle("Facial Series Test")
            viewModel.addItem(WidgetType.FACIAL_PROFILE_SERIES)

            val items = viewModel.state.value.items
            assertEquals(1, items.size)
            val builderItem = items.first()
            assertEquals(WidgetType.FACIAL_PROFILE_SERIES, builderItem.widgetType)

            val validation = viewModel.validate()
            assertTrue(validation.isSuccess)
            val questionnaire = validation.getOrThrow()

            assertEquals(1, questionnaire.item.size)
            val groupItem = questionnaire.item.first()
            assertEquals(Questionnaire.QuestionnaireItemType.Group, groupItem.type.value)
            assertEquals("facial-profile-series", groupItem.getItemControl())
            assertTrue(groupItem.isFacialProfileSeries())

            // Verify 3 standardized child items
            assertEquals(3, groupItem.item.size)

            val leftItem = groupItem.item[0]
            assertEquals(Questionnaire.QuestionnaireItemType.Attachment, leftItem.type.value)
            assertEquals(SilhouetteType.PROFILE_CORNEA_NOSE_LEFT, leftItem.getSilhouetteType().getOrNull())
            assertTrue(leftItem.code.any { it.code?.value == "272480006" })

            val frontItem = groupItem.item[1]
            assertEquals(Questionnaire.QuestionnaireItemType.Attachment, frontItem.type.value)
            assertEquals(SilhouetteType.FRONTAL_FACE, frontItem.getSilhouetteType().getOrNull())
            assertTrue(frontItem.code.any { it.code?.value == "272483008" })

            val rightItem = groupItem.item[2]
            assertEquals(Questionnaire.QuestionnaireItemType.Attachment, rightItem.type.value)
            assertEquals(SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT, rightItem.getSilhouetteType().getOrNull())
            assertTrue(rightItem.code.any { it.code?.value == "272481005" })
        }

    @Test
    fun testFacialProfileSeriesDeserialization() =
        runTest {
            val repository = QuestionnaireRepository()
            val viewModel = QuestionnaireBuilderViewModel(repository)

            viewModel.updateTitle("Roundtrip Test")
            viewModel.addItem(WidgetType.FACIAL_PROFILE_SERIES)
            val qId = viewModel.saveQuestionnaire()
            assertNotNull(qId)

            val reloaded = repository.getQuestionnaire(qId)
            assertNotNull(reloaded)

            // Re-open in builder via duplicateFromId
            val secondViewModel = QuestionnaireBuilderViewModel(repository, duplicateFromId = qId)
            val items = secondViewModel.state.value.items
            assertEquals(1, items.size)
            assertEquals(WidgetType.FACIAL_PROFILE_SERIES, items.first().widgetType)
        }
}
