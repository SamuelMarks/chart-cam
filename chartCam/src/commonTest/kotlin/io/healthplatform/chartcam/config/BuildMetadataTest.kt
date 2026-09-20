/**
 * @file BuildMetadataTest.kt
 * Contains declarations for BuildMetadataTest.kt.
 */
package io.healthplatform.chartcam.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests verifying dynamic build metadata configuration and version formatting.
 */
class BuildMetadataTest {
    /**
     * Verifies that BuildMetadata contains valid version and website URL constants and formats properly.
     */
    @Test
    fun testBuildMetadataValues() {
        assertEquals("1.0.6", BuildMetadata.VERSION_NAME)
        assertEquals("https://healthplatform.io", BuildMetadata.WEBSITE_URL)

        val formatted = BuildMetadata.formattedVersionInfo()
        assertTrue(formatted.startsWith(BuildMetadata.VERSION_NAME))
        assertTrue(formatted.endsWith(BuildMetadata.WEBSITE_URL))
        assertEquals("1.0.6 — https://healthplatform.io", formatted)
    }
}
