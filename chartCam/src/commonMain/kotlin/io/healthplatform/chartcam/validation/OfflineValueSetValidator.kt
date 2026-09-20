/**
 * @file OfflineValueSetValidator.kt
 * Validates FHIR Coding, CodeableConcept, and Questionnaire answers against offline ValueSets and CodeSystems.
 */

package io.healthplatform.chartcam.validation

import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.ExtensibleEnumeration
import dev.ohs.fhir.model.r4.FhirEnum
import dev.ohs.fhir.model.r4.OperationOutcome
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import io.healthplatform.chartcam.utils.UUID
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Offline terminology validator operating entirely without remote network or REST endpoints.
 *
 * Verifies that clinical codes, choices, and questionnaire responses conform to
 * declared in-memory ValueSets, CodeSystems, and answer options.
 */
object OfflineValueSetValidator {
    /**
     * Validates an [ExtensibleEnumeration] against declared permissible codes and allowed custom code extensions.
     *
     * @param T The underlying [FhirEnum] type.
     * @param enumeration The [ExtensibleEnumeration] to validate.
     * @param allowedCustomCodes Optional set of permitted custom codes if custom values are restricted.
     * @return A [Result] enclosing an [OperationOutcome] if invalid, or null if valid.
     */
    fun <T : FhirEnum> validateExtensibleEnumeration(
        enumeration: ExtensibleEnumeration<T>,
        allowedCustomCodes: Set<String>? = null,
    ): Result<OperationOutcome?> =
        runCatching {
            when (enumeration) {
                is ExtensibleEnumeration.Predefined -> null
                is ExtensibleEnumeration.Custom -> {
                    val customCode = enumeration.code
                    if (customCode.isBlank()) {
                        createOutcome(
                            severity = OperationOutcome.IssueSeverity.Error,
                            code = OperationOutcome.IssueType.Required,
                            diagnostics = "Custom code in ExtensibleEnumeration cannot be blank",
                            expression = "ExtensibleEnumeration.code",
                        )
                    } else if (allowedCustomCodes != null && !allowedCustomCodes.contains(customCode)) {
                        createOutcome(
                            severity = OperationOutcome.IssueSeverity.Warning,
                            code = OperationOutcome.IssueType.Code_Invalid,
                            diagnostics = "Custom code '$customCode' is not in permitted extension set",
                            expression = "ExtensibleEnumeration.code",
                        )
                    } else {
                        null
                    }
                }
            }
        }

    /**
     * Validates a [Coding] against a permitted set of codes and an expected terminology system.
     *
     * @param coding The FHIR [Coding] to validate.
     * @param allowedCodes The set of valid code strings within the system.
     * @param expectedSystem Optional expected system URI (e.g. "http://loinc.org").
     * @return A [Result] enclosing an [OperationOutcome] if invalid, or null if valid.
     */
    fun validateCoding(
        coding: Coding,
        allowedCodes: Set<String>,
        expectedSystem: String? = null,
    ): Result<OperationOutcome?> =
        runCatching {
            validateCodingInternal(coding, allowedCodes, expectedSystem)
        }

    /**
     * Internal validation for a single coding without Result wrapper.
     *
     * @param coding The coding to check.
     * @param allowedCodes Permitted code strings.
     * @param expectedSystem Optional expected system.
     * @return OperationOutcome if invalid, or null if valid.
     */
    private fun validateCodingInternal(
        coding: Coding,
        allowedCodes: Set<String>,
        expectedSystem: String?,
    ): OperationOutcome? {
        val codeVal = coding.code?.value
        val sysVal = coding.system?.value

        return when {
            codeVal.isNullOrBlank() ->
                createOutcome(
                    severity = OperationOutcome.IssueSeverity.Error,
                    code = OperationOutcome.IssueType.Required,
                    diagnostics = "Coding is missing required code element",
                    expression = "Coding.code",
                )
            expectedSystem != null && sysVal != expectedSystem ->
                createOutcome(
                    severity = OperationOutcome.IssueSeverity.Warning,
                    code = OperationOutcome.IssueType.Code_Invalid,
                    diagnostics = "Expected system '$expectedSystem' but found '$sysVal'",
                    expression = "Coding.system",
                )
            allowedCodes.isNotEmpty() && !allowedCodes.contains(codeVal) ->
                createOutcome(
                    severity = OperationOutcome.IssueSeverity.Error,
                    code = OperationOutcome.IssueType.Code_Invalid,
                    diagnostics = "Code '$codeVal' is not in permitted value set",
                    expression = "Coding.code",
                )
            else -> null
        }
    }

