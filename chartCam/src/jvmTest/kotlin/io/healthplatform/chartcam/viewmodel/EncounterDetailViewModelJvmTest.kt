/**
 * @file EncounterDetailViewModelJvmTest.kt
 * Contains declarations for EncounterDetailViewModelJvmTest.kt.
 */
package io.healthplatform.chartcam.viewmodel
import dev.ohs.fhir.model.r4.Canonical
import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.DateTime
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.Integer
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.String
import io.healthplatform.chartcam.models.DocumentReferenceCreationParams
import io.healthplatform.chartcam.models.createFhirEncounter
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
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
        questionnaireRepository = mock(QuestionnaireRepository::class.java)

        val practitioner = Practitioner.Builder().apply { id = "prac1" }.build()
        `when`(authRepository.currentUser).thenReturn(MutableStateFlow(practitioner))
        `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(emptyList())
        kotlinx.coroutines.runBlocking {
            `when`(fhirRepository.getPhotosForEncounter(anyObj("id"))).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter(anyObj("id"))).thenReturn(emptyList())
        }

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
                .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
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
                .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
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
                .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
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
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
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
     * Verifies that finalizeEncounter uses explicit localized strings for boolean answers and notes.
     */
    @Test
    fun testFinalizeEncounterWithLocalizedStrings() =
        runTest {
            val encounter =
                createFhirEncounter(
                    id = "enc_loc",
                    patientId = "pat1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            val patient = Patient.Builder().apply { id = "pat1" }.build()
            val itemBool =
                Questionnaire.Item
                    .Builder(
                        String.Builder().apply { value = "fever" },
                        Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                    ).apply {
                        text = String.Builder().apply { value = "Fiebre" }
                    }
            val q =
                Questionnaire
                    .Builder(
                        Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                    ).apply {
                        id = "q_loc"
                        item.add(itemBool)
                    }.build()

            `when`(fhirRepository.getPatient("pat1")).thenReturn(patient)
            `when`(fhirRepository.getEncounter("enc_loc")).thenReturn(encounter)
            `when`(fhirRepository.getPhotosForEncounter("enc_loc")).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc_loc")).thenReturn(emptyList())
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(q))

            viewModel.initialize("pat1", "enc_loc", emptyMap())
            viewModel.onAnswerChanged("fever", true)

            viewModel.finalizeEncounter(yesStr = "Sí", noStr = "No", noNotesStr = "Sin notas")
            advanceUntilIdle()

            verify(fhirRepository).updateEncounterStatus("enc_loc", "finished", "Fiebre: Sí.")
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
                                    dev.ohs.fhir.model.r4.Boolean
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
                                                dev.ohs.fhir.model.r4.FhirDecimal
                                                    .fromString("3.14")
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
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
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
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
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
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = qId
                        title =
                            dev.ohs.fhir.model.r4.String
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
            assertEquals(dev.ohs.fhir.model.r4.Encounter.EncounterStatus.In_Progress, state.encounter?.status?.value)
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

    /**
     * Test initialize edge cases when patient, practitioner, or encounter is null.
     */
    @Test
    fun testInitializeNullScenarios() =
        runTest {
            // Patient null
            `when`(fhirRepository.getPatient("missing_pat")).thenReturn(null)
            val vm1 = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vm1.initialize("missing_pat", "new", emptyMap())
            advanceUntilIdle()
            assertFalse(vm1.uiState.value.isLoading)

            // Practitioner null
            val dummyPatient = Patient.Builder().apply { id = "p1" }.build()
            `when`(fhirRepository.getPatient("p1")).thenReturn(dummyPatient)
            val nullAuthRepo = mock(AuthRepository::class.java)
            `when`(nullAuthRepo.currentUser).thenReturn(MutableStateFlow<Practitioner?>(null))
            val vm2 = EncounterDetailViewModel(fhirRepository, nullAuthRepo, questionnaireRepository)
            vm2.initialize("p1", "new", emptyMap())
            advanceUntilIdle()
            assertFalse(vm2.uiState.value.isLoading)

            // Existing encounter null
            `when`(fhirRepository.getEncounter("missing_enc")).thenReturn(null)
            val vm3 = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vm3.initialize("p1", "missing_enc", emptyMap())
            advanceUntilIdle()
            assertFalse(vm3.uiState.value.isLoading)
            assertEquals(
                "p1",
                vm3.uiState.value.patient
                    ?.id,
            )
            kotlin.test.assertNull(vm3.uiState.value.encounter)

            // Early returns when encounter is null but patient is non-null
            vm3.addPhotos(mapOf("step" to "path"))
            vm3.addVoiceMemo("audio_path")
            assertEquals(0, vm3.uiState.value.photos.size)

            // Multiple initialize calls abort early
            vm3.initialize("p1", "missing_enc", emptyMap())
            advanceUntilIdle()
        }

    /**
     * Test existing encounter initialized with photosMap.
     */
    @Test
    fun testExistingEncounterWithPhotos() =
        runTest {
            val encId = "enc-photos"
            val dummyEncounter =
                createFhirEncounter(
                    id = encId,
                    patientId = "pat1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            val dummyPatient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(dummyPatient)
            `when`(fhirRepository.getEncounter(encId)).thenReturn(dummyEncounter)

            val qItem =
                Questionnaire.Item
                    .Builder(
                        dev.ohs.fhir.model.r4.String
                            .Builder()
                            .apply { value = "step1" },
                        Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply {
                        text =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "Step 1 Label" }
                    }
            val dummyQFallback =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "q-fallback"
                        item.add(qItem)
                    }.build()

            val qItemActive =
                Questionnaire.Item
                    .Builder(
                        dev.ohs.fhir.model.r4.String
                            .Builder()
                            .apply { value = "step_active" },
                        Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                    ).apply {
                        text =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "Active Step Label" }
                    }

            val dummyQActive =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "q-photos"
                        item.add(qItemActive)
                    }.build()

            val dummyQr =
                QuestionnaireResponse
                    .Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                    .apply {
                        questionnaire = Canonical.Builder().apply { value = "q-photos" }
                    }.build()

            `when`(fhirRepository.getQuestionnaireResponsesForEncounter(encId)).thenReturn(listOf(dummyQr))
            `when`(fhirRepository.getPhotosForEncounter(encId)).thenReturn(emptyList())
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(dummyQFallback, dummyQActive))

            val vm = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vm.initialize("pat1", encId, mapOf("step_active" to "path/0.jpg", "step1" to "path/1.jpg", "unmatched_step" to "path/2.jpg"))
            advanceUntilIdle()

            assertEquals(3, vm.uiState.value.photos.size)
        }

    /**
     * Test adding voice memo when encounter is null and when initialized.
     */
    @Test
    fun testAddVoiceMemo() =
        runTest {
            val vm = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            // Abort when encounter is null
            vm.addVoiceMemo("audio/path.mp4")
            assertEquals(0, vm.uiState.value.photos.size)

            // When encounter is initialized
            val dummyPatient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(dummyPatient)
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(emptyList())
            vm.initialize("pat1", "new", emptyMap())
            advanceUntilIdle()

            vm.addVoiceMemo("audio/memo.mp4", "Custom Memo")
            advanceUntilIdle()

            assertEquals(1, vm.uiState.value.photos.size)
            assertEquals(
                "audio/mp4",
                vm.uiState.value.photos[0]
                    .content[0]
                    .attachment.contentType
                    ?.value,
            )
        }

    /**
     * Test selecting questionnaire from fresh repository list and non-existent questionnaire.
     */
    @Test
    fun testSelectQuestionnaireFreshListAndNonExistent() =
        runTest {
            val dummyPatient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(dummyPatient)
            val q1 =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id =
                            "q1"
                    }.build()
            val qFresh =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id =
                            "qFresh"
                    }.build()

            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(q1))

            val vm = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vm.initialize("pat1", "new", emptyMap())
            advanceUntilIdle()

            // When questionnaire is not in state but in fresh list
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(q1, qFresh))
            vm.selectQuestionnaireById("qFresh")
            assertEquals(
                "qFresh",
                vm.uiState.value.selectedQuestionnaire
                    ?.id,
            )

            // When questionnaire is in neither
            vm.selectQuestionnaireById("qUnknown")
            assertEquals(
                "qFresh",
                vm.uiState.value.selectedQuestionnaire
                    ?.id,
            )

            // Test createAndSelectQuestionnaire with default labels parameter
            `when`(questionnaireRepository.createQuestionnaire(anyObj("New Title"), anyObj(2), anyObj(""))).thenReturn(qFresh)
            vm.createAndSelectQuestionnaire("New Title", 2)
            assertEquals(
                "qFresh",
                vm.uiState.value.selectedQuestionnaire
                    ?.id,
            )
        }

    /**
     * Test filtering answers on form change with type mismatches.
     */
    @Test
    fun testFilterAnswersWithTypeMismatches() =
        runTest {
            val dummyPatient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(dummyPatient)

            val intItem =
                Questionnaire.Item.Builder(
                    dev.ohs.fhir.model.r4.String.Builder().apply {
                        value = "item_int"
                    },
                    Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                )
            val decItem =
                Questionnaire.Item.Builder(
                    dev.ohs.fhir.model.r4.String.Builder().apply {
                        value = "item_dec"
                    },
                    Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                )
            val boolItem =
                Questionnaire.Item.Builder(
                    dev.ohs.fhir.model.r4.String.Builder().apply {
                        value = "item_bool"
                    },
                    Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                )
            val strItem =
                Questionnaire.Item.Builder(
                    dev.ohs.fhir.model.r4.String.Builder().apply {
                        value = "item_str"
                    },
                    Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                )

            val formWithTypes =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "q_typed"
                        item.add(intItem)
                        item.add(decItem)
                        item.add(boolItem)
                        item.add(strItem)
                    }.build()

            val initialForm =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id =
                            "q_initial"
                    }.build()

            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(initialForm, formWithTypes))

            val vm = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vm.initialize("pat1", "new", emptyMap())
            advanceUntilIdle()

            // Provide valid and invalid values
            vm.onAnswerChanged("item_int", "not-an-int")
            vm.onAnswerChanged("item_dec", "not-a-decimal")
            vm.onAnswerChanged("item_bool", "not-a-bool")
            vm.onAnswerChanged("item_str", "valid-string")
            vm.onAnswerChanged("unmatched_link", "persists")

            vm.selectQuestionnaire(formWithTypes)

            // Incompatible answers should have been filtered out
            assertFalse(
                vm.uiState.value.answers
                    .containsKey("item_int"),
            )
            assertFalse(
                vm.uiState.value.answers
                    .containsKey("item_dec"),
            )
            assertFalse(
                vm.uiState.value.answers
                    .containsKey("item_bool"),
            )
            assertTrue(
                vm.uiState.value.answers
                    .containsKey("item_str"),
            )
            assertTrue(
                vm.uiState.value.answers
                    .containsKey("unmatched_link"),
            )

            // Now provide compatible numbers and booleans
            vm.onAnswerChanged("item_int", 42)
            vm.onAnswerChanged("item_dec", 3.14)
            vm.onAnswerChanged("item_bool", true)

            vm.selectQuestionnaire(formWithTypes)
            assertTrue(
                vm.uiState.value.answers
                    .containsKey("item_int"),
            )
            assertTrue(
                vm.uiState.value.answers
                    .containsKey("item_dec"),
            )
            assertTrue(
                vm.uiState.value.answers
                    .containsKey("item_bool"),
            )

            // Test Long and numeric String representations
            vm.onAnswerChanged("item_int", 42L)
            vm.selectQuestionnaire(formWithTypes)
            assertTrue(
                vm.uiState.value.answers
                    .containsKey("item_int"),
            )

            vm.onAnswerChanged("item_int", "100")
            vm.onAnswerChanged("item_dec", "99.9")
            vm.onAnswerChanged("item_bool", "true")
            vm.selectQuestionnaire(formWithTypes)
            assertTrue(
                vm.uiState.value.answers
                    .containsKey("item_int"),
            )
            assertTrue(
                vm.uiState.value.answers
                    .containsKey("item_dec"),
            )
            assertTrue(
                vm.uiState.value.answers
                    .containsKey("item_bool"),
            )
        }

    /**
     * Test finish encounter notes generation across String, Boolean, List, Float and empty notes.
     */
    @Test
    fun testFinishEncounterNotesVariants() =
        runTest {
            val dummyPatient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(dummyPatient)

            val qItemText =
                Questionnaire.Item
                    .Builder(
                        dev.ohs.fhir.model.r4.String.Builder().apply {
                            value = "link_text"
                        },
                        Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                    ).apply {
                        text =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "Notes Question" }
                    }
            val dummyQ =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = "q_notes"
                        item.add(qItemText)
                    }.build()

            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(dummyQ))

            val vm = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vm.initialize("pat1", "new", emptyMap())
            advanceUntilIdle()

            vm.onAnswerChanged("link_text", "Feeling well")
            vm.onAnswerChanged("link_blank", "   ")
            vm.onAnswerChanged("link_bool_t", true)
            vm.onAnswerChanged("link_bool_f", false)
            vm.onAnswerChanged("link_list", listOf("Tag1", "Tag2"))
            vm.onAnswerChanged("link_empty_list", emptyList<String>())
            vm.onAnswerChanged("link_float", 98.6f)

            vm.finalizeEncounter("Yes", "No", "No Notes Recorded")
            advanceUntilIdle()

            verify(fhirRepository).updateEncounterStatus(
                anyObj("new-id"),
                anyObj("finished"),
                anyObj("Notes Question: Feeling well. link_bool_t: Yes. link_bool_f: No. link_list: Tag1, Tag2. link_float: 98.6."),
            )

            // Finish encounter with empty answers
            val vmEmpty = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(emptyList())
            vmEmpty.initialize("pat1", "new", emptyMap())
            advanceUntilIdle()
            // Notes when selectedQuestionnaire is null
            vmEmpty.onAnswerChanged("unlinked_key", "Unlinked answer")
            vmEmpty.finalizeEncounter("Yes", "No", "No Notes Recorded")
            advanceUntilIdle()
        }

    /**
     * Test saveResponse photos with context identifier and without url.
     */
    @Test
    fun testSaveResponsePhotoVariants() =
        runTest {
            val encId = "enc-photo-vars"
            val dummyEncounter =
                createFhirEncounter(
                    id = encId,
                    patientId = "pat1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                )
            val dummyPatient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(dummyPatient)
            `when`(fhirRepository.getEncounter(encId)).thenReturn(dummyEncounter)

            val photoWithCtx =
                io.healthplatform.chartcam.models.createFhirDocumentReference(
                    DocumentReferenceCreationParams(
                        id = "doc-ctx",
                        patientId = "pat1",
                        encounterId = encId,
                        dateStr = "2026-07-09",
                        desc = "desc",
                        mime = "image/jpeg",
                        urlPath = "path/1",
                        answerCode = "step_from_ctx",
                    ),
                )
            val photoNoUrl =
                io.healthplatform.chartcam.models
                    .createFhirDocumentReference(
                        DocumentReferenceCreationParams(
                            id = "doc-no-url",
                            patientId = "pat1",
                            encounterId = encId,
                            dateStr = "2026-07-09",
                            desc = "step_from_desc",
                            mime = "image/jpeg",
                            urlPath = "",
                        ),
                    ).toBuilder()
                    .apply {
                        context = null
                        content.clear()
                        content.add(
                            dev.ohs.fhir.model.r4.DocumentReference.Content
                                .Builder(
                                    dev.ohs.fhir.model.r4.Attachment
                                        .Builder(),
                                ),
                        )
                    }.build()

            val photoEmpty =
                io.healthplatform.chartcam.models
                    .createFhirDocumentReference(
                        DocumentReferenceCreationParams(
                            id = "doc-empty",
                            patientId = "pat1",
                            encounterId = encId,
                            dateStr = "2026-07-09",
                            desc = "desc",
                            mime = "image/jpeg",
                            urlPath = "path/2",
                        ),
                    ).toBuilder()
                    .apply {
                        context = null
                        description = null
                        content.clear()
                    }.build()

            `when`(fhirRepository.getPhotosForEncounter(encId)).thenReturn(listOf(photoWithCtx, photoNoUrl, photoEmpty))
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter(encId)).thenReturn(emptyList())

            val q =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id =
                            "q_prov"
                    }.build()
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(q))

            val vm = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vm.initialize("pat1", encId, emptyMap())
            advanceUntilIdle()

            vm.finalizeEncounter()
            advanceUntilIdle()
        }

    /**
     * Test deleteEncounter with file storage and early returns.
     */
    @Test
    fun testDeleteEncounterWithFileStorageAndEarlyReturns() =
        runTest {
            val mockStorage = mock(io.healthplatform.chartcam.files.FileStorage::class.java)
            val vmWithStorage = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository, fileStorage = mockStorage)

            // Delete encounter when encounter is null (early return)
            var deleteInvoked = false
            vmWithStorage.deleteEncounter { deleteInvoked = true }
            assertFalse(deleteInvoked)

            // Reopen and finalize when encounter is null (early return)
            vmWithStorage.reopenEncounter()
            vmWithStorage.finalizeEncounter()

            // Initialize and delete with storage
            val dummyPatient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(dummyPatient)
            vmWithStorage.initialize("pat1", "new", emptyMap())
            advanceUntilIdle()

            // Initialize when patient != null (early return)
            vmWithStorage.initialize("pat1", "new", emptyMap())
            advanceUntilIdle()

            var success = false
            vmWithStorage.deleteEncounter { success = true }
            advanceUntilIdle()
            assertTrue(success)
        }

    /**
     * Test buildSubjectReference and buildEncounterReference with existing prefixes.
     */
    @Test
    fun testSubjectAndEncounterReferencePrefixes() =
        runTest {
            val encId = "Encounter/prefixed-enc"
            val dummyEncounter =
                createFhirEncounter(
                    id = encId,
                    patientId = "pat1",
                    practitionerId = "prac1",
                    dateStr = "2026-07-09",
                ).toBuilder()
                    .apply {
                        subject =
                            dev.ohs.fhir.model.r4.Reference.Builder().apply {
                                reference =
                                    dev.ohs.fhir.model.r4.String
                                        .Builder()
                                        .apply { value = "Patient/pat1" }
                            }
                    }.build()

            val dummyPatient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(dummyPatient)
            `when`(fhirRepository.getEncounter(encId)).thenReturn(dummyEncounter)
            `when`(fhirRepository.getPhotosForEncounter(encId)).thenReturn(emptyList())
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter(encId)).thenReturn(emptyList())

            val q =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id =
                            "q_pref"
                    }.build()
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(q))

            val vm = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vm.initialize("pat1", encId, emptyMap())
            advanceUntilIdle()

            vm.finalizeEncounter()
            advanceUntilIdle()
        }

    /**
     * Test remaining edge cases and null branches in EncounterDetailViewModel.
     */
    @Test
    fun testExhaustiveEdgeCasesAndNullBranches() =
        runTest {
            val dummyPatient = Patient.Builder().apply { id = "pat1" }.build()
            `when`(fhirRepository.getPatient("pat1")).thenReturn(dummyPatient)

            val encClass =
                dev.ohs.fhir.model.r4
                    .Coding(
                        code =
                            dev.ohs.fhir.model.r4
                                .Code(value = "AMB"),
                    )

            // Encounter with null id and null subject
            val encNoId =
                dev.ohs.fhir.model.r4.Encounter(
                    id = null,
                    status =
                        dev.ohs.fhir.model.r4
                            .Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = null,
                )

            `when`(fhirRepository.getEncounter("enc_no_id")).thenReturn(encNoId)
            `when`(fhirRepository.getPhotosForEncounter("enc_no_id")).thenReturn(emptyList())

            // QR with null questionnaire
            val qrNoQ =
                QuestionnaireResponse
                    .Builder(Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
                    .apply {
                        questionnaire = null
                    }.build()
            `when`(fhirRepository.getQuestionnaireResponsesForEncounter("enc_no_id")).thenReturn(listOf(qrNoQ))

            val vm = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            assertEquals("", vm.extractCurrentPractitionerId())
            vm.initialize("pat1", "enc_no_id", emptyMap())
            advanceUntilIdle()

            // Abort operations when encounter.id is null
            vm.finalizeEncounter()
            vm.reopenEncounter()
            vm.deleteEncounter {}

            // Encounters with various null subject reference structures
            val dummyQActive =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply { id = "q_active" }
                    .build()
            `when`(questionnaireRepository.getAvailableQuestionnaires()).thenReturn(listOf(dummyQActive))

            val encSubjNoRef =
                dev.ohs.fhir.model.r4.Encounter(
                    id = "enc_ref_test",
                    status =
                        dev.ohs.fhir.model.r4
                            .Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject =
                        dev.ohs.fhir.model.r4
                            .Reference(reference = null),
                )

            val encSubjNullVal =
                dev.ohs.fhir.model.r4.Encounter(
                    id = "enc_ref_test",
                    status =
                        dev.ohs.fhir.model.r4
                            .Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject =
                        dev.ohs.fhir.model.r4
                            .Reference(
                                reference =
                                    dev.ohs.fhir.model.r4
                                        .String(value = null),
                            ),
                )

            val encSubjNull =
                dev.ohs.fhir.model.r4.Encounter(
                    id = "enc_subj_null",
                    status =
                        dev.ohs.fhir.model.r4
                            .Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                    `class` = encClass,
                    subject = null,
                )

            `when`(fhirRepository.getEncounter("enc_subj_null")).thenReturn(encSubjNull)
            val vmSubjNull = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vmSubjNull.initialize("pat1", "enc_subj_null", emptyMap())
            advanceUntilIdle()
            vmSubjNull.finalizeEncounter()

            `when`(fhirRepository.getEncounter("enc_ref_test")).thenReturn(encSubjNoRef)
            val vmSubj1 = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vmSubj1.initialize("pat1", "enc_ref_test", emptyMap())
            advanceUntilIdle()
            vmSubj1.finalizeEncounter()

            `when`(fhirRepository.getEncounter("enc_ref_test")).thenReturn(encSubjNullVal)
            val vmSubj2 = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vmSubj2.initialize("pat1", "enc_ref_test", emptyMap())
            advanceUntilIdle()
            vmSubj2.finalizeEncounter()

            // Form with null ID and items with null linkId, null type, and null text
            val itemNullLinkId =
                Questionnaire.Item.Builder(
                    dev.ohs.fhir.model.r4.String
                        .Builder()
                        .apply { value = null },
                    Enumeration(value = null),
                )
            val itemNullType =
                Questionnaire.Item.Builder(
                    dev.ohs.fhir.model.r4.String.Builder().apply {
                        value = "item_null_type"
                    },
                    Enumeration(value = null),
                )
            val itemNullText =
                Questionnaire.Item
                    .Builder(
                        dev.ohs.fhir.model.r4.String.Builder().apply {
                            value = "item_no_text"
                        },
                        Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                    ).apply {
                        text =
                            null
                    }
            val qNullId =
                Questionnaire
                    .Builder(Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active))
                    .apply {
                        id = null
                        item.add(itemNullLinkId)
                        item.add(itemNullType)
                        item.add(itemNullText)
                    }.build()

            val vmQNull = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vmQNull.initialize("pat1", "enc_ref_test", emptyMap())
            advanceUntilIdle()
            vmQNull.selectQuestionnaire(qNullId)
            vmQNull.onAnswerChanged("item_no_text", "hello")
            vmQNull.onAnswerChanged("item_null_type", "persists")
            vmQNull.finalizeEncounter()

            // Documents with various null identifiers and URLs
            val docNullIdent =
                io.healthplatform.chartcam.models
                    .createFhirDocumentReference(
                        DocumentReferenceCreationParams(
                            id = "doc-ident-null",
                            patientId = "pat1",
                            encounterId = "enc_ref_test",
                            dateStr = "2026-07-09",
                            desc = "desc",
                            mime = "image/jpeg",
                            urlPath = "path",
                        ),
                    ).toBuilder()
                    .apply {
                        context =
                            dev.ohs.fhir.model.r4.DocumentReference.Context.Builder().apply {
                                related.add(
                                    dev.ohs.fhir.model.r4.Reference
                                        .Builder()
                                        .apply { identifier = null },
                                )
                            }
                    }.build()

            val docNullIdentVal =
                docNullIdent
                    .toBuilder()
                    .apply {
                        context =
                            dev.ohs.fhir.model.r4.DocumentReference.Context.Builder().apply {
                                related.clear()
                                related.add(
                                    dev.ohs.fhir.model.r4.Reference.Builder().apply {
                                        identifier =
                                            dev.ohs.fhir.model.r4.Identifier
                                                .Builder()
                                                .apply { value = null }
                                    },
                                )
                            }
                    }.build()

            val docNullUrl =
                docNullIdent
                    .toBuilder()
                    .apply {
                        context = null
                        description =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "step_with_null_url" }
                        content.clear()
                        content.add(
                            dev.ohs.fhir.model.r4.DocumentReference.Content.Builder(
                                dev.ohs.fhir.model.r4.Attachment.Builder().apply {
                                    url =
                                        null
                                },
                            ),
                        )
                    }.build()

            val docEmptyContentWithDesc =
                docNullIdent
                    .toBuilder()
                    .apply {
                        context = null
                        description =
                            dev.ohs.fhir.model.r4.String
                                .Builder()
                                .apply { value = "step_empty_content" }
                        content.clear()
                    }.build()

            `when`(
                fhirRepository.getPhotosForEncounter("enc_ref_test"),
            ).thenReturn(listOf(docNullIdent, docNullIdentVal, docNullUrl, docEmptyContentWithDesc))
            val vmDocs = EncounterDetailViewModel(fhirRepository, authRepository, questionnaireRepository)
            vmDocs.initialize("pat1", "enc_ref_test", emptyMap())
            advanceUntilIdle()
            vmDocs.finalizeEncounter()

            // Practitioner with null ID
            val nullPracAuthRepo = mock(AuthRepository::class.java)
            `when`(nullPracAuthRepo.currentUser).thenReturn(
                MutableStateFlow(
                    dev.ohs.fhir.model.r4
                        .Practitioner(id = null),
                ),
            )
            val vmNullPrac = EncounterDetailViewModel(fhirRepository, nullPracAuthRepo, questionnaireRepository)
            vmNullPrac.initialize("pat1", "enc_ref_test", emptyMap())
            advanceUntilIdle()
            vmNullPrac.finalizeEncounter()
            advanceUntilIdle()

            // Practitioner is null
            val noPracAuthRepo = mock(AuthRepository::class.java)
            `when`(noPracAuthRepo.currentUser).thenReturn(MutableStateFlow(null))
            val vmNoPrac = EncounterDetailViewModel(fhirRepository, noPracAuthRepo, questionnaireRepository)
            vmNoPrac.initialize("pat1", "enc_ref_test", emptyMap())
            advanceUntilIdle()
            vmNoPrac.finalizeEncounter()
            advanceUntilIdle()
        }
}
