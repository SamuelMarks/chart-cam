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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_back
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
 * @param filePath Path to the DICOM file.
 * @param deps Application dependencies.
 * @param onBack Callback when back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DicomViewerScreen(
    filePath: String,
    deps: AppDependencies,
    onBack: () -> Unit,
) {
    var dataset by remember { mutableStateOf<DicomDataset?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(filePath) {
        val storage =
            deps.fileStorage ?: io.healthplatform.chartcam.files
                .createFileStorage()
        val bytes = storage.readImage(filePath)
        dataset = DicomReader.read(bytes).getOrNull()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DICOM Inspector") },
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
            val currentData = dataset
            when {
                isLoading -> CircularProgressIndicator()
                currentData != null -> DicomViewerComponent(dataset = currentData)
                else -> Text("Failed to parse DICOM file: $filePath")
            }
        }
    }
}
