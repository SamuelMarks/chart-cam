package io.healthplatform.chartcam.repository

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestionnaireRepositoryTest {
    @Test
    fun testQuestionnaireRepository() =
        kotlinx.coroutines.runBlocking {
            val repository = QuestionnaireRepository()
            repository.loadDefaultForms()

            // Test fetching available predefined questionnaires
            val available = repository.getAvailableQuestionnaires()
            assertTrue(available.isNotEmpty())

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
