package io.healthplatform.chartcam.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Practitioner(
    val id: String,
    val active: Boolean = true,
    val name: List<HumanName> = emptyList()
)

@Serializable
data class Patient(
    val id: String,
    val name: List<HumanName>,
    val birthDate: LocalDate,
    val gender: String,
    val mrn: String,
    val managingOrganization: String? = null
)

@Serializable
data class Encounter(
    val id: String,
    val status: String,
    val type: String = "clinical-photography",
    val subjectReference: String, // Patient ID
    val participantReference: String, // Practitioner ID
    val period: Period,
    val text: String? = null
)

/**
 * FHIR DocumentReference Resource (Simplified).
 * Represents a pointer to the binary photo data.
 *
 * @property id Logical ID.
 * @property subjectReference Patient ID trying to be linked.
 * @property contextReference Encounter ID this doc is part of.
 * @property date Creation date.
 * @property description Title/Caption (e.g. "Front View").
 * @property content The attachment details (url/path).
 */
@Serializable
data class DocumentReference(
    val id: String,
    val subjectReference: String,
    val contextReference: String, // Encounter
    val date: LocalDateTime,
    val description: String?,
    val content: Attachment
)

/**
 * FHIR Attachment structure.
 *
 * @property contentType Mime type (image/jpeg).
 * @property url Local file URI or Remote URL.
 */
@Serializable
data class Attachment(
    val contentType: String,
    val url: String
)

@Serializable
data class HumanName(
    val family: String,
    val given: List<String> = emptyList()
)

@Serializable
data class Period(
    val start: LocalDateTime,
    val end: LocalDateTime? = null
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val tokenType: String
)

/**
 * FHIR Questionnaire Resource.
 */
@Serializable
data class Questionnaire(
    val id: String,
    val title: String,
    val status: String,
    val item: List<QuestionnaireItem> = emptyList()
)

/**
 * Item in a Questionnaire.
 */
@Serializable
data class QuestionnaireItem(
    val linkId: String,
    val text: String,
    val type: String, // "string", "attachment"
    val required: Boolean = false,
    val repeats: Boolean = false
)

/**
 * FHIR QuestionnaireResponse Resource.
 */
@Serializable
data class QuestionnaireResponse(
    val id: String,
    val questionnaire: String, // ID of Questionnaire
    val status: String, // "completed", "in-progress"
    val subject: String, // Patient ID
    val encounter: String, // Encounter ID
    val author: String, // Practitioner ID
    val item: List<QuestionnaireResponseItem> = emptyList()
)

/**
 * Item in a QuestionnaireResponse.
 */
@Serializable
data class QuestionnaireResponseItem(
    val linkId: String,
    val text: String,
    val answer: List<QuestionnaireResponseAnswer> = emptyList()
)

/**
 * Answer in a QuestionnaireResponse.
 */
@Serializable
data class QuestionnaireResponseAnswer(
    val valueString: String? = null,
    val valueAttachment: Attachment? = null
)
