/**
 * @file BodyMapPinDropControl.kt
 * Contains declarations for BodyMapPinDropControl.kt.
 *
 * Interactive anatomical body map pin-drop control supporting anterior and posterior views.
 */
package io.healthplatform.chartcam.ui.sdc.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.body_map_accessible_select_region
import chartcam.chartcam.generated.resources.body_map_clear_pin
import chartcam.chartcam.generated.resources.body_map_location_pinned
import chartcam.chartcam.generated.resources.body_map_site_view_format
import chartcam.chartcam.generated.resources.body_map_tap_instruction
import chartcam.chartcam.generated.resources.body_map_view_anterior
import chartcam.chartcam.generated.resources.body_map_view_posterior
import chartcam.chartcam.generated.resources.body_site_abdomen
import chartcam.chartcam.generated.resources.body_site_chest
import chartcam.chartcam.generated.resources.body_site_head
import chartcam.chartcam.generated.resources.body_site_left_arm
import chartcam.chartcam.generated.resources.body_site_left_leg
import chartcam.chartcam.generated.resources.body_site_lower_back
import chartcam.chartcam.generated.resources.body_site_pelvis
import chartcam.chartcam.generated.resources.body_site_right_arm
import chartcam.chartcam.generated.resources.body_site_right_leg
import chartcam.chartcam.generated.resources.body_site_upper_back
import chartcam.chartcam.generated.resources.cd_body_map_canvas
import chartcam.chartcam.generated.resources.cd_body_map_clear
import chartcam.chartcam.generated.resources.cd_body_map_pin
import io.healthplatform.chartcam.models.BodyMapLocation
import io.healthplatform.chartcam.ui.components.FormLabel
import io.healthplatform.chartcam.ui.theme.AppSpacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val BODY_MAP_CANVAS_HEIGHT = 280

/**
 * Represents a preset anatomical region selectable via assistive navigation.
 *
 * @property name Localized display name of the anatomical region.
 * @property snomed SNOMED CT concept code.
 * @property xPercent Normalized horizontal percentage coordinate.
 * @property yPercent Normalized vertical percentage coordinate.
 */
data class AccessibleBodySite(
    val name: String,
    val snomed: String,
    val xPercent: Float,
    val yPercent: Float,
)

/**
 * Resolves the upper torso region (chest, upper back, or arms).
 *
 * @param xPercent The horizontal percentage.
 * @param isPosterior Whether the view is posterior.
 * @return Pair of string resource and concept code.
 */
private fun resolveUpperTorsoRegion(
    xPercent: Float,
    isPosterior: Boolean,
): Pair<StringResource, String> =
    when {
        xPercent < 35f ->
            if (isPosterior) {
                Res.string.body_site_left_arm to "368208006"
            } else {
                Res.string.body_site_right_arm to "368209003"
            }
        xPercent > 65f ->
            if (isPosterior) {
                Res.string.body_site_right_arm to "368209003"
            } else {
                Res.string.body_site_left_arm to "368208006"
            }
        isPosterior -> Res.string.body_site_upper_back to "181533004"
        else -> Res.string.body_site_chest to "51185008"
    }

/**
 * Resolves the lower torso region (abdomen, pelvis, lower back, or hands/arms).
 *
 * @param xPercent The horizontal percentage.
 * @param yPercent The vertical percentage.
 * @param isPosterior Whether the view is posterior.
 * @return Pair of string resource and concept code.
 */
private fun resolveLowerTorsoRegion(
    xPercent: Float,
    yPercent: Float,
    isPosterior: Boolean,
): Pair<StringResource, String> =
    when {
        xPercent < 35f ->
            if (isPosterior) {
                Res.string.body_site_left_arm to "368208006"
            } else {
                Res.string.body_site_right_arm to "368209003"
            }
        xPercent > 65f ->
            if (isPosterior) {
                Res.string.body_site_right_arm to "368209003"
            } else {
                Res.string.body_site_left_arm to "368208006"
            }
        isPosterior -> Res.string.body_site_lower_back to "181534005"
        yPercent < 50f -> Res.string.body_site_abdomen to "818987002"
        else -> Res.string.body_site_pelvis to "12921003"
    }

/**
 * Resolves the leg region (left/right leg based on anterior/posterior).
 *
 * @param xPercent The horizontal percentage.
 * @param isPosterior Whether the view is posterior.
 * @return Pair of string resource and concept code.
 */
private fun resolveLegRegion(
    xPercent: Float,
    isPosterior: Boolean,
): Pair<StringResource, String> =
    if (xPercent <= 50f) {
        if (isPosterior) Res.string.body_site_left_leg to "368214008" else Res.string.body_site_right_leg to "368215009"
    } else {
        if (isPosterior) Res.string.body_site_right_leg to "368215009" else Res.string.body_site_left_leg to "368214008"
    }

