/**
 * @file LanguageMenu.kt
 * Contains declarations for LanguageMenu.kt.
 *
 * Provides a unified accessible language selection dropdown menu for the application.
 */
package io.healthplatform.chartcam.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_switch_language
import chartcam.chartcam.generated.resources.english
import chartcam.chartcam.generated.resources.espanol
import chartcam.chartcam.generated.resources.hebrew
import chartcam.chartcam.generated.resources.japanese
import chartcam.chartcam.generated.resources.state_selected
import chartcam.chartcam.generated.resources.state_unselected
import chartcam.chartcam.generated.resources.traditional_chinese
import io.healthplatform.chartcam.ui.currentLanguageState
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
    val currentLang by currentLanguageState.collectAsState()

    val languages =
        listOf(
            "en" to Res.string.english,
            "es" to Res.string.espanol,
            "ja" to Res.string.japanese,
            "he" to Res.string.hebrew,
            "zh" to Res.string.traditional_chinese,
        )

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
            languages.forEach { (code, res) ->
                val isSelected = currentLang.lowercase().startsWith(code)
                val stateDesc =
                    stringResource(
                        if (isSelected) Res.string.state_selected else Res.string.state_unselected,
                    )
                DropdownMenuItem(
                    text = { Text(stringResource(res)) },
                    trailingIcon =
                        if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                    modifier =
                        Modifier.semantics {
                            role = Role.RadioButton
                            selected = isSelected
                            stateDescription = stateDesc
                        },
                    onClick = {
                        setAppLanguage(code)
                        showLanguageMenu = false
                    },
                )
            }
        }
    }
}
