/**
 * @file DemoModeBanner.kt
 * Contains declarations for DemoModeBanner.kt.
 *
 * Persistent high-contrast clinical safety banner indicating demo mode is active.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_demo_mode_exit
import chartcam.chartcam.generated.resources.demo_mode_banner_a11y
import chartcam.chartcam.generated.resources.demo_mode_banner_desc
import chartcam.chartcam.generated.resources.demo_mode_banner_title
import chartcam.chartcam.generated.resources.demo_mode_exit
import io.healthplatform.chartcam.ui.currentLanguageState
import io.healthplatform.chartcam.ui.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

/** Test tag for the root demo mode banner container. */
const val TAG_DEMO_BANNER = "demo_mode_banner"

/** Test tag for the exit demo action button. */
const val TAG_EXIT_DEMO_BUTTON = "exit_demo_button"

/**
 * High-contrast clinical safety banner displayed across patient and encounter screens
 * when the application is running in demonstration mode.
 *
 * Clearly alerts healthcare providers that sample data is loaded and that real protected
 * health information (PHI) must not be entered.
 *
 * @param onExitDemo Callback invoked when the user elects to terminate demo mode and log out.
 * @param modifier Optional modifier applied to the banner root container.
 */
@Composable
fun DemoModeBanner(
    onExitDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentLang by currentLanguageState.collectAsState()
    key(currentLang) {
        val titleText = stringResource(Res.string.demo_mode_banner_title)
        val descText = stringResource(Res.string.demo_mode_banner_desc)
        val cdExit = stringResource(Res.string.cd_demo_mode_exit)
        val bannerA11yText =
            stringResource(Res.string.demo_mode_banner_a11y, titleText, descText)

        Surface(
            modifier =
                modifier
                    .fillMaxWidth()
                    .testTag(TAG_DEMO_BANNER)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = bannerA11yText
                    },
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            tonalElevation = AppSpacing.xs,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(AppSpacing.lg),
                    )
                    Spacer(modifier = Modifier.size(AppSpacing.moderate))
                    Column {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = descText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.size(AppSpacing.sm))

                OutlinedButton(
                    onClick = onExitDemo,
                    modifier =
                        Modifier
                            .defaultMinSize(minWidth = AppSpacing.minTouchTarget, minHeight = AppSpacing.minTouchTarget)
                            .testTag(TAG_EXIT_DEMO_BUTTON)
                            .semantics {
                                contentDescription = cdExit
                                onClick(label = cdExit) {
                                    onExitDemo()
                                    true
                                }
                            },
                    shape = MaterialTheme.shapes.small,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                ) {
                    Text(
                        text = stringResource(Res.string.demo_mode_exit),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
