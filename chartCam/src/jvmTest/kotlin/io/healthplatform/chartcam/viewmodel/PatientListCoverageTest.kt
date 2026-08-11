/**
 * @file PatientListCoverageTest.kt
 * Contains declarations for PatientListCoverageTest.kt.
 */
package io.healthplatform.chartcam.viewmodel

import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.mockito.Mockito

/**
 * Coverage test class for PatientListViewModel.
 */
class PatientListCoverageTest {
    /**
     * Tests delete account with null fields.
     */
    @Test
    fun testDeleteAccountWithNullFields() {
        val fhirRepository = Mockito.mock(FhirRepository::class.java)
        val authRepository = Mockito.mock(AuthRepository::class.java)
        val exportImportService = Mockito.mock(ExportImportService::class.java)

        val practitioner = Practitioner.Builder().build() // no ID, no name
        Mockito.`when`(authRepository.currentUser).thenReturn(MutableStateFlow(practitioner))

        val patient = Patient.Builder().build() // no ID
        kotlinx.coroutines.runBlocking {
            Mockito.`when`(fhirRepository.getAllPatients(false, "")).thenReturn(listOf(patient))
        }

        val vm = PatientListViewModel(fhirRepository, exportImportService, authRepository)
        vm.deleteAccount { }
        kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(100) }
    }
}
