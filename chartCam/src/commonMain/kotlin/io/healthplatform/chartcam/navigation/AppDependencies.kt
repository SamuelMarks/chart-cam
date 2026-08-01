/**
 * @file AppDependencies.kt
 * Contains declarations for AppDependencies.
 *
 * Defines the core dependencies required for the application's navigation graph.
 */
package io.healthplatform.chartcam.navigation

import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sync.SyncWorker

/**
 * Contains the core repository and service dependencies required for the application.
 *
 * @property authRepository Manages user authentication and session state.
 * @property fhirRepository Handles interactions with the local FHIR database.
 * @property questionnaireRepository Manages questionnaire definitions and forms.
 * @property exportImportService Handles exporting and importing application data.
 * @property syncWorker Manages background synchronization of data.
 * @property photoSessionManager Manages the state of captured photos during an active session.
 */
data class AppDependencies(
    val authRepository: AuthRepository,
    val fhirRepository: FhirRepository,
    val questionnaireRepository: QuestionnaireRepository,
    val exportImportService: ExportImportService,
    val syncWorker: SyncWorker,
    val photoSessionManager: PhotoSessionManager,
)
