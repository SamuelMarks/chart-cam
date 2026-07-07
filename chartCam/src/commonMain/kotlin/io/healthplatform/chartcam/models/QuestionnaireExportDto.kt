package io.healthplatform.chartcam.models

import kotlinx.serialization.Serializable

/**
 * A standard JSON schema wrapper for exporting and importing FHIR Questionnaire templates.
 * Includes a version identifier for future compatibility.
 */
@Serializable
data class QuestionnaireExportDto(
    /**
     * The schema version. Current version is 1.
     */
    val version: Int = 1,
    /**
     * The application that generated this export.
     */
    val app: String = "ChartCam",
    /**
     * The raw FHIR JSON string of the Questionnaire resource.
     */
    val fhirJson: String,
)
