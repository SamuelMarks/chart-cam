/**
 * @file FhirValidator.kt
 * Validates FHIR R4 resources against core structure and ChartCam clinical rules.
 */
package io.healthplatform.chartcam.validation

import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.OperationOutcome
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Resource
import io.healthplatform.chartcam.utils.UUID
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Base sealed class for FHIR validation exceptions.
 *
 * @param message Explanation of the validation violation.
 */
sealed class FhirValidationException(
    message: String,
) : Exception(message) {
    /**
     * Exception thrown when a mandatory field is missing in the FHIR resource.
     *
     * @param field The name or path of the missing field.
     */
    class MissingRequiredFieldException(
        val field: String,
    ) : FhirValidationException("Missing required field: $field")

    /**
     * Exception thrown when duplicate linkIds are detected across questionnaire items.
     *
     * @param linkId The duplicated item identifier.
     */
    class DuplicateLinkIdException(
        val linkId: String,
    ) : FhirValidationException("Duplicate linkId found in Questionnaire: $linkId")

    /**
     * Exception thrown when a choice or open-choice question provides no answer options.
     *
     * @param linkId The question linkId lacking options.
     */
    class EmptyChoiceOptionsException(
        val linkId: String,
    ) : FhirValidationException("Choice question '$linkId' must define at least one answer option")

    /**
     * Exception thrown when an enableWhen condition refers to a non-existent question linkId.
     *
     * @param sourceLinkId The question containing the dangling clause.
     * @param targetLinkId The missing target question identifier.
     */
    class DanglingEnableWhenReferenceException(
        val sourceLinkId: String,
        val targetLinkId: String,
    ) : FhirValidationException(
            "Question '$sourceLinkId' has enableWhen condition pointing to non-existent question '$targetLinkId'",
        )
}

/**
 * Enforces ChartCam clinical profile conformance and business integrity rules on FHIR resources.
 * Distinguishes application-specific clinical constraints from base FHIR schema serialization checks.
 */
object FhirValidator {
    /**
     * Validates a FHIR resource against ChartCam clinical profiles.
     *
     * @param resource The FHIR Resource to validate.
     * @return A [Result] indicating success if valid, or a [FhirValidationException] on failure.
     */
    fun validate(resource: Resource): Result<Unit> =
        when (resource) {
            is Patient -> validatePatient(resource)
            is Questionnaire -> validateQuestionnaire(resource)
            is QuestionnaireResponse -> validateQuestionnaireResponse(resource)
            else -> Result.success(Unit)
        }

    /**
     * Validates a FHIR resource and returns a standard FHIR [OperationOutcome] describing any issues.
     *
     * @param resource The FHIR Resource to validate.
     * @return A [Result] enclosing an [OperationOutcome] if defects exist, or null if completely valid.
     */
    fun validateToOutcome(resource: Resource): Result<OperationOutcome?> =
        runCatching {
            val issues = mutableListOf<OperationOutcome.Issue>()

            when (resource) {
                is Patient -> checkPatientOutcomeIssues(resource, issues)
                is Questionnaire -> checkQuestionnaireOutcomeIssues(resource, issues)
                is QuestionnaireResponse -> checkQuestionnaireResponseOutcomeIssues(resource, issues)
                else -> Unit
            }

            if (issues.isEmpty()) {
                null
            } else {
                OperationOutcome(
                    id = UUID.randomUUID(),
                    issue = issues,
                )
            }
        }

    /**
     * Validates a Patient resource, ensuring it has necessary fields like name and identifier.
     *
     * @param patient The Patient resource to validate.
     * @return A [Result] indicating success or failure.
     */
    fun validatePatient(patient: Patient): Result<Unit> {
        val err = checkPatientErrors(patient)
        return if (err != null) Result.failure(err) else Result.success(Unit)
    }

    /**
     * Evaluates structural patient errors.
     *
     * @param patient The patient resource.
     * @return A validation exception if an error is found, or null.
     */
    private fun checkPatientErrors(patient: Patient): FhirValidationException? =
        when {
            patient.name.isEmpty() -> FhirValidationException.MissingRequiredFieldException("name")
            !patient.name.any { it.given.isNotEmpty() } ->
                FhirValidationException.MissingRequiredFieldException("name.given")
            !patient.name.any { it.family?.value?.isNotEmpty() == true } ->
                FhirValidationException.MissingRequiredFieldException("name.family")
            patient.identifier.isEmpty() -> FhirValidationException.MissingRequiredFieldException("identifier")
            else -> null
        }

