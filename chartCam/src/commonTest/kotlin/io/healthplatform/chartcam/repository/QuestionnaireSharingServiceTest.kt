package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuestionnaireSharingServiceTest {
    private val service = QuestionnaireSharingService()

    @Test
    fun testSerializationAndDeserialization() {
        val original =
            Questionnaire
                .Builder(Enumeration(value = PublicationStatus.Active))
                .apply {
                    this.id = "test-form"
                    this.title =
                        com.google.fhir.model.r4.String
                            .Builder()
                            .apply { value = "Test Title" }
                }.build()

        val json = service.serializeQuestionnaire(original)

        // Ensure it's not empty and contains our app signature
        assertEquals(true, json.contains("ChartCam"))

        val deserialized = service.deserializeQuestionnaire(json)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.title?.value, deserialized.title?.value)
    }

    @Test
    fun testDeserializeInvalidAppFails() {
        val invalidJson = """{"version": 1, "app": "OtherApp", "fhirJson": "{}"}"""
        assertFailsWith<IllegalArgumentException> {
            service.deserializeQuestionnaire(invalidJson)
        }
    }

    @Test
    fun testDeserializeUnsupportedVersionFails() {
        val invalidJson = """{"version": 2, "app": "ChartCam", "fhirJson": "{}"}"""
        assertFailsWith<IllegalArgumentException> {
            service.deserializeQuestionnaire(invalidJson)
        }
    }

    @Test
    fun testDeserializeInvalidFormatFails() {
        val invalidJson = """not a json"""
        assertFailsWith<IllegalArgumentException> {
            service.deserializeQuestionnaire(invalidJson)
        }
    }
}