    /**
     * Validates a [CodeableConcept] containing one or more codings against a permitted set of codes.
     *
     * @param concept The FHIR [CodeableConcept] to validate.
     * @param allowedCodes The set of valid code strings.
     * @param expectedSystem Optional expected system URI.
     * @return A [Result] enclosing an [OperationOutcome] if invalid, or null if valid.
     */
    fun validateCodeableConcept(
        concept: CodeableConcept,
        allowedCodes: Set<String>,
        expectedSystem: String? = null,
    ): Result<OperationOutcome?> =
        runCatching {
            if (concept.coding.isEmpty()) {
                return@runCatching createOutcome(
                    severity = OperationOutcome.IssueSeverity.Warning,
                    code = OperationOutcome.IssueType.Required,
                    diagnostics = "CodeableConcept has no coding elements",
                    expression = "CodeableConcept.coding",
                )
            }

            val issues = mutableListOf<OperationOutcome.Issue>()
            for (coding in concept.coding) {
                val outcome = validateCodingInternal(coding, allowedCodes, expectedSystem)
                if (outcome != null) {
                    issues.addAll(outcome.issue)
                }
            }

            if (issues.isEmpty()) null else OperationOutcome(id = UUID.randomUUID(), issue = issues)
        }

    /**
     * Validates all answers in a [QuestionnaireResponse] against answer options declared in the template.
     *
     * @param questionnaire The template [Questionnaire] defining choice options.
     * @param response The [QuestionnaireResponse] to validate.
     * @return A [Result] enclosing an [OperationOutcome] describing any invalid choices, or null if valid.
     */
    fun validateResponseOptions(
        questionnaire: Questionnaire,
        response: QuestionnaireResponse,
    ): Result<OperationOutcome?> =
        runCatching {
            val qMap = mutableMapOf<String, Questionnaire.Item>()

            /**
             * Indexes questionnaire items by linkId recursively.
             *
             * @param items The items to index.
             */
            fun indexItems(items: List<Questionnaire.Item>) {
                for (it in items) {
                    val id = it.linkId.value ?: it.linkId.id
                    if (!id.isNullOrBlank()) qMap[id] = it
                    indexItems(it.item)
                }
            }
            indexItems(questionnaire.item)

            val issues = mutableListOf<OperationOutcome.Issue>()

            /**
             * Traverses response items recursively and verifies choice answers.
             *
             * @param rItems The response items to check.
             */
            fun checkResponseItems(rItems: List<QuestionnaireResponse.Item>) {
                for (rItem in rItems) {
                    val linkId = rItem.linkId.value ?: rItem.linkId.id ?: ""
                    val qItem = qMap[linkId]
                    if (qItem != null) {
                        checkChoiceAnswerItem(qItem, rItem, issues, linkId)
                    }
                    checkResponseItems(rItem.item)
                }
            }

            checkResponseItems(response.item)
            if (issues.isEmpty()) null else OperationOutcome(id = UUID.randomUUID(), issue = issues)
        }

    /**
     * Validates answers for a single questionnaire response item against declared choice options.
     *
     * @param qItem Corresponding questionnaire template item.
     * @param rItem Received response item.
     * @param issues Mutable list of issues to populate.
     * @param linkId Identifier of the question being validated.
     */
    private fun checkChoiceAnswerItem(
        qItem: Questionnaire.Item,
        rItem: QuestionnaireResponse.Item,
        issues: MutableList<OperationOutcome.Issue>,
        linkId: String,
    ) {
        if (qItem.type.value != Questionnaire.QuestionnaireItemType.Choice || qItem.answerOption.isEmpty()) {
            return
        }
        val validOptionStrings =
            qItem.answerOption
                .mapNotNull { opt ->
                    when (val v = opt.value) {
                        is Questionnaire.Item.AnswerOption.Value.String -> v.value.value
                        is Questionnaire.Item.AnswerOption.Value.Coding -> v.value.code?.value
                        else -> null
                    }
                }.toSet()

        for (ans in rItem.answer) {
            val answerStr =
                when (val v = ans.value) {
                    is QuestionnaireResponse.Item.Answer.Value.String -> v.value.value
                    is QuestionnaireResponse.Item.Answer.Value.Coding -> v.value.code?.value
                    else -> null
                }
            if (answerStr != null && !validOptionStrings.contains(answerStr)) {
                val diag = "Answer '$answerStr' is not a valid option for choice question '$linkId'"
                val expr = "QuestionnaireResponse.item.where(linkId='$linkId').answer"
                issues.add(
                    OperationOutcome.Issue(
                        severity = Enumeration(value = OperationOutcome.IssueSeverity.Error),
                        code = Enumeration(value = OperationOutcome.IssueType.Code_Invalid),
                        diagnostics = FhirString(value = diag),
                        expression = listOf(FhirString(value = expr)),
                    ),
                )
            }
        }
    }

    /**
     * Constructs a single-issue OperationOutcome.
     *
     * @param severity Issue severity.
     * @param code Issue type code.
     * @param diagnostics Diagnostic message.
     * @param expression Target FHIRPath expression.
     * @return Populated [OperationOutcome].
     */
    private fun createOutcome(
        severity: OperationOutcome.IssueSeverity,
        code: OperationOutcome.IssueType,
        diagnostics: String,
        expression: String,
    ): OperationOutcome =
        OperationOutcome(
            id = UUID.randomUUID(),
            issue =
                listOf(
                    OperationOutcome.Issue(
                        severity = Enumeration(value = severity),
                        code = Enumeration(value = code),
                        diagnostics = FhirString(value = diagnostics),
                        expression = listOf(FhirString(value = expression)),
                    ),
                ),
        )
}