    /**
     * Validates a Questionnaire against structural rules.
     *
     * @param questionnaire The Questionnaire to validate.
     * @return A [Result] indicating success or failure.
     */
    fun validateQuestionnaire(questionnaire: Questionnaire): Result<Unit> {
        val err = checkQuestionnaireErrors(questionnaire)
        return if (err != null) Result.failure(err) else Result.success(Unit)
    }

    /**
     * Validates a QuestionnaireResponse ensuring required links and status are populated.
     *
     * @param response The [QuestionnaireResponse] to validate.
     * @return A [Result] indicating success or failure.
     */
    fun validateQuestionnaireResponse(response: QuestionnaireResponse): Result<Unit> =
        if (response.status.value == null) {
            Result.failure(FhirValidationException.MissingRequiredFieldException("status"))
        } else {
            Result.success(Unit)
        }

    /**
     * Evaluates structural questionnaire errors.
     *
     * @param questionnaire The questionnaire resource.
     * @return A validation exception if an error is found, or null.
     */
    private fun checkQuestionnaireErrors(questionnaire: Questionnaire): FhirValidationException? {
        val headerError =
            when {
                questionnaire.title?.value.isNullOrEmpty() ->
                    FhirValidationException.MissingRequiredFieldException("title")
                questionnaire.item.isEmpty() -> FhirValidationException.MissingRequiredFieldException("item")
                else -> null
            }
        if (headerError != null) return headerError

        val linkIds = mutableSetOf<String>()
        return validateItems(questionnaire.item, linkIds) ?: validateEnableWhenClauses(questionnaire.item, linkIds)
    }

    /**
     * Validates individual questionnaire items.
     *
     * @param items List of questionnaire items.
     * @param linkIds Set of encountered linkIds.
     * @return A validation exception if an error is found, or null.
     */
    private fun validateItems(
        items: List<Questionnaire.Item>,
        linkIds: MutableSet<String>,
    ): FhirValidationException? {
        for (item in items) {
            val err = validateSingleItem(item, linkIds)
            if (err != null) return err
        }
        return null
    }

    /**
     * Validates an individual questionnaire item.
     *
     * @param item The item to validate.
     * @param linkIds The set of seen linkIds.
     * @return An exception if invalid, or null.
     */
    private fun validateSingleItem(
        item: Questionnaire.Item,
        linkIds: MutableSet<String>,
    ): FhirValidationException? {
        val id = item.linkId.value
        val isChoice = item.type.value == Questionnaire.QuestionnaireItemType.Choice
        return when {
            id.isNullOrEmpty() -> FhirValidationException.MissingRequiredFieldException("item.linkId")
            item.text?.value.isNullOrEmpty() ->
                FhirValidationException.MissingRequiredFieldException("item.text for '$id'")
            !linkIds.add(id) -> FhirValidationException.DuplicateLinkIdException(id)
            isChoice && item.answerOption.isEmpty() -> FhirValidationException.EmptyChoiceOptionsException(id)
            else -> null
        }
    }

    /**
     * Validates that all enableWhen clauses point to existing linkIds.
     *
     * @param items List of questionnaire items.
     * @param linkIds Set of valid linkIds in the questionnaire.
     * @return A validation exception if a dangling clause is found, or null.
     */
    internal fun validateEnableWhenClauses(
        items: List<Questionnaire.Item>,
        linkIds: Set<String>,
    ): FhirValidationException? {
        for (item in items) {
            val sourceId = item.linkId.value ?: ""
            for (ew in item.enableWhen) {
                val target = ew.question.value
                if (target != null && !linkIds.contains(target)) {
                    return FhirValidationException.DanglingEnableWhenReferenceException(sourceId, target)
                }
            }
        }
        return null
    }

