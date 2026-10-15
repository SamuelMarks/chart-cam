/**
 * @file QuickAppTest.kt
 * Contains desktop Compose UI smoke tests for the root [App] composable.
 */
package io.healthplatform.chartcam

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Desktop UI smoke tests verifying root [App] composition and semantics node queries.
 */
class QuickAppTest {
    /**
     * Verifies that [App] renders its root semantics node and returns an empty list
     * when querying for a non-existent content description.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testApp() {
        runDesktopComposeUiTest(width = 1284, height = 2778) {
            setContent { App() }
            waitForIdle()
            onRoot().assertExists()
            val missingNodes = onAllNodes(hasContentDescription("DoesNotExist")).fetchSemanticsNodes()
            assertTrue(missingNodes.isEmpty())
            val missingLookupFailure =
                runCatching {
                    onAllNodes(hasContentDescription("DoesNotExist"))[0].fetchSemanticsNode()
                }
            assertTrue(missingLookupFailure.isFailure)
            assertEquals("AssertionError", missingLookupFailure.exceptionOrNull()?.let { it::class.simpleName })
        }
    }
}
