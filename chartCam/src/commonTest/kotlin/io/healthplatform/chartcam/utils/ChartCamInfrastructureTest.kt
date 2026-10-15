/**
 * @file ChartCamInfrastructureTest.kt
 * Unit tests for [ChartCamError], [DispatcherProvider], [LocaleProvider], [createCryptoService],
 * [FileStorage.clearCacheCatching], [QuestionnaireRepository] suspending helpers, and [ExportImportReferenceHelper].
 */
package io.healthplatform.chartcam.utils

import dev.ohs.fhir.model.r4.Base64Binary
import dev.ohs.fhir.model.r4.Binary
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.files.clearCacheCatching
import io.healthplatform.chartcam.repository.ExportImportReferenceHelper
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Comprehensive unit tests for core infrastructure, error types, providers, and repository helpers.
 */
class ChartCamInfrastructureTest {
    /**
     * Verifies all [ChartCamError] subtypes and their messages and causes.
     */
    @Test
    fun testChartCamErrorSubtypes() {
        val rootCause = RuntimeException("root")
        val storageErr = ChartCamError.StorageFailure("disk full", rootCause)
        assertEquals("disk full", storageErr.message)
        assertEquals(rootCause, storageErr.cause)
        val storageNoCause = ChartCamError.StorageFailure("disk full no cause")
        assertNull(storageNoCause.cause)

        val cryptoErr = ChartCamError.CryptoFailure("bad tag", rootCause)
        assertEquals("bad tag", cryptoErr.message)
        assertEquals(rootCause, cryptoErr.cause)
        val cryptoNoCause = ChartCamError.CryptoFailure("bad tag no cause")
        assertNull(cryptoNoCause.cause)

        val validationErr = ChartCamError.ValidationFailure("invalid field", rootCause)
        assertEquals("invalid field", validationErr.message)
        assertEquals(rootCause, validationErr.cause)
        val validationNoCause = ChartCamError.ValidationFailure("invalid field no cause")
        assertNull(validationNoCause.cause)

        val hardwareErr = ChartCamError.HardwareFailure("camera busy", rootCause)
        assertEquals("camera busy", hardwareErr.message)
        assertEquals(rootCause, hardwareErr.cause)
        val hardwareNoCause = ChartCamError.HardwareFailure("camera busy no cause")
        assertNull(hardwareNoCause.cause)

        val stateErr = ChartCamError.StateViolation("limit reached", rootCause)
        assertEquals("limit reached", stateErr.message)
        assertEquals(rootCause, stateErr.cause)
        val stateNoCause = ChartCamError.StateViolation("limit reached no cause")
        assertNull(stateNoCause.cause)
    }

    /**
     * Verifies [DefaultDispatcherProvider], [DefaultLocaleProvider], [createCryptoService], and [FileStorage.clearCacheCatching].
     */
    @Test
    fun testProvidersAndStorageExtensions() {
        assertNotNull(DefaultDispatcherProvider.main)
        assertNotNull(DefaultDispatcherProvider.default)
        assertNotNull(DefaultDispatcherProvider.io)

        appLocaleState.value = "es"
        assertEquals("es", DefaultLocaleProvider.currentLanguage())
        assertEquals("es", DefaultLocaleProvider.languageFlow.value)
        appLocaleState.value = "en"

        val crypto = createCryptoService()
        assertNotNull(crypto)

        var cacheCleared = false
        val fakeStorage =
            object : FileStorage {
                override fun saveImage(
                    fileName: String,
                    bytes: ByteArray,
                ): String = fileName

                override fun readImage(path: String): ByteArray = ByteArray(0)

                override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

                override fun clearCache() {
                    cacheCleared = true
                }
            }
        assertTrue(fakeStorage.clearCacheCatching().isSuccess)
        assertTrue(cacheCleared)
    }

