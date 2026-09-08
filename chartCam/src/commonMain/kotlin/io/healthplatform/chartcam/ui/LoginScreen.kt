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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import chartcam.chartcam.generated.resources.cd_demo_app_tour_button
import chartcam.chartcam.generated.resources.cd_demo_mode_button
import chartcam.chartcam.generated.resources.demo_app_tour_button
import chartcam.chartcam.generated.resources.demo_mode_button
import chartcam.chartcam.generated.resources.feature_capture
import chartcam.chartcam.generated.resources.feature_secure
import chartcam.chartcam.generated.resources.feature_sync
import chartcam.chartcam.generated.resources.legal_disclaimer
import chartcam.chartcam.generated.resources.login_signup
import chartcam.chartcam.generated.resources.logo
import chartcam.chartcam.generated.resources.offline_mode
import chartcam.chartcam.generated.resources.password
import chartcam.chartcam.generated.resources.state_unselected
import chartcam.chartcam.generated.resources.username
import io.healthplatform.chartcam.ui.components.LanguageMenu
import io.healthplatform.chartcam.ui.components.TraditionalChineseVerticalBanner
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Test tag for the one-click demo login button. */
const val TAG_DEMO_BUTTON = "login_demo_button"

/** Test tag for the workflow tutorial tour launcher button. */
const val TAG_TOUR_BUTTON = "login_tour_button"

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
    val currentLang by currentLanguageState.collectAsState()

    // Side effect check: if user is logged in, navigate
    if (state.isLoggedIn) {
        onLoginSuccess()
    }

    key(currentLang) {
        if (state.isTutorialVisible) {
            OnboardingTutorialScreen(
                onDismiss = { viewModel.showTutorial(false) },
                onComplete = { viewModel.showTutorial(false) },
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    LanguageMenu()
                }

                Spacer(modifier = Modifier.height(16.dp))
                LoginHeader()

                val stateErrorMessageStr = state.errorMessage?.let { stringResource(it) }
                LoginCard(
                    isLoading = state.isLoading,
                    isDemoLoading = state.isDemoLoading,
                    stateErrorMessage = stateErrorMessageStr,
                    onLogin = { username, password -> viewModel.login(username, password) },
                    onDemoLogin = { viewModel.onDemoLoginClicked() },
                    onOpenTour = { viewModel.showTutorial(true) },
                )

                Spacer(modifier = Modifier.height(48.dp))
                FeaturesRow()
            }
        }
    }
}

/**
 * Internal helper.
 */
@Composable
private fun LoginHeader() {
    val currentLang by currentLanguageState.collectAsState()
    var isVerticalMode by remember { mutableStateOf(true) }

    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null, // Decorative logo; app title is read out directly below
        modifier = Modifier.size(120.dp).padding(bottom = 16.dp),
    )

    if (isTraditionalChinese(currentLang)) {
        TraditionalChineseVerticalBanner(
            title = stringResource(Res.string.app_name_title),
            subtitle = stringResource(Res.string.app_slogan),
            onToggleMode = { isVerticalMode = !isVerticalMode },
            isVerticalMode = isVerticalMode,
            modifier = Modifier.padding(bottom = 16.dp).semantics { heading() },
        )
    } else {
        Text(
            text = stringResource(Res.string.app_name_title),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp).semantics { heading() },
        )

        Text(
            text = stringResource(Res.string.app_slogan),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp),
        )
    }
}

/**
 * Internal helper.
 * @param isLoading The isLoading.
 * @param isDemoLoading The isDemoLoading.
 * @param stateErrorMessage The stateErrorMessage.
 * @param onLogin The onLogin.
 * @param onDemoLogin Callback to trigger demo authentication.
 * @param onOpenTour Callback to open onboarding tutorial.
 */
@Composable
private fun LoginCard(
    isLoading: Boolean,
    isDemoLoading: Boolean = false,
    stateErrorMessage: String?,
    onLogin: (String, String) -> Unit,
    onDemoLogin: () -> Unit = {},
    onOpenTour: () -> Unit = {},
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val allFieldsRequiredMsg = stringResource(Res.string.all_fields_required)

    val attemptLogin = {
        focusManager.clearFocus()
        if (username.isNotBlank() && password.isNotBlank()) {
            onLogin(username, password)
        } else {
            formError = allFieldsRequiredMsg
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            UsernameField(
                username = username,
                onUsernameChange = {
                    username = it
                    formError = null
                },
                isLoading = isLoading,
                isError = formError != null,
                errorMessage = formError,
            )

            PasswordField(
                password = password,
                onPasswordChange = {
                    password = it
                    formError = null
                },
                isLoading = isLoading,
                isError = formError != null,
                onLogin = attemptLogin,
                errorMessage = formError,
            )

            OfflineModeSwitch()

            val displayError = stateErrorMessage ?: formError
            if (displayError != null) {
                ErrorMessage(displayError)
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            } else {
                LoginButton(onClick = attemptLogin)
            }

            Spacer(modifier = Modifier.height(12.dp))
            DemoLoginButton(
                isDemoLoading = isDemoLoading,
                onClick = onDemoLogin,
            )

            Spacer(modifier = Modifier.height(8.dp))
            AppTourButton(onClick = onOpenTour)

            Spacer(modifier = Modifier.height(16.dp))
            LegalDisclaimer()
        }
    }
}

