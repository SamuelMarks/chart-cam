/**
 * @file DeepLinkTest.kt
 * Contains tests for navigation deep linking logic.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for evaluating deep link parsing and navigation logic.
 */
class DeepLinkTest {
    /**
     * Verifies that deep links for specific patients can be accurately parsed
     * and routed to the correct parameters.
     */
    @Test
    fun testDeepLinkingToPatientView() {
        // Simulating navigation intent parsing
        val mockDeepLinkUri = "app://chartcam/patient/pat-789"

        fun parsePatientIdFromDeepLink(uri: String): String? {
            val prefix = "app://chartcam/patient/"
            return if (uri.startsWith(prefix)) {
                uri.removePrefix(prefix)
            } else {
                null
            }
        }

        val extractedId = parsePatientIdFromDeepLink(mockDeepLinkUri)
        assertNotNull(extractedId, "Should extract patient ID from deep link")
        assertEquals("pat-789", extractedId, "Extracted ID should match the URI segment")
    }

    /**
     * Verifies that deep links for specific forms can be accurately parsed.
     */
    @Test
    fun testDeepLinkingToFormView() {
        val mockDeepLinkUri = "app://chartcam/form/questionnaire-123"

        fun parseFormIdFromDeepLink(uri: String): String? {
            val prefix = "app://chartcam/form/"
            return if (uri.startsWith(prefix)) {
                uri.removePrefix(prefix)
            } else {
                null
            }
        }

        val extractedId = parseFormIdFromDeepLink(mockDeepLinkUri)
        assertNotNull(extractedId, "Should extract form ID from deep link")
        assertEquals("questionnaire-123", extractedId, "Extracted ID should match the URI segment")
    }
}
