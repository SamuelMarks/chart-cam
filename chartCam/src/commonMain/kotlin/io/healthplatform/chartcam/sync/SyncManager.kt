package io.healthplatform.chartcam.sync

import com.google.fhir.model.r4.Bundle
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.Resource
import com.google.fhir.model.r4.Uri
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.utils.UUID
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Handles communication with the remote FHIR server.
 * Responsible for pushing local transactions up to the server and
 * pulling remote searchsets down to update the local database.
 */
class SyncManager(
    private val fhirRepository: FhirRepository,
    private val httpClient: HttpClient,
    private val fileStorage: FileStorage
) {
    private val fhirJson = FhirR4Json()
    private val baseUrl = "https://mock-fhir-server.example.com/fhir"

    /**
     * Uploads the encounter and all associated records to the server as a single transaction.
     * @param encounterId the ID of the encounter to sync.
     * @return true if the upload was successful.
     */
    suspend fun syncEncounter(encounterId: String): Boolean {
        // App is currently offline-only, so we just return true.
        // Data is already saved to the local database via fhirRepository.
        println("=== SYNC START: Encounter $encounterId ===")
        println("App is offline-only. Skipping server sync.")
        println("=== SYNC COMPLETE (Success=true) ===")
        return true
    }

    /**
     * Downloads an entire patient's history (Encounters, DocumentReferences, etc) via a GET search.
     * Parses the incoming Searchset bundle and populates the local database.
     * 
     * @param patientId The patient to retrieve records for.
     * @param lastUpdated Optional ISO-8601 date to filter updates by (e.g. gt2024-01-01).
     * @return true if fetch and save was successful.
     */
    suspend fun fetchPatientHistory(patientId: String, lastUpdated: String? = null): Boolean {
        try {
            var url = "$baseUrl/Patient/$patientId/\$everything"
            if (lastUpdated != null) {
                url += "?_lastUpdated=$lastUpdated"
            }
            
            val response: HttpResponse = httpClient.get(url) {
                contentType(ContentType.Application.Json)
            }
            
            if (!response.status.isSuccess()) {
                return false
            }
            
            val bodyText = response.bodyAsText()
            val bundle = fhirJson.decodeFromString(bodyText) as? Bundle ?: return false
            
            if (bundle.type?.value != Bundle.BundleType.Searchset && bundle.type?.value != Bundle.BundleType.History) {
                return false
            }

            for (entry in bundle.entry) {
                val resource = entry.resource ?: continue
                
                when (resource) {
                    is Patient -> {
                        fhirRepository.savePatient(resource)
                    }
                    is Encounter -> {
                        fhirRepository.saveEncounter(resource)
                    }
                    is QuestionnaireResponse -> {
                        fhirRepository.saveQuestionnaireResponse(resource)
                    }
                    is DocumentReference -> {
                        fhirRepository.saveDocumentReference(resource)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
