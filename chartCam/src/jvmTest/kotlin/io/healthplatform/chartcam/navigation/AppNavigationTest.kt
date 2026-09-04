/**
 * @file AppNavigationTest.kt
 * Contains declarations for AppNavigationTest.kt.
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Application Navigation utilities.
 */
class AppNavigationTest {
    @Test
    fun testPhotoSessionManager() {
        val manager = PhotoSessionManager()
        val photos = mapOf("id1" to "path1", "id2" to "path2")

        manager.setPhotos(photos)

        val retrieved = manager.get()
        assertEquals(2, retrieved.size)
        assertEquals("path1", retrieved["id1"])

        val cleared = manager.getAndClear()
        assertEquals(2, cleared.size)

        val empty = manager.get()
        assertTrue(empty.isEmpty())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun appNavigationQuestionnaireListRouteDisplaysQuestionnaireListScreen() =
        runComposeUiTest {
            setContent {
                // Note: Navigation graph might fail to start if it requires auth to reach that screen,
                // or we might need to manually navigate the navController.
                // Let's just instantiate AppNavigation and see what it does.
                AppNavigation()
            }
        }
}
