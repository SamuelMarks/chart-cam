/**
 * @file QuestionnaireSharingServiceTest.kt
 * Contains declarations for QuestionnaireSharingServiceTest.kt.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for the Questionnaire sharing and serialization behavior.
 */
class QuestionnaireSharingServiceTest {
    /** Test instance of the sharing service. */
    private val service = QuestionnaireSharingService()

    /**
     * Validates round-trip serialization logic.
     */
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

        // Ensure it's not empty and contains expected fields
        assertEquals(true, json.contains("test-form"))

        val deserialized = service.deserializeQuestionnaire(json)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.title?.value, deserialized.title?.value)
    }

    /**
     * Validates deserialization failure on malformed json.
     */
    @Test
    fun testDeserializeInvalidFormatFails() {
        val invalidJson = """not a json"""
        assertFailsWith<IllegalArgumentException> {
            service.deserializeQuestionnaire(invalidJson)
        }
    }
}
