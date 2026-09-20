package io.healthplatform.chartcam

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

class TestTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCompose() =
        runComposeUiTest {
            setContent {
                Text("Hello")
            }
            onNodeWithText("Hello").assertExists()
        }

    @Test
    fun testFoo() {
        val date =
            dev.ohs.fhir.model.r4.Date
                .Builder()
                .apply {
                    value =
                        dev.ohs.fhir.model.r4.FhirDate
                            .fromString("2020-01-01")
                }.build()
        kotlin.test.assertEquals("2020-01-01", date.value?.toString())
    }
}
