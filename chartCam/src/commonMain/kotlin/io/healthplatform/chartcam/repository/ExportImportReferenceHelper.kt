/**
 * @file ExportImportReferenceHelper.kt
 * Pure helper functions for extracting FHIR references and decoding binary payloads
 * during encrypted archive export and import operations.
 */
package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.Binary
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Reference
import io.healthplatform.chartcam.utils.CryptoServiceContract
import io.healthplatform.chartcam.utils.decryptCatching
import io.healthplatform.chartcam.utils.flatMap
import okio.ByteString.Companion.decodeBase64

/**
 * Internal helper functions supporting [ExportImportService] reference resolution and archive decryption.
 */
internal object ExportImportReferenceHelper {
    /**
     * Extracts a bare Encounter ID from an optional reference element.
     *
     * @param ref The reference element.
     * @return The bare Encounter ID or an empty string.
     */
    fun extractEncounterReferenceId(ref: Reference?): String {
        val raw = if (ref != null) ref.reference else null
        val str = if (raw != null) raw.value else null
        return if (str != null) str.removePrefix("Encounter/") else ""
    }

    /**
     * Extracts a bare Practitioner ID from an optional reference element.
     *
     * @param ref The reference element.
     * @return The bare Practitioner ID or an empty string.
     */
    fun extractPractitionerReferenceId(ref: Reference?): String {
        val raw = if (ref != null) ref.reference else null
        val str = if (raw != null) raw.value else null
        return if (str != null) str.removePrefix("Practitioner/") else ""
    }

    /**
     * Extracts a bare Patient ID from an optional reference element.
     *
     * @param ref The reference element.
     * @return The bare Patient ID or an empty string.
     */
    fun extractPatientReferenceId(ref: Reference?): String {
        val raw = if (ref != null) ref.reference else null
        val str = if (raw != null) raw.value else null
        return if (str != null) str.removePrefix("Patient/") else ""
    }

    /**
     * Checks if managing organization reference contains the specified practitioner ID.
     *
     * @param orgRef The organization reference element.
     * @param practitionerId The practitioner identifier to match.
     * @return True if the reference matches the practitioner ID, false otherwise.
     */
    fun organizationMatchesPractitioner(
        orgRef: Reference?,
        practitionerId: String,
    ): Boolean {
        val raw = if (orgRef != null) orgRef.reference else null
        val str = if (raw != null) raw.value else null
        return str != null && str.contains(practitionerId)
    }

    /**
     * Decodes a Base64-encoded FHIR [Binary] payload into raw bytes.
     *
     * @param binary The FHIR [Binary] resource.
     * @return Decoded [ByteArray], or null if missing or invalid Base64.
     */
    fun decodeBinaryBytes(binary: Binary): ByteArray? {
        val dataObj = binary.data
        val base64Str = if (dataObj != null) dataObj.value else null
        val decoded = if (base64Str != null) base64Str.decodeBase64() else null
        return if (decoded != null) decoded.toByteArray() else null
    }

    /**
     * Decrypts an encrypted archive string and parses it into a FHIR R4 [Bundle] via [Result] composition.
     *
     * @param cryptoService The cryptographic service used to decrypt the payload.
     * @param encryptedData The Base64-encoded encrypted archive string.
     * @param password The decryption password.
     * @return A [Result] enclosing the decoded FHIR [Bundle] or a failure.
     */
    suspend fun decryptAndDecodeBundle(
        cryptoService: CryptoServiceContract,
        encryptedData: String,
        password: String,
    ): Result<Bundle> =
        cryptoService
            .decryptCatching(encryptedData, password)
            .flatMap { jsonData ->
                io.healthplatform.chartcam.fhir.FhirJsonParser
                    .decodeTypedResource(Bundle.serializer(), jsonData)
            }
}
