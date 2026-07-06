package io.healthplatform.chartcam.repository

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QuestionnaireRepositoryTest {
    @Test
    fun testQuestionnaireRepository() {
        val repository = QuestionnaireRepository()

        // Test fetching available predefined questionnaires
        val available = repository.getAvailableQuestionnaires()
        assertEquals(2, available.size)

        // Test fetching a specific one
        val stdForm = repository.getQuestionnaire("std-form")
        assertNotNull(stdForm)
        assertEquals("std-form", stdForm.id)

        // Test fetching non-existent
        assertNull(repository.getQuestionnaire("does-not-exist"))

        // Test dynamic creation
        val customForm = repository.createQuestionnaire("My Custom Form", 2, "Label 1, Label 2")
        assertNotNull(customForm)
        assertEquals("custom-my-custom-form", customForm.id)

        // Fetch it back
        val fetchedCustom = repository.getQuestionnaire("custom-my-custom-form")
        assertNotNull(fetchedCustom)

        // Total should now be 3
        assertEquals(3, repository.getAvailableQuestionnaires().size)
    }
}
