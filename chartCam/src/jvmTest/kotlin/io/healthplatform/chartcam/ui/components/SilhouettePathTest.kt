/**
 * @file SilhouettePathTest.kt
 * Contains tests for Silhouette paths and vector mathematics.
 */
package io.healthplatform.chartcam.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Unit tests verifying vector path generation for profile and frontal silhouettes.
 */
class SilhouettePathTest {
    @Test
    fun testBuildProfileFacePath() {
        val pathLeft = buildProfileFacePath(width = 800f, height = 1200f, isMirrored = false)
        assertNotNull(pathLeft)
        assertFalse(pathLeft.isEmpty)

        val pathRight = buildProfileFacePath(width = 800f, height = 1200f, isMirrored = true)
        assertNotNull(pathRight)
        assertFalse(pathRight.isEmpty)
    }

    @Test
    fun testBuildCorneaBracketPath() {
        val corneaLeft = buildCorneaBracketPath(width = 800f, height = 1200f, isMirrored = false)
        assertNotNull(corneaLeft)
        assertFalse(corneaLeft.isEmpty)

        val corneaRight = buildCorneaBracketPath(width = 800f, height = 1200f, isMirrored = true)
        assertNotNull(corneaRight)
        assertFalse(corneaRight.isEmpty)
    }

    @Test
    fun testBuildNoseBoxPath() {
        val noseLeft = buildNoseBoxPath(width = 800f, height = 1200f, isMirrored = false)
        assertNotNull(noseLeft)
        assertFalse(noseLeft.isEmpty)

        val noseRight = buildNoseBoxPath(width = 800f, height = 1200f, isMirrored = true)
        assertNotNull(noseRight)
        assertFalse(noseRight.isEmpty)
    }

    @Test
    fun testBuildFrankfortLinePath() {
        val lineLeft = buildFrankfortLinePath(width = 800f, height = 1200f, isMirrored = false)
        assertNotNull(lineLeft)
        assertFalse(lineLeft.isEmpty)

        val lineRight = buildFrankfortLinePath(width = 800f, height = 1200f, isMirrored = true)
        assertNotNull(lineRight)
        assertFalse(lineRight.isEmpty)
    }

    @Test
    fun testBuildFrontalSilhouettePaths() {
        val oval = buildFrontalFaceOvalPath(width = 800f, height = 1200f)
        assertNotNull(oval)
        assertFalse(oval.isEmpty)

        val eyes = buildBipupillaryLinePath(width = 800f, height = 1200f)
        assertNotNull(eyes)
        assertFalse(eyes.isEmpty)

        val midline = buildFacialMidlinePath(width = 800f, height = 1200f)
        assertNotNull(midline)
        assertFalse(midline.isEmpty)

        val guides = buildNoseAndMouthGuidesPath(width = 800f, height = 1200f)
        assertNotNull(guides)
        assertFalse(guides.isEmpty)
    }
}
