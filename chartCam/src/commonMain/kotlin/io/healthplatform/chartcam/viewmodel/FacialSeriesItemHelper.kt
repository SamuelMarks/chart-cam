/**
 * @file FacialSeriesItemHelper.kt
 * Contains declarations for FacialSeriesItemHelper.kt.
 *
 * Helper functions to construct standardized FHIR child items for facial series widgets.
 */
package io.healthplatform.chartcam.viewmodel

import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Populates the 3 standardized child items for a facial profile series group.
 *
 * @param groupBuilder The group item builder to append child items to.
 * @param baseLinkId The base linkId of the group.
 * @param leftProfileText Localized text for the left profile.
 * @param frontViewText Localized text for the front view.
 * @param rightProfileText Localized text for the right profile.
 * @return A [Result] encapsulating either unit on success, or an exception on failure.
 */
internal fun applyFacialSeriesItems(
    groupBuilder: Questionnaire.Item.Builder,
    baseLinkId: String,
    leftProfileText: String,
    frontViewText: String,
    rightProfileText: String,
): Result<Unit> =
    if (baseLinkId.isBlank()) {
        Result.failure(IllegalArgumentException("baseLinkId cannot be blank"))
    } else {
        runCatching {
            groupBuilder.item.add(
                createFacialSeriesChildItem(
                    linkId = "${baseLinkId}_left",
                    text = leftProfileText,
                    silhouetteCode = "profile-cornea-left",
                    snomedCode = "272480006",
                    snomedDisplay = "Left lateral",
                ).getOrThrow(),
            )
            groupBuilder.item.add(
                createFacialSeriesChildItem(
                    linkId = "${baseLinkId}_front",
                    text = frontViewText,
                    silhouetteCode = "frontal-face",
                    snomedCode = "272483008",
                    snomedDisplay = "Anterior",
                ).getOrThrow(),
            )
            groupBuilder.item.add(
                createFacialSeriesChildItem(
                    linkId = "${baseLinkId}_right",
                    text = rightProfileText,
                    silhouetteCode = "profile-cornea-right",
                    snomedCode = "272481005",
                    snomedDisplay = "Right lateral",
                ).getOrThrow(),
            )
        }
    }

/**
 * Creates a child attachment item for the facial profile series.
 *
 * @param linkId Unique item identifier.
 * @param text Human-readable question text.
 * @param silhouetteCode The camera silhouette preset code.
 * @param snomedCode The anatomical orientation SNOMED code.
 * @param snomedDisplay The anatomical orientation display name.
 * @return A [Result] encapsulating the populated [Questionnaire.Item.Builder] or an exception on failure.
 */
internal fun createFacialSeriesChildItem(
    linkId: String,
    text: String,
    silhouetteCode: String,
    snomedCode: String,
    snomedDisplay: String,
): Result<Questionnaire.Item.Builder> =
    if (linkId.isBlank() || silhouetteCode.isBlank() || snomedCode.isBlank()) {
        Result.failure(IllegalArgumentException("Required parameters cannot be blank"))
    } else {
        runCatching {
            Questionnaire.Item
                .Builder(
                    FhirString(value = linkId).toBuilder(),
                    Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                ).apply {
                    this.text = FhirString(value = text).toBuilder()
                    this.required = FhirBoolean(value = true).toBuilder()
                    this.extension.add(
                        Extension
                            .Builder(
                                url = "http://healthplatform.io/fhir/StructureDefinition/camera-silhouette",
                            ).apply {
                                value =
                                    Extension.Value.Code(
                                        Code(value = silhouetteCode),
                                    )
                            },
                    )
                    this.code.add(
                        Coding(
                            system = Uri(value = "http://loinc.org"),
                            code = Code(value = "72170-4"),
                            display = FhirString(value = "Photographic image"),
                        ).toBuilder(),
                    )
                    this.code.add(
                        Coding(
                            system = Uri(value = "http://snomed.info/sct"),
                            code = Code(value = snomedCode),
                            display = FhirString(value = snomedDisplay),
                        ).toBuilder(),
                    )
                }
        }
    }