    /**
     * Verifies [QuestionnaireRepository] custom externalScope constructor and suspending save/delete methods.
     */
    @Test
    fun testQuestionnaireRepositorySuspendingOperations() =
        runTest {
            val customScope = CoroutineScope(SupervisorJob() + DefaultDispatcherProvider.default)
            val repo = QuestionnaireRepository(fhirRepository = null, externalScope = customScope)

            val missingIdQ = Questionnaire(status = Enumeration(value = PublicationStatus.Active))
            assertTrue(repo.saveQuestionnaireSuspending(missingIdQ).isFailure)

            val validQ =
                Questionnaire(
                    id = "q-suspend-1",
                    status = Enumeration(value = PublicationStatus.Active),
                    title = FhirString(value = "Suspend Test"),
                )
            assertTrue(repo.saveQuestionnaireSuspending(validQ).isSuccess)
            assertNotNull(repo.getQuestionnaire("q-suspend-1"))

            assertTrue(repo.deleteQuestionnaireSuspending("q-suspend-1").isSuccess)
            assertNull(repo.getQuestionnaire("q-suspend-1"))
        }

    /**
     * Verifies all branches of [ExportImportReferenceHelper].
     */
    @Test
    fun testExportImportReferenceHelperBranches() {
        assertEquals("", ExportImportReferenceHelper.extractEncounterReferenceId(null))
        assertEquals("", ExportImportReferenceHelper.extractEncounterReferenceId(Reference()))
        assertEquals(
            "enc-1",
            ExportImportReferenceHelper.extractEncounterReferenceId(
                Reference(reference = FhirString(value = "Encounter/enc-1")),
            ),
        )

        assertEquals("", ExportImportReferenceHelper.extractPractitionerReferenceId(null))
        assertEquals("", ExportImportReferenceHelper.extractPractitionerReferenceId(Reference()))
        assertEquals(
            "prac-1",
            ExportImportReferenceHelper.extractPractitionerReferenceId(
                Reference(reference = FhirString(value = "Practitioner/prac-1")),
            ),
        )

        assertEquals("", ExportImportReferenceHelper.extractPatientReferenceId(null))
        assertEquals("", ExportImportReferenceHelper.extractPatientReferenceId(Reference()))
        assertEquals(
            "pat-1",
            ExportImportReferenceHelper.extractPatientReferenceId(
                Reference(reference = FhirString(value = "Patient/pat-1")),
            ),
        )

        assertFalse(ExportImportReferenceHelper.organizationMatchesPractitioner(null, "prac-1"))
        assertFalse(ExportImportReferenceHelper.organizationMatchesPractitioner(Reference(), "prac-1"))
        assertFalse(
            ExportImportReferenceHelper.organizationMatchesPractitioner(
                Reference(reference = FhirString(value = "Organization/other")),
                "prac-1",
            ),
        )
        assertTrue(
            ExportImportReferenceHelper.organizationMatchesPractitioner(
                Reference(reference = FhirString(value = "Practitioner/prac-1")),
                "prac-1",
            ),
        )

        val emptyBinary = Binary(contentType = Code(value = "image/jpeg"))
        assertNull(ExportImportReferenceHelper.decodeBinaryBytes(emptyBinary))

        val nullValBinary = Binary(contentType = Code(value = "image/jpeg"), data = Base64Binary())
        assertNull(ExportImportReferenceHelper.decodeBinaryBytes(nullValBinary))

        val invalidBase64Binary =
            Binary(
                contentType = Code(value = "image/jpeg"),
                data = Base64Binary(value = "%%%invalid_base64%%%"),
            )
        assertNull(ExportImportReferenceHelper.decodeBinaryBytes(invalidBase64Binary))

        val validBinary =
            Binary(
                contentType = Code(value = "image/jpeg"),
                data = Base64Binary(value = "AQID"),
            )
        assertNotNull(ExportImportReferenceHelper.decodeBinaryBytes(validBinary))
    }
}
