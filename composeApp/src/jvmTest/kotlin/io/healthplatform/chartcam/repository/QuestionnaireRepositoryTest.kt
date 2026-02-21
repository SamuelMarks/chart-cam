package io.healthplatform.chartcam.repository

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestionnaireRepositoryTest {

    @Test
    fun `getAvailableQuestionnaires returns predefined forms`() {
        val repo = QuestionnaireRepository()
        val forms = repo.getAvailableQuestionnaires()
        assertTrue(forms.size >= 2)
        assertTrue(forms.any { it.id == "std-form" })
        assertTrue(forms.any { it.id == "basic-followup" })
    }

    @Test
    fun `getQuestionnaire returns form or null`() {
        val repo = QuestionnaireRepository()
        assertNotNull(repo.getQuestionnaire("std-form"))
        assertNull(repo.getQuestionnaire("non-existent"))
    }

    @Test
    fun `createQuestionnaire adds a new custom form`() {
        val repo = QuestionnaireRepository()
        val q = repo.createQuestionnaire("My Custom Form", 3)
        assertEquals("custom-my-custom-form", q.id)
        assertEquals("My Custom Form", q.title)
        assertEquals(4, q.item.size) // 1 notes + 3 photos
        
        // It should now be retrievable
        assertNotNull(repo.getQuestionnaire(q.id))
        assertTrue(repo.getAvailableQuestionnaires().contains(q))
    }
}
