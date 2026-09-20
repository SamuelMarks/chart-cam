/**
 * @file RoutesTest.kt
 * Contains declarations for RoutesTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

        val qbrDefault = QuestionnaireBuilderRoute()
        assertEquals(null, qbrDefault.duplicateFromId)

        val triage = TriageRoute
        assertNotNull(triage)

        val dvr = DicomViewerRoute("/path/to/dicom.dcm")
        assertEquals("/path/to/dicom.dcm", dvr.filePath)

        val dvrCopy = dvr.copy()
        assertEquals(dvr, dvrCopy)
        assertEquals(dvr.hashCode(), dvrCopy.hashCode())
        assertNotNull(dvr.toString())

        val crDefault = CaptureForPatientRoute("p10")
        assertEquals("p10", crDefault.patientId)
        assertEquals(null, crDefault.questionnaireId)
        assertEquals(null, crDefault.linkId)
    }

    /**
     * Validates data class equality, copy, destructuring, and toString branches across route models.
     */
    @Test
    fun testRouteDataClassesEqualityAndComponents() {
        // PatientDetailRoute
        val pdr1 = PatientDetailRoute("p1")
        val pdr2 = PatientDetailRoute("p1")
        val pdr3 = PatientDetailRoute("p2")
        assertTrue(pdr1.equals(pdr1))
        assertEquals(pdr1, pdr2)
        assertEquals(pdr1.hashCode(), pdr2.hashCode())
        assertNotEquals(pdr1, pdr3)
        assertFalse(pdr1.equals(null))
        assertFalse(pdr1.equals("p1"))
        assertEquals("p1", pdr1.component1())
        assertEquals("PatientDetailRoute(patientId=p1)", pdr1.toString())
        assertEquals(pdr1, pdr1.copy())
        assertEquals(pdr3, pdr1.copy(patientId = "p2"))

        // PatientVisitsRoute
        val pvr1 = PatientVisitsRoute("p1")
        val pvr2 = PatientVisitsRoute("p1")
        val pvr3 = PatientVisitsRoute("p2")
        assertTrue(pvr1.equals(pvr1))
        assertEquals(pvr1, pvr2)
        assertEquals(pvr1.hashCode(), pvr2.hashCode())
        assertNotEquals(pvr1, pvr3)
        assertFalse(pvr1.equals(null))
        assertFalse(pvr1.equals("p1"))
        assertEquals("p1", pvr1.component1())
        assertEquals("PatientVisitsRoute(patientId=p1)", pvr1.toString())
        assertEquals(pvr1, pvr1.copy())
        assertEquals(pvr3, pvr1.copy(patientId = "p2"))

        // CaptureForPatientRoute
        val cfr1 = CaptureForPatientRoute("p1", "q1", "l1")
        val cfr2 = CaptureForPatientRoute("p1", "q1", "l1")
        val cfr3 = cfr1.copy(patientId = "p2")
        val cfr4 = cfr1.copy(questionnaireId = "q2")
        val cfr5 = cfr1.copy(linkId = "l2")
        val cfr6 = cfr1.copy(patientId = "p2", questionnaireId = "q2")
        val cfr7 = cfr1.copy(patientId = "p2", linkId = "l2")
        val cfr8 = cfr1.copy(questionnaireId = "q2", linkId = "l2")
        val cfr9 = cfr1.copy(patientId = "p2", questionnaireId = "q2", linkId = "l2")
        val cfrCopy = cfr1.copy()
        assertTrue(cfr1.equals(cfr1))
        assertEquals(cfr1, cfr2)
        assertEquals(cfr1, cfrCopy)
        assertEquals(cfr1.hashCode(), cfr2.hashCode())
        assertNotEquals(cfr1, cfr3)
        assertNotEquals(cfr1, cfr4)
        assertNotEquals(cfr1, cfr5)
        assertNotEquals(cfr1, cfr6)
        assertNotEquals(cfr1, cfr7)
        assertNotEquals(cfr1, cfr8)
        assertNotEquals(cfr1, cfr9)
        assertFalse(cfr1.equals(null))
        assertFalse(cfr1.equals("other"))

        val cfrNull1 = CaptureForPatientRoute("p1", null, null)
        val cfrNull2 = CaptureForPatientRoute("p1", null, null)
        val cfrQOnly = CaptureForPatientRoute("p1", "q1", null)
        val cfrLOnly = CaptureForPatientRoute("p1", null, "l1")
        val cfrCons1 = CaptureForPatientRoute("p1")
        val cfrCons2 = CaptureForPatientRoute("p1", "q1")
        assertTrue(cfrNull1.equals(cfrNull1))
        assertTrue(cfrNull1.equals(cfrNull2))
        assertEquals(cfrNull1.hashCode(), cfrNull2.hashCode())
        assertEquals(cfrNull1, cfrCons1)
        assertEquals(cfrQOnly, cfrCons2)
        assertFalse(cfrNull1.equals(cfr1))
        assertFalse(cfr1.equals(cfrNull1))
        assertFalse(cfrNull1.equals(cfrQOnly))
        assertFalse(cfrQOnly.equals(cfrNull1))
        assertFalse(cfrNull1.equals(cfrLOnly))
        assertFalse(cfrLOnly.equals(cfrNull1))
        assertFalse(cfrQOnly.equals(cfrLOnly))
        assertFalse(cfrLOnly.equals(cfrQOnly))
        assertFalse(cfr1.equals(cfrQOnly))
        assertFalse(cfrQOnly.equals(cfr1))
        assertFalse(cfr1.equals(cfrLOnly))
        assertFalse(cfrLOnly.equals(cfr1))

        val (cp, cq, cl) = cfr1
        assertEquals("p1", cp)
        assertEquals("q1", cq)
        assertEquals("l1", cl)
        assertTrue(cfr1.toString().contains("patientId=p1"))

        // VisitDetailRoute
        val vdr1 = VisitDetailRoute("p1", "v1")
        val vdr2 = VisitDetailRoute("p1", "v1")
        val vdr3 = vdr1.copy(patientId = "p2")
        val vdr4 = vdr1.copy(visitId = "v2")
        val vdr5 = vdr1.copy(patientId = "p2", visitId = "v2")
        val vdrCopy = vdr1.copy()
        assertTrue(vdr1.equals(vdr1))
        assertEquals(vdr1, vdr2)
        assertEquals(vdr1, vdrCopy)
        assertEquals(vdr1.hashCode(), vdr2.hashCode())
        assertNotEquals(vdr1, vdr3)
        assertNotEquals(vdr1, vdr4)
        assertNotEquals(vdr1, vdr5)
        assertFalse(vdr1.equals(null))
        assertFalse(vdr1.equals("vdr"))
        val (vp, vv) = vdr1
        assertEquals("p1", vp)
        assertEquals("v1", vv)
        assertTrue(vdr1.toString().contains("visitId=v1"))

        // NewVisitRoute
        val nvr1 = NewVisitRoute("p1")
        val nvr2 = NewVisitRoute("p1")
        val nvr3 = NewVisitRoute("p2")
        assertTrue(nvr1.equals(nvr1))
        assertEquals(nvr1, nvr2)
        assertEquals(nvr1.hashCode(), nvr2.hashCode())
        assertNotEquals(nvr1, nvr3)
        assertFalse(nvr1.equals(null))
        assertFalse(nvr1.equals("nvr"))
        assertEquals("p1", nvr1.component1())
        assertEquals("NewVisitRoute(patientId=p1)", nvr1.toString())
        assertEquals(nvr1, nvr1.copy())
        assertEquals(nvr3, nvr1.copy(patientId = "p2"))

        // QuestionnaireBuilderRoute
        val qbr1 = QuestionnaireBuilderRoute("dup-1")
        val qbr2 = QuestionnaireBuilderRoute("dup-1")
        val qbr3 = QuestionnaireBuilderRoute("dup-2")
        val qbrNull1 = QuestionnaireBuilderRoute(null)
        val qbrNull2 = QuestionnaireBuilderRoute(null)
        val qbrDefault1 = QuestionnaireBuilderRoute()
        val qbrNamedParam = QuestionnaireBuilderRoute(duplicateFromId = "dup-1")
        val qbrNamedNull = QuestionnaireBuilderRoute(duplicateFromId = null)
        assertTrue(qbr1.equals(qbr1))
        assertTrue(qbrNull1.equals(qbrNull1))
        assertTrue(qbrNull1.equals(qbrNull2))
        assertEquals(qbrNull1, qbrDefault1)
        assertEquals(qbr1, qbrNamedParam)
        assertEquals(qbrNull1, qbrNamedNull)
        assertEquals(qbrNull1.hashCode(), qbrNull2.hashCode())
        assertEquals(qbr1, qbr2)
        assertEquals(qbr1.hashCode(), qbr2.hashCode())
        assertNotEquals(qbr1, qbr3)
        assertNotEquals(qbr1, qbrNull1)
        assertNotEquals(qbrNull1, qbr1)
        assertNotEquals(qbrNull1, qbr3)
        assertNotEquals(qbr3, qbrNull1)
        assertFalse(qbr1.equals(null))
        assertFalse(qbr1.equals("qbr"))
        assertFalse(qbrNull1.equals(null))
        assertFalse(qbrNull1.equals("qbr"))
        assertEquals("dup-1", qbr1.component1())
        assertTrue(qbr1.toString().contains("duplicateFromId=dup-1"))
        assertEquals(qbr1, qbr1.copy())
        assertEquals(qbr3, qbr1.copy(duplicateFromId = "dup-2"))
        assertEquals(qbrNull1, qbr1.copy(duplicateFromId = null))
        assertEquals(qbrNull1, qbrNull1.copy())
        assertEquals(qbr3, qbrNull1.copy("dup-2"))
        assertEquals(qbrNull1, qbrNull1.copy(null))
        assertEquals(qbr1, qbr1.copy("dup-1"))

        // DicomViewerRoute
        val dvr1 = DicomViewerRoute("/file.dcm")
        val dvr2 = DicomViewerRoute("/file.dcm")
        val dvr3 = DicomViewerRoute("/other.dcm")
        assertTrue(dvr1.equals(dvr1))
        assertEquals(dvr1, dvr2)
        assertEquals(dvr1.hashCode(), dvr2.hashCode())
        assertNotEquals(dvr1, dvr3)
        assertFalse(dvr1.equals(null))
        assertFalse(dvr1.equals("dvr"))
        assertEquals("/file.dcm", dvr1.component1())
        assertEquals("DicomViewerRoute(filePath=/file.dcm)", dvr1.toString())
        assertEquals(dvr1, dvr1.copy())
        assertEquals(dvr3, dvr1.copy(filePath = "/other.dcm"))

        // TriageRoute
        val tr = TriageRoute
        assertNotNull(tr.toString())
    }

    /**
     * Validates serialization and deserialization round trips for all serializable routes.
     */
    @Test
    fun testRoutesSerializationRoundTrip() {
        // TriageRoute
        val trJson = Json.encodeToString(TriageRoute.serializer(), TriageRoute)
        val trDecoded = Json.decodeFromString(TriageRoute.serializer(), trJson)
        assertEquals(TriageRoute, trDecoded)

        // PatientDetailRoute
        val pdr = PatientDetailRoute("p123")
        val pdrJson = Json.encodeToString(PatientDetailRoute.serializer(), pdr)
        val pdrDecoded = Json.decodeFromString(PatientDetailRoute.serializer(), pdrJson)
        assertEquals(pdr, pdrDecoded)

        // PatientVisitsRoute
        val pvr = PatientVisitsRoute("p456")
        val pvrJson = Json.encodeToString(PatientVisitsRoute.serializer(), pvr)
        val pvrDecoded = Json.decodeFromString(PatientVisitsRoute.serializer(), pvrJson)
        assertEquals(pvr, pvrDecoded)

        // CaptureForPatientRoute with all non-null
        val cfrFull = CaptureForPatientRoute("p789", "q-std", "link-42")
        val cfrFullJson = Json.encodeToString(CaptureForPatientRoute.serializer(), cfrFull)
        val cfrFullDecoded = Json.decodeFromString(CaptureForPatientRoute.serializer(), cfrFullJson)
        assertEquals(cfrFull, cfrFullDecoded)

        // CaptureForPatientRoute with nulls and omitted defaults
        val cfrSparse = CaptureForPatientRoute("p789")
        val cfrSparseJson = Json.encodeToString(CaptureForPatientRoute.serializer(), cfrSparse)
        val cfrSparseDecoded = Json.decodeFromString(CaptureForPatientRoute.serializer(), cfrSparseJson)
        assertEquals(cfrSparse, cfrSparseDecoded)

        val cfrPartialJson = """{"patientId":"p789","questionnaireId":"q-1"}"""
        val cfrPartialDecoded = Json.decodeFromString(CaptureForPatientRoute.serializer(), cfrPartialJson)
        assertEquals("p789", cfrPartialDecoded.patientId)
        assertEquals("q-1", cfrPartialDecoded.questionnaireId)
        assertEquals(null, cfrPartialDecoded.linkId)

        // VisitDetailRoute
        val vdr = VisitDetailRoute("p1", "v99")
        val vdrJson = Json.encodeToString(VisitDetailRoute.serializer(), vdr)
        val vdrDecoded = Json.decodeFromString(VisitDetailRoute.serializer(), vdrJson)
        assertEquals(vdr, vdrDecoded)

        // NewVisitRoute
        val nvr = NewVisitRoute("p999")
        val nvrJson = Json.encodeToString(NewVisitRoute.serializer(), nvr)
        val nvrDecoded = Json.decodeFromString(NewVisitRoute.serializer(), nvrJson)
        assertEquals(nvr, nvrDecoded)

        // QuestionnaireBuilderRoute with non-null and null duplicateFromId
        val qbrFull = QuestionnaireBuilderRoute("dup-origin")
        val qbrFullJson = Json.encodeToString(QuestionnaireBuilderRoute.serializer(), qbrFull)
        val qbrFullDecoded = Json.decodeFromString(QuestionnaireBuilderRoute.serializer(), qbrFullJson)
        assertEquals(qbrFull, qbrFullDecoded)

        val qbrSparse = QuestionnaireBuilderRoute()
        val qbrSparseJson = Json.encodeToString(QuestionnaireBuilderRoute.serializer(), qbrSparse)
        val qbrSparseDecoded = Json.decodeFromString(QuestionnaireBuilderRoute.serializer(), qbrSparseJson)
        assertEquals(qbrSparse, qbrSparseDecoded)

        val qbrOmittedJson = "{}"
        val qbrOmittedDecoded = Json.decodeFromString(QuestionnaireBuilderRoute.serializer(), qbrOmittedJson)
        assertEquals(null, qbrOmittedDecoded.duplicateFromId)

        val qbrNullJson = """{"duplicateFromId":null}"""
        val qbrNullDecoded = Json.decodeFromString(QuestionnaireBuilderRoute.serializer(), qbrNullJson)
        assertEquals(null, qbrNullDecoded.duplicateFromId)

        val cfrNullJson = """{"patientId":"p789","questionnaireId":null,"linkId":null}"""
        val cfrNullExplicitDecoded = Json.decodeFromString(CaptureForPatientRoute.serializer(), cfrNullJson)
        assertEquals(null, cfrNullExplicitDecoded.questionnaireId)
        assertEquals(null, cfrNullExplicitDecoded.linkId)

        // DicomViewerRoute
        val dvr = DicomViewerRoute("/storage/scan.dcm")
        val dvrJson = Json.encodeToString(DicomViewerRoute.serializer(), dvr)
        val dvrDecoded = Json.decodeFromString(DicomViewerRoute.serializer(), dvrJson)
        assertEquals(dvr, dvrDecoded)

        // Test serialization with encodeDefaults enabled to cover default encoding branches
        val jsonWithDefaults = Json { encodeDefaults = true }
        jsonWithDefaults.encodeToString(CaptureForPatientRoute.serializer(), cfrSparse)
        jsonWithDefaults.encodeToString(CaptureForPatientRoute.serializer(), cfrFull)
        jsonWithDefaults.encodeToString(QuestionnaireBuilderRoute.serializer(), qbrSparse)
        jsonWithDefaults.encodeToString(QuestionnaireBuilderRoute.serializer(), qbrFull)

        // Test deserialization of incomplete JSON to trigger serialization constructor validation branches
        kotlin.runCatching { Json.decodeFromString(PatientDetailRoute.serializer(), "{}") }
        kotlin.runCatching { Json.decodeFromString(PatientVisitsRoute.serializer(), "{}") }
        kotlin.runCatching { Json.decodeFromString(CaptureForPatientRoute.serializer(), "{}") }
        kotlin.runCatching { Json.decodeFromString(VisitDetailRoute.serializer(), "{}") }
        kotlin.runCatching { Json.decodeFromString(NewVisitRoute.serializer(), "{}") }
        kotlin.runCatching { Json.decodeFromString(DicomViewerRoute.serializer(), "{}") }
    }
}
