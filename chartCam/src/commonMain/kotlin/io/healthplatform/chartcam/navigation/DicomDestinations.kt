/**
 * @file DicomDestinations.kt
 * Registers navigation destinations for DICOM file inspection and viewing.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_back
import chartcam.chartcam.generated.resources.error_dicom_parse_failed_format
import chartcam.chartcam.generated.resources.loading
import chartcam.chartcam.generated.resources.title_dicom_inspector
import io.healthplatform.chartcam.dicom.DicomDataset
import io.healthplatform.chartcam.dicom.DicomReader
import io.healthplatform.chartcam.ui.components.DicomViewerComponent
import org.jetbrains.compose.resources.stringResource

/**
 * Registers the DICOM viewer destination to the navigation graph.
 *
 * @param navController The navigation controller.
 * @param deps The application dependencies.
 */
fun NavGraphBuilder.dicomViewerDestination(
    navController: NavHostController,
    deps: AppDependencies,
) {
    composable<DicomViewerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<DicomViewerRoute>()
        DicomViewerScreen(
            filePath = route.filePath,
            deps = deps,
            onBack = { navController.popBackStack() },
        )
    }
}

/**
 * Screen presenting local DICOM dataset inspection and image/PDF viewing.
 *
 * **State & Side Effects:**
 * Asynchronously loads local DICOM byte payload from file storage and decodes via [DicomReader].
 *
 * @param filePath Path to the DICOM file.
 * @param deps Application dependencies.
 * @param onBack Callback when back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DicomViewerScreen(
    filePath: String,
    deps: AppDependencies,
    onBack: () -> Unit,
) {
    var datasetResult by remember { mutableStateOf<Result<DicomDataset>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val loadingText = stringResource(Res.string.loading)

    LaunchedEffect(filePath) {
        val storage =
            deps.fileStorage ?: io.healthplatform.chartcam.files
                .createFileStorage()
        val bytes = storage.readImage(filePath)
        datasetResult = DicomReader.read(bytes)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.title_dicom_inspector),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            val result = datasetResult
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.semantics {
                                contentDescription = loadingText
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                }
                result != null && result.isSuccess -> {
                    val dataset = result.getOrNull()
                    if (dataset != null) {
                        DicomViewerComponent(dataset = dataset)
                    } else {
                        Text(
                            text = stringResource(Res.string.error_dicom_parse_failed_format, filePath),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                else -> {
                    Text(
                        text = stringResource(Res.string.error_dicom_parse_failed_format, filePath),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }
}
