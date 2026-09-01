/**
 * @file EncounterDetailViewModelJvmTest.kt
 * Contains declarations for EncounterDetailViewModelJvmTest.kt.
 */
package io.healthplatform.chartcam.viewmodel
import com.google.fhir.model.r4.Canonical
import com.google.fhir.model.r4.Date
import com.google.fhir.model.r4.DateTime
import com.google.fhir.model.r4.Decimal
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.FhirDateTime
import com.google.fhir.model.r4.Integer
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.String
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Helper to avoid Kotlin NPE when using ArgumentCaptor.
 * @param captor the captor
 * @param dummy the dummy value
 * @return the dummy value
 */
private fun <T> capture(
    captor: ArgumentCaptor<T>,
    dummy: T,
): T {
    captor.capture()
    return dummy
}

/**
 * Helper to provide any object.
 * @param dummy the dummy value
 * @return the dummy value
 */
private fun <T> anyObj(dummy: T): T {
    any(dummy!!::class.java)
    return dummy
}

/**
 * Test class for EncounterDetailViewModel on JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EncounterDetailViewModelJvmTest {
    private lateinit var fhirRepository: FhirRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var syncWorker: SyncWorker
    private lateinit var questionnaireRepository: QuestionnaireRepository
    private lateinit var viewModel: EncounterDetailViewModel

    private val dummyEncounter =
        createFhirEncounter(id = "enc1", patientId = "pat1", practitionerId = "prac1", dateStr = "2026-07-09")
    private val dummyDoc =
        io.healthplatform.chartcam.models.createFhirDocumentReference(
            DocumentReferenceCreationParams(
                id = "doc1",
                patientId = "pat1",
                encounterId = "enc1",
                dateStr = "2026-07-09",
                desc = "desc",
                mime = "image/jpeg",
                urlPath = "path",
            ),
        )
    private val dummyQr =
        QuestionnaireResponse
            .Builder(
                Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            ).build()

    /**
     * Set up for testing EncounterDetailViewModel.
     */
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fhirRepository = mock(FhirRepository::class.java)
        authRepository = mock(AuthRepository::class.java)
        syncWorker = mock(SyncWorker::class.java)
        questionnaireRepository = mock(QuestionnaireRepository::class.java)

        val practitioner = Practitioner.Builder().apply { id = "prac1" }.build()
        `when`(authRepository.currentUser).thenReturn(MutableStateFlow(practitioner))

        viewModel =
            EncounterDetailViewModel(
                fhirRepository,
                authRepository,
                questionnaireRepository,
            )
    }

    /**
     * Tear down for testing EncounterDetailViewModel.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test updating answer changes state.
     */
    @Test
    fun testOnAnswerChangedUpdatesStateCorrectly() {
        viewModel.onAnswerChanged("link1", "my answer")
        assertEquals("my answer", viewModel.uiState.value.answers["link1"])

        viewModel.onAnswerChanged("link1", null)
        assertTrue(
            viewModel.uiState.value.answers
                .isEmpty(),
        )
    }

    /**
     * Test updating notes changes state.
     */
    @Test
    fun testOnNotesChanged() {
        viewModel.onNotesChanged("some notes")
        assertEquals("some notes", viewModel.uiState.value.answers["notes"])
    }

    /**
     * Test selecting questionnaire.
     */
    @Test
    fun testSelectQuestionnaire() {
        val q =
            Questionnaire
                .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                .apply {
                    id = "q1"
                }.build()
        viewModel.selectQuestionnaire(q)
        assertEquals(
            "q1",
            viewModel.uiState.value.selectedQuestionnaire
                ?.id,
        )
    }

    /**
     * Test selecting questionnaire by id.
     */
    @Test
    fun testSelectQuestionnaireById() {
        val q =
            Questionnaire
                .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                .apply {
                    id = "q1"
                }.build()
        `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(q))
        viewModel.selectQuestionnaireById("q1")
        assertEquals(
            "q1",
            viewModel.uiState.value.selectedQuestionnaire
                ?.id,
        )
    }

    /**
     * Test creating and selecting questionnaire.
     */
    @Test
    fun testCreateAndSelectQuestionnaire() {
        val q =
            Questionnaire
                .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                .apply {
                    id =
                        "q_new"
                }.build()
        `when`(questionnaireRepository.createQuestionnaire("Test", 2, "Label")).thenReturn(q)
        viewModel.createAndSelectQuestionnaire("Test", 2, "Label")
        assertEquals(
            "q_new",
            viewModel.uiState.value.selectedQuestionnaire
                ?.id,
        )
    }

    /**
     * Test resetting finalized state.
     */
    @Test
    fun testResetFinalized() {
        viewModel.resetFinalized()
        assertFalse(viewModel.uiState.value.isFinalized)
    }

    /**
     * Test deleting encounter.
     */
    @Test
    fun testDeleteEncounter() =
        runTest {
            val encounter =
                createFhirEncounter(
                    id = "enc1",
                    patientId = "pat1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            val patient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(patient)
            `when`(fhirRepository.getEncounter("enc1")).thenReturn(encounter)
            `when`(fhirRepository.getPhotosForEncounter("enc1")).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc1")).thenReturn(emptyList())
            viewModel.initialize("pat1", "enc1", emptyMap())

            var success = false
            viewModel.deleteEncounter { success = true }
            verify(fhirRepository).deleteEncounter("enc1")
            assertTrue(success)
        }

    /**
     * Test adding photos.
     */
    @Test
    fun testAddPhotos() =
        runTest {
            val encounter =
                createFhirEncounter(
                    id = "enc1",
                    patientId = "pat1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            val patient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(patient)
            `when`(fhirRepository.getEncounter("enc1")).thenReturn(encounter)
            `when`(fhirRepository.getPhotosForEncounter("enc1")).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc1")).thenReturn(emptyList())
            viewModel.initialize("pat1", "enc1", emptyMap())

            viewModel.addPhotos(mapOf("Step1" to "path1"))

            val docCaptor = ArgumentCaptor.forClass(DocumentReference::class.java)
            verify(fhirRepository, times(1)).saveDocumentReference(capture(docCaptor, dummyDoc))
            assertEquals("Step1", docCaptor.value.description?.value)
            assertEquals(1, viewModel.uiState.value.photos.size)
        }

    /**
     * Test initializing new encounter.
     */
    @Test
    fun testInitializeNewEncounter() =
        runTest {
            val patient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(patient)

            viewModel.initialize("pat1", "new", mapOf("Step1" to "path1"))

            verify(fhirRepository).saveEncounter(anyObj(dummyEncounter))
            verify(fhirRepository).saveDocumentReference(anyObj(dummyDoc))

            assertNotNull(viewModel.uiState.value.encounter)
            assertEquals(
                "pat1",
                viewModel.uiState.value.patient
                    ?.id,
            )
        }

    /**
     * Test finalize encounter builds response.
     */
    @Test
    fun testFinalizeEncounterBuildsProperQuestionnaireResponse() =
        runTest {
            val encounter =
                createFhirEncounter(
                    id = "enc1",
                    patientId = "pat1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            val patient = Patient.Builder().apply { id = "pat1" }.build()
            val nestedItem =
                Questionnaire.Item.Builder(
                    String.Builder().apply {
                        value = "link_nested"
                    },
                    Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                )
            val groupItem =
                Questionnaire.Item
                    .Builder(
                        String.Builder().apply {
                            value = "group1"
                        },
                        Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                    ).apply { item.add(nestedItem) }

            val q =
                Questionnaire
                    .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "q1"
                        item.add(
                            Questionnaire.Item.Builder(
                                String.Builder().apply {
                                    value = "link_str"
                                },
                                Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ),
                        )
                        item.add(
                            Questionnaire.Item.Builder(
                                String.Builder().apply {
                                    value = "link_bool"
                                },
                                Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                            ),
                        )
                        item.add(
                            Questionnaire.Item.Builder(
                                String.Builder().apply {
                                    value = "link_float_dec"
                                },
                                Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                            ),
                        )
                        item.add(
                            Questionnaire.Item.Builder(
                                String.Builder().apply {
                                    value = "link_float_int"
                                },
                                Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ),
                        )
                        item.add(
                            Questionnaire.Item.Builder(
                                String.Builder().apply {
                                    value = "link_date"
                                },
                                Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                            ),
                        )
                        item.add(
                            Questionnaire.Item.Builder(
                                String.Builder().apply {
                                    value = "link_datetime"
                                },
                                Enumeration(value = Questionnaire.QuestionnaireItemType.DateTime),
                            ),
                        )
                        item.add(
                            Questionnaire.Item.Builder(
                                String.Builder().apply {
                                    value = "link_list"
                                },
                                Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            ),
                        )
                        item.add(groupItem)
                    }.build()

            `when`(fhirRepository.getPatient("pat1")).thenReturn(patient)
            `when`(fhirRepository.getEncounter("enc1")).thenReturn(encounter)
            `when`(fhirRepository.getPhotosForEncounter("enc1")).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc1")).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc1")).thenReturn(emptyList())
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(q))

            viewModel.initialize("pat1", "enc1", emptyMap())

            viewModel.onAnswerChanged("link_str", "test string")
            viewModel.onAnswerChanged("link_bool", true)
            viewModel.onAnswerChanged("link_float_dec", 5.5f)
            viewModel.onAnswerChanged("link_float_int", 10.0f)
            viewModel.onAnswerChanged("link_date", "2026-07-09")
            viewModel.onAnswerChanged("link_datetime", "2026-07-09T10:00:00Z")
            viewModel.onAnswerChanged("link_list", listOf("opt1", "opt2"))
            viewModel.onAnswerChanged("link_nested", "nested ans")

            viewModel.finalizeEncounter()

            val qrCaptor = ArgumentCaptor.forClass(QuestionnaireResponse::class.java)
            verify(fhirRepository).saveQuestionnaireResponse(capture(qrCaptor, dummyQr))

            val qr = qrCaptor.value
            assertEquals("q1", qr.questionnaire?.value)
            assertTrue(qr.item.any { it.linkId.value == "link_str" })
            assertTrue(qr.item.any { it.linkId.value == "link_float_dec" })
            assertTrue(qr.item.any { it.linkId.value == "link_list" })

            val gItem = qr.item.find { it.linkId.value == "group1" }
            assertNotNull(gItem)
            assertTrue(gItem.item.any { it.linkId.value == "link_nested" })
        }

    /**
     * Test loading existing encounter with existing responses.
     */
    @Test
    fun testLoadingExistingEncounterWithExistingResponses() =
        runTest {
            val encounter =
                createFhirEncounter(
                    id = "enc1",
                    patientId = "pat1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            val patient = Patient.Builder().apply { id = "pat1" }.build()

            val itemStr =
                QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "link_str" }).apply {
                    answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value
                                    .String(String.Builder().apply { value = "loaded str" }.build())
                        },
                    )
                }

            val itemBool =
                QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "link_bool" }).apply {
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
                }

            val itemDecimal =
                QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "link_decimal" }).apply {
                    answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value.Decimal(
                                    Decimal
                                        .Builder()
                                        .apply {
                                            value =
                                                com.ionspin.kotlin.bignum.decimal.BigDecimal
                                                    .parseString("3.14")
                                        }.build(),
                                )
                        },
                    )
                }

            val itemInteger =
                QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "link_integer" }).apply {
                    answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value
                                    .Integer(Integer.Builder().apply { value = 12 }.build())
                        },
                    )
                }

            val itemDate =
                QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "link_date" }).apply {
                    answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value
                                    .Date(Date.Builder().apply { value = FhirDate.fromString("2026-07-09") }.build())
                        },
                    )
                }

            val itemDateTime =
                QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "link_datetime" }).apply {
                    answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value.DateTime(
                                    DateTime
                                        .Builder()
                                        .apply {
                                            value =
                                                FhirDateTime.fromString("2026-07-09T10:00:00Z")
                                        }.build(),
                                )
                        },
                    )
                }

            val itemNested =
                QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "group1" }).apply {
                    item.add(
                        QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "link_nested" }).apply {
                            answer.add(
                                QuestionnaireResponse.Item.Answer.Builder().apply {
                                    value =
                                        QuestionnaireResponse.Item.Answer.Value
                                            .String(String.Builder().apply { value = "nested str" }.build())
                                },
                            )
                        },
                    )
                }

            val qr =
                QuestionnaireResponse
                    .Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                    .apply {
                        questionnaire = Canonical.Builder().apply { value = "Questionnaire/q1" }
                        item.add(itemStr)
                        item.add(itemBool)
                        item.add(itemDecimal)
                        item.add(itemInteger)
                        item.add(itemDate)
                        item.add(itemDateTime)
                        item.add(itemNested)
                    }.build()

            val dummyQ =
                Questionnaire
                    .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "q1"
                        title = String.Builder().apply { value = "Found Questionnaire" }
                    }.build()

            `when`(fhirRepository.getPatient("pat1")).thenReturn(patient)
            `when`(fhirRepository.getEncounter("enc1")).thenReturn(encounter)
            `when`(fhirRepository.getPhotosForEncounter("enc1")).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc1")).thenReturn(listOf(qr))
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(dummyQ))

            viewModel.initialize("pat1", "enc1", emptyMap())

            assertEquals("loaded str", viewModel.uiState.value.answers["link_str"])
            assertEquals(true, viewModel.uiState.value.answers["link_bool"])
            assertEquals(3.14f, viewModel.uiState.value.answers["link_decimal"])
            assertEquals(12.0f, viewModel.uiState.value.answers["link_integer"])
            assertEquals("2026-07-09", viewModel.uiState.value.answers["link_date"])
            assertEquals("2026-07-09T10:00:00Z", viewModel.uiState.value.answers["link_datetime"])
            assertEquals("nested str", viewModel.uiState.value.answers["link_nested"])
            assertEquals(
                "q1",
                viewModel.uiState.value.selectedQuestionnaire
                    ?.id,
            )
        }

    /**
     * Test form update callback.
     */
    @Test
    fun testOnFormUpdated() {
        val newMap = mapOf("q1" to "a1")
        viewModel.onFormUpdated(newMap)
        assertEquals("a1", viewModel.uiState.value.answers["q1"])
    }

    /**
     * Test dummy item building coverage.
     */
    @Test
    fun testBuildDummyItemsRecursivelyCoverage() =
        runTest {
            // Trigger initialization with a QR that has no backing questionnaire
            val encId = "enc_dummy"
            val patId = "pat_dummy"
            val patient = Patient.Builder().apply { id = patId }.build()
            val encounter =
                io.healthplatform.chartcam.models.createFhirEncounter(
                    id = encId,
                    patientId = patId,
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            val qr =
                QuestionnaireResponse
                    .Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                    .apply {
                        questionnaire = Canonical.Builder().apply { value = "non_existent_q" }
                        val dummyItemStr =
                            QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "dummy_str" }).apply {
                                answer.add(
                                    QuestionnaireResponse.Item.Answer.Builder().apply {
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value
                                                .String(String.Builder().apply { value = "test" }.build())
                                    },
                                )
                            }
                        item.add(dummyItemStr)
                    }.build()

            `when`(fhirRepository.getPatient(patId)).thenReturn(patient)
            `when`(fhirRepository.getEncounter(encId)).thenReturn(encounter)
            `when`(fhirRepository.getPhotosForEncounter(encId)).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter(encId)).thenReturn(listOf(qr))
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(emptyList())

            viewModel.initialize(patId, encId, emptyMap())
            advanceUntilIdle()

            assertEquals("test", viewModel.uiState.value.answers["dummy_str"])
            // If the dummy items were built successfully, a fallback questionnaire will be present in state
            assertNotNull(viewModel.uiState.value.selectedQuestionnaire)
            assertEquals(
                "Recovered Form",
                viewModel.uiState.value.selectedQuestionnaire
                    ?.title
                    ?.value,
            )
        }

    /**
     * Test recursively extracting answers list.
     */
    @Test
    fun testExtractAnswersRecursivelyList() =
        runTest {
            val encounter =
                createFhirEncounter(
                    id = "enc1",
                    patientId = "pat1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            val patient = Patient.Builder().apply { id = "pat1" }.build()

            val itemMultiStr =
                QuestionnaireResponse.Item.Builder(String.Builder().apply { value = "link_multi" }).apply {
                    answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value
                                    .String(String.Builder().apply { value = "val1" }.build())
                        },
                    )
                    answer.add(
                        QuestionnaireResponse.Item.Answer.Builder().apply {
                            value =
                                QuestionnaireResponse.Item.Answer.Value
                                    .String(String.Builder().apply { value = "val2" }.build())
                        },
                    )
                }

            val qr =
                QuestionnaireResponse
                    .Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                    .apply {
                        questionnaire = Canonical.Builder().apply { value = "Questionnaire/q1" }
                        item.add(itemMultiStr)
                    }.build()

            val dummyQ =
                Questionnaire
                    .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "q1"
                    }.build()

            `when`(fhirRepository.getPatient("pat1")).thenReturn(patient)
            `when`(fhirRepository.getEncounter("enc1")).thenReturn(encounter)
            `when`(fhirRepository.getPhotosForEncounter("enc1")).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc1")).thenReturn(listOf(qr))
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(dummyQ))

            viewModel.initialize("pat1", "enc1", emptyMap())
            advanceUntilIdle()

            val answers = viewModel.uiState.value.answers["link_multi"] as List<*>
            assertEquals(listOf("val1", "val2"), answers)
        }

    /**
     * Test reopening encounter.
     */
    @Test
    fun testReopenEncounter() =
        runTest {
            val encId = "enc-1"
            val dummyEncounter =
                io.healthplatform.chartcam.models.createFhirEncounter(
                    id = encId,
                    patientId = "patient-1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )

            val dummyPatient = Patient.Builder().apply { id = "patient-1" }.build()
            `when`(fhirRepository.getPatient("patient-1")).thenReturn(dummyPatient)
            `when`(fhirRepository.getEncounter(encId)).thenReturn(dummyEncounter)

            val qId = "q-1"
            val dummyQ =
                Questionnaire
                    .Builder(Enumeration(value = com.google.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = qId
                        title =
                            com.google.fhir.model.r4.String
                                .Builder()
                                .apply { value = "Test Q" }
                    }.build()

            val dummyQr =
                QuestionnaireResponse
                    .Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                    .apply {
                        questionnaire = Canonical.Builder().apply { value = qId }
                    }.build()
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter(encId)).thenReturn(listOf(dummyQr))
            `when`(fhirRepository.getPhotosForEncounter(encId)).thenReturn(emptyList())

            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(dummyQ))

            val viewModel = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            viewModel.initialize("patient-1", encId, emptyMap())

            advanceUntilIdle()

            println("STATE BEFORE REOPEN: ${viewModel.uiState.value.encounter?.id}")

            // Provide a mock updated encounter that reflects the changed status for the second getEncounter call
            val reopenedEncounter =
                io.healthplatform.chartcam.models.createFhirEncounter(
                    id = encId,
                    patientId = "patient-1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            `when`(fhirRepository.getEncounter(encId)).thenReturn(reopenedEncounter)

            // Now call reopenEncounter
            viewModel.reopenEncounter()

            advanceUntilIdle()

            // Verify update was called
            verify(fhirRepository).updateEncounterStatus(encId, "in-progress", "")

            val state = viewModel.uiState.value
            assertEquals(com.google.fhir.model.r4.Encounter.EncounterStatus.In_Progress, state.encounter?.status?.value)
            assertEquals(false, state.isFinalized)
        }

    /**
     * Test adding photos with nulls.
     */
    @Test
    fun testAddPhotosWithNulls() {
        val photosMap = mapOf("Step1" to "path1")
        // Enoucnter is null initially
        viewModel.addPhotos(photosMap)
        // Shouldn't crash, should just return
        assertEquals(0, viewModel.uiState.value.photos.size)
    }
}