/**
 * Renders an accessible secondary button for instant demo mode authentication.
 *
 * @param isDemoLoading Whether the demo session is currently loading.
 * @param onClick Callback triggered when the demo button is clicked.
 */
@Composable
private fun DemoLoginButton(
    isDemoLoading: Boolean,
    onClick: () -> Unit,
) {
    val cdDemo = stringResource(Res.string.cd_demo_mode_button)
    OutlinedButton(
        onClick = onClick,
        enabled = !isDemoLoading,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag(TAG_DEMO_BUTTON)
                .semantics {
                    contentDescription = cdDemo
                },
        shape = MaterialTheme.shapes.medium,
    ) {
        if (isDemoLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            text = stringResource(Res.string.demo_mode_button),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Renders a button allowing practitioners to launch the interactive workflow tutorial on demand.
 *
 * @param onClick Callback triggered when the tour button is clicked.
 */
@Composable
private fun AppTourButton(onClick: () -> Unit) {
    val cdTour = stringResource(Res.string.cd_demo_app_tour_button)
    TextButton(
        onClick = onClick,
        modifier =
            Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag(TAG_TOUR_BUTTON)
                .semantics {
                    contentDescription = cdTour
                    onClick(label = cdTour) {
                        onClick()
                        true
                    }
                },
    ) {
        Text(
            text = stringResource(Res.string.demo_app_tour_button),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Internal helper.
 */
@Composable
private fun LegalDisclaimer() {
    Text(
        text = stringResource(Res.string.legal_disclaimer),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Internal helper.
 * @param username The username.
 * @param onUsernameChange The onUsernameChange.
 * @param isLoading The isLoading.
 * @param isError The isError.
 * @param errorMessage The errorMessage.
 */
@Composable
private fun UsernameField(
    username: String,
    onUsernameChange: (String) -> Unit,
    isLoading: Boolean,
    isError: Boolean,
    errorMessage: String? = null,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text(stringResource(Res.string.username)) },
        supportingText = {
            if (isError && errorMessage != null) {
                Text(errorMessage)
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .semantics {
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }.onKeyEvent {
                    if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                        val dir = if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
                        focusManager.moveFocus(dir)
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
        isError = isError,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
    )
}

/**
 * Internal helper.
 * @param password The password.
 * @param onPasswordChange The onPasswordChange.
 * @param isLoading The isLoading.
 * @param isError The isError.
 * @param onLogin The onLogin.
 * @param errorMessage The errorMessage.
 */
@Composable
private fun PasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    isError: Boolean,
    onLogin: () -> Unit,
    errorMessage: String? = null,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(Res.string.password)) },
        supportingText = {
            if (isError && errorMessage != null) {
                Text(errorMessage)
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .semantics {
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }.onKeyEvent {
                    if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                        val dir = if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
                        focusManager.moveFocus(dir)
                        true
                    } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                        onLogin()
                        true
                    } else {
                        false
                    }
                },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        isError = isError,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onLogin() }),
        enabled = !isLoading,
    )
}

/**
 * Internal helper.
 */
@Composable
private fun OfflineModeSwitch() {
    val offlineModeLabel = stringResource(Res.string.offline_mode)
    val unselectedText = stringResource(Res.string.state_unselected)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = offlineModeLabel
                    role = Role.Switch
                    disabled()
                    stateDescription = unselectedText
                },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = offlineModeLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = false,
            onCheckedChange = null,
            enabled = false,
        )
    }
}

/**
 * Internal helper.
 * @param text The text.
 */
@Composable
private fun ErrorMessage(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                },
    )
}

/**
 * Internal helper.
 * @param onClick The onClick.
 */
@Composable
private fun LoginButton(onClick: () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalTextStyle provides
            androidx.compose.material3.LocalTextStyle.current
                .copy(fontWeight = FontWeight.Normal),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        ) {
            Text(
                text = stringResource(Res.string.login_signup),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
            )
        }
    }
}

/**
 * Internal helper.
 */
@Composable
private fun FeaturesRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        FeatureIcon(Icons.Default.CameraAlt, stringResource(Res.string.feature_capture))
        FeatureIcon(Icons.Default.Security, stringResource(Res.string.feature_secure))
        FeatureIcon(Icons.Default.CloudSync, stringResource(Res.string.feature_sync))
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
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
