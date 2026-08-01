package io.healthplatform.chartcam.viewmodel

import com.google.fhir.model.r4.Attachment
import com.google.fhir.model.r4.Canonical
import com.google.fhir.model.r4.Date
import com.google.fhir.model.r4.DateTime
import com.google.fhir.model.r4.Decimal
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.FhirDateTime
import com.google.fhir.model.r4.Integer
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.mockito.Mockito

class EncounterDetailCoverageTest {
    @Test
    fun testNullsAndMissingForms() {
        val fhirRepository = Mockito.mock(FhirRepository::class.java)
        val authRepository = Mockito.mock(AuthRepository::class.java)
        val qrRepo = Mockito.mock(QuestionnaireRepository::class.java)
        val syncWorker = Mockito.mock(SyncWorker::class.java)

        val pat = Patient.Builder().apply { id = "pat1" }.build()
        val enc =
            io.healthplatform.chartcam.models
                .createFhirEncounter("enc1", "pat1", "prac1", "2026-07-09")

        val qr =
            QuestionnaireResponse
                .Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                .apply {
                    questionnaire = Canonical.Builder().apply { value = "non-existent-q" }

                    // Add items of different types to hit buildDummyItemsRecursively branches
                    item.add(
                        QuestionnaireResponse.Item
                            .Builder(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "str" },
                            ).apply {
                                answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.String(
                                                com.google.fhir.model.r4.String
                                                    .Builder()
                                                    .apply { value = "str" }
                                                    .build(),
                                            )
                                    },
                                )
                            },
                    )
                    item.add(
                        QuestionnaireResponse.Item
                            .Builder(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "bool" },
                            ).apply {
                                answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Boolean(
                                                com.google.fhir.model.r4.Boolean
                                                    .Builder()
                                                    .apply { value = true }
                                                    .build(),
                                            )
                                    },
                                )
                            },
                    )
                    item.add(
                        QuestionnaireResponse.Item
                            .Builder(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "dec" },
                            ).apply {
                                answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.Decimal(
                                                Decimal
                                                    .Builder()
                                                    .apply {
                                                        value =
                                                            com.ionspin.kotlin.bignum.decimal.BigDecimal
                                                                .parseString("1.0")
                                                    }.build(),
                                            )
                                    },
                                )
                            },
                    )
                    item.add(
                        QuestionnaireResponse.Item
                            .Builder(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "int" },
                            ).apply {
                                answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .Integer(Integer.Builder().apply { value = 1 }.build())
                                    },
                                )
                            },
                    )
                    item.add(
                        QuestionnaireResponse.Item
                            .Builder(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "date" },
                            ).apply {
                                answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .Date(Date.Builder().apply { value = FhirDate.fromString("2026-07-09") }.build())
                                    },
                                )
                            },
                    )
                    item.add(
                        QuestionnaireResponse.Item
                            .Builder(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "dt" },
                            ).apply {
                                answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.DateTime(
                                                DateTime
                                                    .Builder()
                                                    .apply {
                                                        value =
                                                            FhirDateTime.fromString("2026-07-09T00:00:00Z")
                                                    }.build(),
                                            )
                                    },
                                )
                            },
                    )
                    item.add(
                        QuestionnaireResponse.Item
                            .Builder(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "att" },
                            ).apply {
                                answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .Attachment(Attachment.Builder().build())
                                    },
                                )
                            },
                    )
                    // Add a group (nested item)
                    item.add(
                        QuestionnaireResponse.Item
                            .Builder(
                                com.google.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "grp" },
                            ).apply {
                                item.add(
                                    QuestionnaireResponse.Item
                                        .Builder(
                                            com.google.fhir.model.r4.String
                                                .Builder()
                                                .apply { value = "nested" },
                                        ).apply {
                                            answer.add(
                                                QuestionnaireResponse.Item.Answer.Builder().apply {
                                                    value =
                                                        QuestionnaireResponse.Item.Answer.Value.String(
                                                            com.google.fhir.model.r4.String
                                                                .Builder()
                                                                .apply { value = "str" }
                                                                .build(),
                                                        )
                                                },
                                            )
                                        },
                                )
                            },
                    )
                }.build()

        kotlinx.coroutines.runBlocking {
            Mockito.`when`(fhirRepository.getPatient("pat1")).thenReturn(pat)
            Mockito.`when`(fhirRepository.getEncounter("enc1")).thenReturn(enc)
            Mockito.`when`(fhirRepository.getPhotosForEncounter("enc1")).thenReturn(emptyList())
            Mockito.`when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc1")).thenReturn(listOf(qr))
            Mockito.`when`(qrRepo.getAvailableQuestionnaires()).thenReturn(emptyList())
        }

        val prac = Practitioner.Builder().apply { id = "prac1" }.build()
        Mockito.`when`(authRepository.currentUser).thenReturn(MutableStateFlow(prac))

        val vm = EncounterDetailViewModel(fhirRepository, authRepository, syncWorker, qrRepo)
        vm.initialize("pat1", "enc1", mapOf("Step1" to "path1"))

        kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(200) }
    }

    @Test
    fun testOnFormUpdated() {
        val fhirRepository = Mockito.mock(FhirRepository::class.java)
        val authRepository = Mockito.mock(AuthRepository::class.java)
        val qrRepo = Mockito.mock(QuestionnaireRepository::class.java)
        val syncWorker = Mockito.mock(SyncWorker::class.java)

        val vm = EncounterDetailViewModel(fhirRepository, authRepository, syncWorker, qrRepo)
        vm.onFormUpdated(mapOf("test" to "test"))
    }

    @Test
    fun testSelectQuestionnaireByIdAndCreate() {
        val fhirRepository = Mockito.mock(FhirRepository::class.java)
        val authRepository = Mockito.mock(AuthRepository::class.java)
        val qrRepo = Mockito.mock(QuestionnaireRepository::class.java)
        val syncWorker = Mockito.mock(SyncWorker::class.java)

        val q =
            Questionnaire
                .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                .apply {
                    id = "q-fresh"
                }.build()
        Mockito.`when`(qrRepo.getAvailableQuestionnaires()).thenReturn(listOf(q))
        Mockito.`when`(qrRepo.createQuestionnaire("Title", 1, "label")).thenReturn(q)

        val vm = EncounterDetailViewModel(fhirRepository, authRepository, syncWorker, qrRepo)

        vm.selectQuestionnaireById("q-fresh")
        vm.createAndSelectQuestionnaire("Title", 1, "label")
    }
}
