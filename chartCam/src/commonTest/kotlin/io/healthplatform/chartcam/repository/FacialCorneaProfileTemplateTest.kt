/**
 * @file FacialCorneaProfileTemplateTest.kt
 * Contains tests for the facial-cornea-profile questionnaire template and its localization.
 */
package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.camera.SilhouetteType
import io.healthplatform.chartcam.fhir.getSilhouetteType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests verifying the default template facial-cornea-profile loads and localizes properly.
 */
class FacialCorneaProfileTemplateTest {
    @Test
    fun testFacialCorneaProfileTemplateLoadsAndContainsRequiredQuestions() =
        runTest {
            val repository = QuestionnaireRepository()
            repository.loadDefaultForms()

            val form = repository.getQuestionnaire("facial-cornea-profile")
            assertNotNull(form, "facial-cornea-profile should be loaded as default template")
            assertEquals("Facial Profile & Cornea Examination", form.title?.value)

            // Must contain 4 items: clinical_notes + 3 guided photos
            assertEquals(4, form.item.size)

            val notesItem = form.item.find { it.linkId.value == "clinical_notes" }
            assertNotNull(notesItem)
            assertEquals("Clinical Observations & Notes", notesItem.text?.value)

            val leftItem = form.item.find { it.linkId.value == "profile_left" }
            assertNotNull(leftItem)
            assertEquals(Questionnaire.QuestionnaireItemType.Attachment, leftItem.type.value)
            assertEquals(SilhouetteType.PROFILE_CORNEA_NOSE_LEFT, leftItem.getSilhouetteType().getOrNull())

            val frontItem = form.item.find { it.linkId.value == "front_view" }
            assertNotNull(frontItem)
            assertEquals(Questionnaire.QuestionnaireItemType.Attachment, frontItem.type.value)
            assertEquals(SilhouetteType.FRONTAL_FACE, frontItem.getSilhouetteType().getOrNull())

            val rightItem = form.item.find { it.linkId.value == "profile_right" }
            assertNotNull(rightItem)
            assertEquals(Questionnaire.QuestionnaireItemType.Attachment, rightItem.type.value)
            assertEquals(SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT, rightItem.getSilhouetteType().getOrNull())
        }

    @Test
    fun testFacialCorneaProfileLocalization() =
        runTest {
            val repository = QuestionnaireRepository()
            repository.loadDefaultForms()

            // Spanish
            val esForm = repository.getQuestionnaire("facial-cornea-profile", language = "es")
            assertNotNull(esForm)
            assertEquals("Examen de perfil facial y córnea", esForm.title?.value)
            assertEquals(
                "Observaciones clínicas y notas",
                esForm.item
                    .find { it.linkId.value == "clinical_notes" }
                    ?.text
                    ?.value,
            )
            assertEquals(
                "Perfil izquierdo (córnea y nariz)",
                esForm.item
                    .find { it.linkId.value == "profile_left" }
                    ?.text
                    ?.value,
            )
            assertEquals(
                "Vista frontal",
                esForm.item
                    .find { it.linkId.value == "front_view" }
                    ?.text
                    ?.value,
            )
            assertEquals(
                "Perfil derecho (córnea y nariz)",
                esForm.item
                    .find { it.linkId.value == "profile_right" }
                    ?.text
                    ?.value,
            )

            // Japanese
            val jaForm = repository.getQuestionnaire("facial-cornea-profile", language = "ja")
            assertNotNull(jaForm)
            assertEquals("顔貌側面および角膜検査", jaForm.title?.value)

            // Hebrew
            val heForm = repository.getQuestionnaire("facial-cornea-profile", language = "he")
            assertNotNull(heForm)
            assertEquals("בדיקת פרופיל פנים וקרנית", heForm.title?.value)

            // Traditional Chinese
            val zhForm = repository.getQuestionnaire("facial-cornea-profile", language = "zh")
            assertNotNull(zhForm)
            assertEquals("面部側臉與角膜檢查", zhForm.title?.value)
        }

    @Test
    fun testFacialCorneaProfileUnknownItemTranslations() =
        runTest {
            val repository = QuestionnaireRepository()
            repository.loadDefaultForms()
            val customQ =
                Questionnaire
                    .Builder(
                        status =
                            dev.ohs.fhir.model.r4.Enumeration(
                                value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active,
                            ),
                    ).apply {
                        this.id = "facial-cornea-profile"
                        this.item.add(
                            Questionnaire.Item.Builder(
                                linkId =
                                    dev.ohs.fhir.model.r4
                                        .String(value = "unknown_item")
                                        .toBuilder(),
                                type =
                                    dev.ohs.fhir.model.r4.Enumeration(
                                        value = Questionnaire.QuestionnaireItemType.String,
                                    ),
                            ),
                        )
                    }.build()
            val localized = repository.localizeQuestionnaire(customQ, "es")
            assertEquals(1, localized.item.size)
        }
}
