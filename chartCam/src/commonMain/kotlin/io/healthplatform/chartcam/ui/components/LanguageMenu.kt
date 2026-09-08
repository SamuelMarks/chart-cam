/**
 * @file LanguageMenu.kt
 * Contains declarations for LanguageMenu.kt.
 *
 * Provides a unified accessible language selection dropdown menu for the application.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_switch_language
import chartcam.chartcam.generated.resources.english
import chartcam.chartcam.generated.resources.espanol
import chartcam.chartcam.generated.resources.hebrew
import chartcam.chartcam.generated.resources.japanese
import chartcam.chartcam.generated.resources.traditional_chinese
import io.healthplatform.chartcam.ui.setAppLanguage
import org.jetbrains.compose.resources.stringResource

/** Test tag identifying the language switcher menu anchor. */
const val TAG_LANGUAGE_MENU_BUTTON = "language_menu_button"

/**
 * A composable providing a dropdown menu to select the active application language.
 *
 * @param modifier The modifier to apply to the container box.
 */
@Composable
fun LanguageMenu(modifier: Modifier = Modifier) {
    var showLanguageMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { showLanguageMenu = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag(TAG_LANGUAGE_MENU_BUTTON),
        ) {
            Icon(
                Icons.Default.Translate,
                contentDescription = stringResource(Res.string.cd_switch_language),
            )
        }
        DropdownMenu(
            expanded = showLanguageMenu,
            onDismissRequest = { showLanguageMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.english)) },
                onClick = {
                    setAppLanguage("en")
                    showLanguageMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.espanol)) },
                onClick = {
                    setAppLanguage("es")
                    showLanguageMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.japanese)) },
                onClick = {
                    setAppLanguage("ja")
                    showLanguageMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.hebrew)) },
                onClick = {
                    setAppLanguage("he")
                    showLanguageMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.traditional_chinese)) },
                onClick = {
                    setAppLanguage("zh")
                    showLanguageMenu = false
                },
            )
        }
    }
}
