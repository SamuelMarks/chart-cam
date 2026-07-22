/**
 * @file LoginScreen.kt
 * Contains declarations for LoginScreen.kt.
 *
 * Login Screen UI definition.
 * Provides the user interface for practitioner authentication.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.all_fields_required
import chartcam.chartcam.generated.resources.app_name_title
import chartcam.chartcam.generated.resources.app_slogan
import chartcam.chartcam.generated.resources.cd_switch_language
import chartcam.chartcam.generated.resources.english
import chartcam.chartcam.generated.resources.espanol
import chartcam.chartcam.generated.resources.feature_capture
import chartcam.chartcam.generated.resources.feature_secure
import chartcam.chartcam.generated.resources.feature_sync
import chartcam.chartcam.generated.resources.japanese
import chartcam.chartcam.generated.resources.login_signup
import chartcam.chartcam.generated.resources.logo
import chartcam.chartcam.generated.resources.offline_mode
import chartcam.chartcam.generated.resources.password
import chartcam.chartcam.generated.resources.username
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Screen enabling Practitioner authentication.
 * Displays the app branding, handles login credentials input,
 * and allows language switching.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param viewModel The ViewModel handling login business logic.
 * @param onLoginSuccess Callback triggered when the authentication is successful.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Side effect check: if user is logged in, navigate
    if (state.isLoggedIn) {
        onLoginSuccess()
    }

    val currentLanguage by currentLanguageState.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Box {
                var showLanguageMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showLanguageMenu = true }) {
                    Icon(Icons.Default.Translate, contentDescription = stringResource(Res.string.cd_switch_language))
                }
                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.english)) },
                        onClick = {
                            // setAppLanguage("en")
                            showLanguageMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.espanol)) },
                        onClick = {
                            // setAppLanguage("es")
                            showLanguageMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.japanese)) },
                        onClick = {
                            // setAppLanguage("ja")
                            showLanguageMenu = false
                        },
                    )
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null, // Decorative logo; app title is read out directly below
                modifier = Modifier.size(120.dp).padding(bottom = 16.dp),
            )

            // Title
            Text(
                text = stringResource(Res.string.app_name_title),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Slogan
            Text(
                text = stringResource(Res.string.app_slogan),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    var username by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var formError by remember { mutableStateOf<String?>(null) }
                    val allFieldsRequiredMsg = stringResource(Res.string.all_fields_required)
                    val isLoading = state.isLoading

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            formError = null
                        },
                        label = { Text(stringResource(Res.string.username)) },
                        modifier =
                            Modifier.fillMaxWidth().padding(bottom = 16.dp).onKeyEvent {
                                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                    focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                    true
                                } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                    focusManager.moveFocus(FocusDirection.Next)
                                    true
                                } else {
                                    false
                                }
                            },
                        singleLine = true,
                        enabled = !isLoading,
                        isError = formError != null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            formError = null
                        },
                        label = { Text(stringResource(Res.string.password)) },
                        modifier =
                            Modifier.fillMaxWidth().padding(bottom = 24.dp).onKeyEvent {
                                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                    focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                    true
                                } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                    focusManager.clearFocus()
                                    if (username.isNotBlank() && password.isNotBlank()) {
                                        viewModel.login(username, password)
                                    } else {
                                        formError = allFieldsRequiredMsg
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = formError != null,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                if (username.isNotBlank() && password.isNotBlank()) {
                                    viewModel.login(username, password)
                                } else {
                                    formError = allFieldsRequiredMsg
                                }
                            }),
                        enabled = !isLoading,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(Res.string.offline_mode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = false,
                            onCheckedChange = null,
                            enabled = false,
                        )
                    }

                    val stateErrorMessageStr = state.errorMessage?.let { stringResource(it) }
                    val displayError = stateErrorMessageStr ?: formError
                    if (displayError != null) {
                        Text(
                            text = displayError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(),
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                    } else {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalTextStyle provides
                                androidx.compose.material3.LocalTextStyle.current
                                    .copy(fontWeight = FontWeight.Normal),
                        ) {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (username.isNotBlank() && password.isNotBlank()) {
                                        viewModel.login(username, password)
                                    } else {
                                        formError = allFieldsRequiredMsg
                                    }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = androidx.compose.ui.graphics.Color.White,
                                    ),
                            ) {
                                Text(
                                    text = stringResource(Res.string.login_signup),
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Features Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                FeatureIcon(Icons.Default.CameraAlt, stringResource(Res.string.feature_capture))
                FeatureIcon(Icons.Default.Security, stringResource(Res.string.feature_secure))
                FeatureIcon(Icons.Default.CloudSync, stringResource(Res.string.feature_sync))
            }
        }
    }
}

/**
 * Renders a small feature highlight icon and label.
 *
 * **State & Side Effects:**
 * Manages internal UI state or propagates hoisted state. `Modifier` behaviors (if any) are applied to the root element.
 *
 * @param icon The icon to display.
 * @param label The text describing the feature.
 */
@Composable
fun FeatureIcon(
    icon: ImageVector,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp).padding(bottom = 4.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