/**
 * Resolves the anatomical region name and SNOMED concept based on relative coordinates and view.
 *
 * @param xPercent The horizontal percentage (0.0 to 100.0).
 * @param yPercent The vertical percentage (0.0 to 100.0).
 * @param isPosterior Whether the view is currently posterior (back) instead of anterior.
 * @return A Pair containing the localized [StringResource] and the SNOMED concept code.
 */
fun resolveAnatomicalRegion(
    xPercent: Float,
    yPercent: Float,
    isPosterior: Boolean,
): Pair<StringResource, String> =
    when {
        yPercent < 18f -> Res.string.body_site_head to "69536005"
        yPercent < 40f -> resolveUpperTorsoRegion(xPercent, isPosterior)
        yPercent < 60f -> resolveLowerTorsoRegion(xPercent, yPercent, isPosterior)
        else -> resolveLegRegion(xPercent, isPosterior)
    }

/**
 * Interactive anatomical body map pin-drop control.
 *
 * @param location The currently pinned location, or null.
 * @param onLocationChanged Callback invoked when a location is pinned or cleared.
 * @param label The localized title/label for the questionnaire item.
 * @param isRequired Whether an answer is required.
 * @param isError Whether this control is in an error state.
 * @param errorMessage Localized error message to announce when [isError] is true.
 * @param readOnly Whether the control is rendered in read-only / review mode.
 * @param modifier The modifier to apply to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMapPinDropControl(
    location: BodyMapLocation?,
    onLocationChanged: (BodyMapLocation?) -> Unit,
    label: String,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var isPosterior by remember { mutableStateOf(false) }
    val headText = stringResource(Res.string.body_site_head)
    val rightArmText = stringResource(Res.string.body_site_right_arm)
    val leftArmText = stringResource(Res.string.body_site_left_arm)
    val upperBackText = stringResource(Res.string.body_site_upper_back)
    val chestText = stringResource(Res.string.body_site_chest)
    val lowerBackText = stringResource(Res.string.body_site_lower_back)
    val abdomenText = stringResource(Res.string.body_site_abdomen)
    val pelvisText = stringResource(Res.string.body_site_pelvis)
    val rightLegText = stringResource(Res.string.body_site_right_leg)
    val leftLegText = stringResource(Res.string.body_site_left_leg)
    val anteriorText = stringResource(Res.string.body_map_view_anterior)
    val posteriorText = stringResource(Res.string.body_map_view_posterior)
    val canvasDesc = stringResource(Res.string.cd_body_map_canvas)
    val clearPinDesc = stringResource(Res.string.cd_body_map_clear)
    val siteViewFormat = stringResource(Res.string.body_map_site_view_format)

    val regionNameMap =
        remember(
            headText,
            rightArmText,
            leftArmText,
            upperBackText,
            chestText,
            lowerBackText,
            abdomenText,
            pelvisText,
            rightLegText,
            leftLegText,
        ) {
            mapOf(
                Res.string.body_site_head to headText,
                Res.string.body_site_right_arm to rightArmText,
                Res.string.body_site_left_arm to leftArmText,
                Res.string.body_site_upper_back to upperBackText,
                Res.string.body_site_chest to chestText,
                Res.string.body_site_lower_back to lowerBackText,
                Res.string.body_site_abdomen to abdomenText,
                Res.string.body_site_pelvis to pelvisText,
                Res.string.body_site_right_leg to rightLegText,
                Res.string.body_site_left_leg to leftLegText,
            )
        }

    val accessibleSites =
        remember(
            isPosterior,
            headText,
            rightArmText,
            leftArmText,
            upperBackText,
            chestText,
            lowerBackText,
            abdomenText,
            pelvisText,
            rightLegText,
            leftLegText,
        ) {
            if (isPosterior) {
                listOf(
                    AccessibleBodySite(headText, "69536005", 50f, 10f),
                    AccessibleBodySite(upperBackText, "181533004", 50f, 30f),
                    AccessibleBodySite(lowerBackText, "181534005", 50f, 48f),
                    AccessibleBodySite(leftArmText, "368208006", 28.5f, 38f),
                    AccessibleBodySite(rightArmText, "368209003", 71.5f, 38f),
                    AccessibleBodySite(leftLegText, "368214008", 43.5f, 75f),
                    AccessibleBodySite(rightLegText, "368215009", 56.5f, 75f),
                )
            } else {
                listOf(
                    AccessibleBodySite(headText, "69536005", 50f, 10f),
                    AccessibleBodySite(chestText, "51185008", 50f, 30f),
                    AccessibleBodySite(abdomenText, "818987002", 50f, 45f),
                    AccessibleBodySite(pelvisText, "12921003", 50f, 52f),
                    AccessibleBodySite(rightArmText, "368209003", 28.5f, 38f),
                    AccessibleBodySite(leftArmText, "368208006", 71.5f, 38f),
                    AccessibleBodySite(rightLegText, "368215009", 43.5f, 75f),
                    AccessibleBodySite(leftLegText, "368214008", 56.5f, 75f),
                )
            }
        }

    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.sm)
                .semantics {
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                        liveRegion = LiveRegionMode.Polite
                    }
                },
    ) {
        FormLabel(
            text = label,
            isRequired = isRequired,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        // View toggle chips (Anterior / Posterior)
        if (!readOnly) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !isPosterior,
                    onClick = { isPosterior = false },
                    label = { Text(stringResource(Res.string.body_map_view_anterior)) },
                    modifier = Modifier.testTag("BodyMapViewAnterior"),
                )
                FilterChip(
                    selected = isPosterior,
                    onClick = { isPosterior = true },
                    label = { Text(stringResource(Res.string.body_map_view_posterior)) },
                    modifier = Modifier.testTag("BodyMapViewPosterior"),
                )
            }

            // Accessible dropdown region selector for non-pointer / TalkBack navigation
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            ) {
                val currentDisplayName =
                    location?.let {
                        val (res, _) = resolveAnatomicalRegion(it.xPercent, it.yPercent, it.regionId == "posterior")
                        val site = regionNameMap[res] ?: it.displayName
                        val viewStr = if (it.regionId == "posterior") posteriorText else anteriorText
                        formatSiteView(siteViewFormat, site, viewStr)
                    } ?: ""

                OutlinedTextField(
                    value = currentDisplayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.body_map_accessible_select_region)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier =
                        Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("BodyMapAccessibleRegionSelector"),
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                ) {
                    accessibleSites.forEach { site ->
                        val viewSuffix = if (isPosterior) posteriorText else anteriorText
                        val formattedSiteName = formatSiteView(siteViewFormat, site.name, viewSuffix)
                        DropdownMenuItem(
                            text = { Text(site.name) },
                            onClick = {
                                dropdownExpanded = false
                                onLocationChanged(
                                    BodyMapLocation(
                                        regionId = if (isPosterior) "posterior" else "anterior",
                                        displayName = formattedSiteName,
                                        snomedCode = site.snomed,
                                        xPercent = site.xPercent,
                                        yPercent = site.yPercent,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }

        val viewSuffix = if (isPosterior) posteriorText else anteriorText
        val canvasActions =
            if (!readOnly) {
                accessibleSites.map { site ->
                    val actionName = formatSiteView(siteViewFormat, site.name, viewSuffix)
                    CustomAccessibilityAction(
                        label = actionName,
                        action = {
                            onLocationChanged(
                                BodyMapLocation(
                                    regionId = if (isPosterior) "posterior" else "anterior",
                                    displayName = actionName,
                                    snomedCode = site.snomed,
                                    xPercent = site.xPercent,
                                    yPercent = site.yPercent,
                                ),
                            )
                            true
                        },
                    )
                }
            } else {
                emptyList()
            }

        // Silhouette Canvas Card
        OutlinedCard(
            shape = MaterialTheme.shapes.medium,
            colors =
                CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(BODY_MAP_CANVAS_HEIGHT.dp)
                    .semantics {
                        contentDescription = canvasDesc
                        if (canvasActions.isNotEmpty()) {
                            customActions = canvasActions
                        }
                    },
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(BODY_MAP_CANVAS_HEIGHT.dp)) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outline
                val pinCenterColor = MaterialTheme.colorScheme.onPrimary

                Canvas(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .testTag("BodyMapCanvas")
                            .then(
                                if (!readOnly) {
                                    Modifier.pointerInput(isPosterior) {
                                        detectTapGestures { offset ->
                                            val xPct = (offset.x / size.width) * 100f
                                            val yPct = (offset.y / size.height) * 100f
                                            val (res, snomed) = resolveAnatomicalRegion(xPct, yPct, isPosterior)
                                            val siteName = regionNameMap[res] ?: headText
                                            val currentViewSuffix = if (isPosterior) posteriorText else anteriorText
                                            val newLocation =
                                                BodyMapLocation(
                                                    regionId = if (isPosterior) "posterior" else "anterior",
                                                    displayName =
                                                        formatSiteView(
                                                            siteViewFormat,
                                                            siteName,
                                                            currentViewSuffix,
                                                        ),
                                                    snomedCode = snomed,
                                                    xPercent = xPct.coerceIn(0f, 100f),
                                                    yPercent = yPct.coerceIn(0f, 100f),
                                                )
                                            onLocationChanged(newLocation)
                                        }
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f

                    // Draw stylized anatomical silhouette
                    val bodyColor = outlineColor.copy(alpha = 0.25f)
                    val strokeColor = outlineColor.copy(alpha = 0.6f)
                    val strokeStyle = Stroke(width = 2.dp.toPx())

                    // Head
                    drawCircle(
                        color = bodyColor,
                        radius = h * 0.07f,
                        center = Offset(cx, h * 0.10f),
                    )
                    drawCircle(
                        color = strokeColor,
                        radius = h * 0.07f,
                        center = Offset(cx, h * 0.10f),
                        style = strokeStyle,
                    )

                    // Torso
                    val torsoPath =
                        Path().apply {
                            moveTo(cx - w * 0.16f, h * 0.20f)
                            lineTo(cx + w * 0.16f, h * 0.20f)
                            lineTo(cx + w * 0.12f, h * 0.54f)
                            lineTo(cx - w * 0.12f, h * 0.54f)
                            close()
                        }
                    drawPath(path = torsoPath, color = bodyColor)
                    drawPath(path = torsoPath, color = strokeColor, style = strokeStyle)

                    // Left Arm
                    drawRoundRect(
                        color = bodyColor,
                        topLeft = Offset(cx + w * 0.18f, h * 0.22f),
                        size = Size(w * 0.07f, h * 0.32f),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(cx + w * 0.18f, h * 0.22f),
                        size = Size(w * 0.07f, h * 0.32f),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = strokeStyle,
                    )

                    // Right Arm
                    drawRoundRect(
                        color = bodyColor,
                        topLeft = Offset(cx - w * 0.25f, h * 0.22f),
                        size = Size(w * 0.07f, h * 0.32f),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(cx - w * 0.25f, h * 0.22f),
                        size = Size(w * 0.07f, h * 0.32f),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = strokeStyle,
                    )

                    // Left Leg
                    drawRoundRect(
                        color = bodyColor,
                        topLeft = Offset(cx + w * 0.02f, h * 0.56f),
                        size = Size(w * 0.09f, h * 0.38f),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(cx + w * 0.02f, h * 0.56f),
                        size = Size(w * 0.09f, h * 0.38f),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = strokeStyle,
                    )

                    // Right Leg
                    drawRoundRect(
                        color = bodyColor,
                        topLeft = Offset(cx - w * 0.11f, h * 0.56f),
                        size = Size(w * 0.09f, h * 0.38f),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(cx - w * 0.11f, h * 0.56f),
                        size = Size(w * 0.09f, h * 0.38f),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = strokeStyle,
                    )

                    // Pinned location indicator
                    if (location != null) {
                        val pinX = (location.xPercent / 100f) * w
                        val pinY = (location.yPercent / 100f) * h

                        // Pin shadow/ring
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = Offset(pinX, pinY),
                            style = Fill,
                        )
                        // Pin center
                        drawCircle(
                            color = primaryColor,
                            radius = 6.dp.toPx(),
                            center = Offset(pinX, pinY),
                            style = Fill,
                        )
                        drawCircle(
                            color = pinCenterColor,
                            radius = 2.dp.toPx(),
                            center = Offset(pinX, pinY),
                            style = Fill,
                        )
                    }
                }

                // Watermark instruction
                if (location == null && !readOnly) {
                    Text(
                        text = stringResource(Res.string.body_map_tap_instruction),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(AppSpacing.sm),
                    )
                }
            }
        }

        // Pin details readout and clear button
        if (location != null) {
            val (regionRes, _) =
                resolveAnatomicalRegion(
                    location.xPercent,
                    location.yPercent,
                    location.regionId == "posterior",
                )
            val site = regionNameMap[regionRes] ?: location.displayName
            val viewStr = if (location.regionId == "posterior") posteriorText else anteriorText
            val dynamicDisplayName = formatSiteView(siteViewFormat, site, viewStr)
            val pinSummary =
                stringResource(
                    Res.string.body_map_location_pinned,
                    dynamicDisplayName,
                    location.xPercent.toInt(),
                    location.yPercent.toInt(),
                )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = AppSpacing.sm)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f).padding(end = AppSpacing.sm),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = stringResource(Res.string.cd_body_map_pin, dynamicDisplayName),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = pinSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                if (!readOnly) {
                    OutlinedButton(
                        onClick = { onLocationChanged(null) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ClearBodyMapPinButton")
                                .semantics {
                                    contentDescription = clearPinDesc
                                },
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(AppSpacing.md).padding(end = AppSpacing.xs),
                        )
                        Text(stringResource(Res.string.body_map_clear_pin))
                    }
                }
            }
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Formats anatomical site and view orientation using localized template.
 *
 * @param format The localized format template string.
 * @param site The anatomical site name.
 * @param view The view orientation string.
 * @return The formatted label string.
 */
internal fun formatSiteView(
    format: String,
    site: String,
    view: String,
): String = format.replace("%1\$s", site).replace("%2\$s", view)