    /**
     * Collects Patient validation issues for OperationOutcome generation.
     *
     * @param patient The Patient to validate.
     * @param issues The list of issues to populate.
     */
    private fun checkPatientOutcomeIssues(
        patient: Patient,
        issues: MutableList<OperationOutcome.Issue>,
    ) {
        if (patient.name.isEmpty()) {
            issues.add(
                createIssue(
                    severity = OperationOutcome.IssueSeverity.Error,
                    code = OperationOutcome.IssueType.Required,
                    diagnostics = "Patient is missing required field 'name'",
                    expression = "Patient.name",
                ),
            )
        }
        if (!patient.name.any { it.given.isNotEmpty() }) {
            issues.add(
                createIssue(
                    severity = OperationOutcome.IssueSeverity.Error,
                    code = OperationOutcome.IssueType.Required,
                    diagnostics = "Patient.name is missing 'given' name",
                    expression = "Patient.name.given",
                ),
            )
        }
        if (!patient.name.any { it.family?.value?.isNotEmpty() == true }) {
            issues.add(
                createIssue(
                    severity = OperationOutcome.IssueSeverity.Error,
                    code = OperationOutcome.IssueType.Required,
                    diagnostics = "Patient.name is missing 'family' name",
                    expression = "Patient.name.family",
                ),
            )
        }
        if (patient.identifier.isEmpty()) {
            issues.add(
                createIssue(
                    severity = OperationOutcome.IssueSeverity.Error,
                    code = OperationOutcome.IssueType.Required,
                    diagnostics = "Patient is missing required field 'identifier'",
                    expression = "Patient.identifier",
                ),
            )
        }
    }

    /**
     * Collects Questionnaire validation issues for OperationOutcome generation.
     *
     * @param questionnaire The Questionnaire to validate.
     * @param issues The list of issues to populate.
     */
    private fun checkQuestionnaireOutcomeIssues(
        questionnaire: Questionnaire,
        issues: MutableList<OperationOutcome.Issue>,
    ) {
        if (questionnaire.title?.value.isNullOrBlank()) {
            issues.add(
                createIssue(
                    severity = OperationOutcome.IssueSeverity.Error,
                    code = OperationOutcome.IssueType.Required,
                    diagnostics = "Questionnaire is missing required field 'title'",
                    expression = "Questionnaire.title",
                ),
            )
        }
        val linkIds = mutableSetOf<String>()
        for (item in questionnaire.item) {
            val id = item.linkId.value
            if (id.isNullOrBlank()) {
                issues.add(
                    createIssue(
                        severity = OperationOutcome.IssueSeverity.Error,
                        code = OperationOutcome.IssueType.Required,
                        diagnostics = "Questionnaire item missing linkId",
                        expression = "Questionnaire.item.linkId",
                    ),
                )
            } else if (!linkIds.add(id)) {
                issues.add(
                    createIssue(
                        severity = OperationOutcome.IssueSeverity.Error,
                        code = OperationOutcome.IssueType.Duplicate,
                        diagnostics = "Duplicate linkId: $id",
                        expression = "Questionnaire.item.where(linkId='$id')",
                    ),
                )
            }
        }
    }

    /**
     * Collects QuestionnaireResponse validation issues for OperationOutcome generation.
     *
     * @param response The QuestionnaireResponse to validate.
     * @param issues The list of issues to populate.
     */
    private fun checkQuestionnaireResponseOutcomeIssues(
        response: QuestionnaireResponse,
        issues: MutableList<OperationOutcome.Issue>,
    ) {
        if (response.status.value == null) {
            issues.add(
                createIssue(
                    severity = OperationOutcome.IssueSeverity.Error,
                    code = OperationOutcome.IssueType.Required,
                    diagnostics = "QuestionnaireResponse is missing required field 'status'",
                    expression = "QuestionnaireResponse.status",
                ),
            )
        }
    }

    /**
     * Constructs a typed OperationOutcome Issue.
     *
     * @param severity The severity level.
     * @param code The issue type code.
     * @param diagnostics Descriptive error message.
     * @param expression Target FHIRPath expression.
     * @return The populated [OperationOutcome.Issue].
     */
    private fun createIssue(
        severity: OperationOutcome.IssueSeverity,
        code: OperationOutcome.IssueType,
        diagnostics: kotlin.String,
        expression: kotlin.String,
    ): OperationOutcome.Issue =
        OperationOutcome.Issue(
            severity = Enumeration(value = severity),
            code = Enumeration(value = code),
            diagnostics = FhirString(value = diagnostics),
            expression = listOf(FhirString(value = expression)),
        )
}
