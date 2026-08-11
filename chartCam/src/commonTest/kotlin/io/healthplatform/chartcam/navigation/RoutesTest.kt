/**
 * @file RoutesTest.kt
 * Contains declarations for RoutesTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the [Routes] object and data classes defining navigation routes.
 */
class RoutesTest {
    /**
     * Validates that standard routes and route arguments parse correctly.
     */
    @Test
    fun testRoutes() {
        assertEquals("/auth/login", Routes.LOGIN)
        assertEquals("/capture", Routes.CAPTURE)
        assertEquals("/patients", Routes.PATIENT_LIST)
        assertEquals("/questionnaires", Routes.QUESTIONNAIRE_LIST)

        val pr = PatientDetailRoute("p1")
        assertEquals("p1", pr.patientId)

        val pvr = PatientVisitsRoute("p2")
        assertEquals("p2", pvr.patientId)

        val cr = CaptureForPatientRoute("p3", "q1", "l1")
        assertEquals("p3", cr.patientId)
        assertEquals("q1", cr.questionnaireId)
        assertEquals("l1", cr.linkId)

        val vr = VisitDetailRoute("p4", "v1")
        assertEquals("p4", vr.patientId)
        assertEquals("v1", vr.visitId)

        val nvr = NewVisitRoute("p5")
        assertEquals("p5", nvr.patientId)

        val qbr = QuestionnaireBuilderRoute("q2")
        assertEquals("q2", qbr.duplicateFromId)

        val triage = TriageRoute
        assertNotNull(triage)
    }
}
