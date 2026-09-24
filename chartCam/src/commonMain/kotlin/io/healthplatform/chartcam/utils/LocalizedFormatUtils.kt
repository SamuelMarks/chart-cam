/**
 * @file LocalizedFormatUtils.kt
 * Contains declarations for LocalizedFormatUtils.kt.
 *
 * Provides safe, Result-based string formatting, date parsing, and localized conflict resolution utilities.
 */
package io.healthplatform.chartcam.utils

import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.conflict_type_exact_match
import chartcam.chartcam.generated.resources.conflict_type_id_collision
import chartcam.chartcam.generated.resources.conflict_type_mrn_collision
import chartcam.chartcam.generated.resources.conflict_type_orphan
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.models.ConflictType
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

/**
 * Marker object providing namespace and declaration matching for localized format utilities.
 */
object LocalizedFormatUtils

/**
 * Error indicating that a localized template formatting or resolution operation failed.
 *
 * @property message The detailed error message describing the failure reason.
 */
class FormatError(
    override val message: String,
) : Throwable(message)

/**
 * Safely formats a localized template string with arguments without throwing exceptions.
 *
 * Replaces positional placeholders such as `%1$s`, `%2$d`, `%s`, and `%d` with the provided arguments.
 *
 * @param template The template string containing format specifiers.
 * @param args The variable arguments to substitute into the template.
 * @return A [Result] containing the formatted string or a [FormatError] if placeholders cannot be matched.
 */
fun formatTemplateSafely(
    template: String,
    vararg args: Any,
): Result<String> {
    var result = template
    var index = 1
    for (arg in args) {
        val indexedPattern = "%" + index + "\\$[a-zA-Z]"
        val regexIndexed = Regex(indexedPattern)
        if (regexIndexed.containsMatchIn(result)) {
            result = result.replace(regexIndexed, arg.toString())
        } else if (result.contains("%s") || result.contains("%d")) {
            result = result.replaceFirst(Regex("%[a-zA-Z]"), arg.toString())
        } else {
            return Result.failure(
                FormatError("Template '$template' does not contain enough placeholders for argument $index"),
            )
        }
        index++
    }
    return Result.success(result)
}

/**
 * Safely parses a flexible user-entered date string according to the active language conventions returning a [Result].
 *
 * @param input The raw input date string to parse.
 * @param language The BCP-47 language tag to use for disambiguating date components.
 * @return A [Result] enclosing the parsed [LocalDate] or a [FormatError] if parsing fails.
 */
fun safeParseFlexibleDate(
    input: String,
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): Result<LocalDate> {
    val parsed =
        io.healthplatform.chartcam.ui.components
            .parseFlexibleDate(input, language)
    return if (parsed != null) {
        Result.success(parsed)
    } else {
        Result.failure(FormatError("Unable to parse date '$input' for language '$language'"))
    }
}

/**
 * Safely resolves a raw conflict type string to its corresponding [ConflictType] returning a [Result].
 *
 * @param rawType The string representation of the conflict type.
 * @return A [Result] containing the resolved [ConflictType] or a [FormatError] if not found.
 */
fun safeResolveConflictType(rawType: String): Result<ConflictType> {
    val trimmed = rawType.trim()
    val match = ConflictType.entries.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
    return if (match != null) {
        Result.success(match)
    } else {
        Result.failure(FormatError("Unknown conflict type: $rawType"))
    }
}

/**
 * Resolves the localized [StringResource] for this [ConflictType].
 *
 * @return The corresponding localized [StringResource].
 */
fun ConflictType.toLocalizedResource(): StringResource =
    when (this) {
        ConflictType.EXACT_MATCH -> Res.string.conflict_type_exact_match
        ConflictType.ID_COLLISION_DIFFERENT_DATA -> Res.string.conflict_type_id_collision
        ConflictType.MRN_COLLISION_DIFFERENT_ID -> Res.string.conflict_type_mrn_collision
        ConflictType.ORPHAN_ENCOUNTER -> Res.string.conflict_type_orphan
    }

/**
 * Resolves a human-readable, localized slot title from a Questionnaire definition returning a [Result].
 *
 * @param key The slot or item linkId to locate.
 * @param questionnaire The FHIR Questionnaire containing item definitions, if available.
 * @return A [Result] enclosing the resolved label or a [FormatError] if not found.
 */
fun getLocalizedSlotLabel(
    key: String,
    questionnaire: Questionnaire?,
): Result<String> {
    if (questionnaire != null && questionnaire.item.isNotEmpty()) {
        val item = QuestionnaireUtils.findItemRecursively(questionnaire.item, key)
        val label = item?.text?.value
        if (label != null && label.isNotBlank()) {
            return Result.success(label)
        }
    }
    return Result.failure(FormatError("Slot item not found for key: $key"))
}
