package io.healthplatform.chartcam.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FormLabel(
    text: String,
    isRequired: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val display = if (isRequired) "$text *" else text
    Text(text = display, modifier = modifier)
}
