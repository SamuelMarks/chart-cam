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
        // Test default parameter
        val pathDefault = buildProfileFacePath(width = 800f, height = 1200f)
        assertNotNull(pathDefault)
        assertFalse(pathDefault.isEmpty)

        val pathLeft = buildProfileFacePath(width = 800f, height = 1200f, isMirrored = false)
        assertNotNull(pathLeft)
        assertFalse(pathLeft.isEmpty)

        val pathRight = buildProfileFacePath(width = 800f, height = 1200f, isMirrored = true)
        assertNotNull(pathRight)
        assertFalse(pathRight.isEmpty)

        // Test alternate aspect ratio (landscape) and zero boundary
        val pathLandscape = buildProfileFacePath(width = 1920f, height = 1080f, isMirrored = false)
        assertNotNull(pathLandscape)
        val pathZero = buildProfileFacePath(width = 0f, height = 0f, isMirrored = false)
        assertNotNull(pathZero)
    }

    @Test
    fun testBuildCorneaBracketPath() {
        // Test default parameter
        val corneaDefault = buildCorneaBracketPath(width = 800f, height = 1200f)
        assertNotNull(corneaDefault)
        assertFalse(corneaDefault.isEmpty)

        val corneaLeft = buildCorneaBracketPath(width = 800f, height = 1200f, isMirrored = false)
        assertNotNull(corneaLeft)
        assertFalse(corneaLeft.isEmpty)

        val corneaRight = buildCorneaBracketPath(width = 800f, height = 1200f, isMirrored = true)
        assertNotNull(corneaRight)
        assertFalse(corneaRight.isEmpty)

        val corneaLandscape = buildCorneaBracketPath(width = 1920f, height = 1080f, isMirrored = true)
        assertNotNull(corneaLandscape)
        val corneaZero = buildCorneaBracketPath(width = 0f, height = 0f, isMirrored = false)
        assertNotNull(corneaZero)
    }

    @Test
    fun testBuildNoseBoxPath() {
        // Test default parameter
        val noseDefault = buildNoseBoxPath(width = 800f, height = 1200f)
        assertNotNull(noseDefault)
        assertFalse(noseDefault.isEmpty)

        val noseLeft = buildNoseBoxPath(width = 800f, height = 1200f, isMirrored = false)
        assertNotNull(noseLeft)
        assertFalse(noseLeft.isEmpty)

        val noseRight = buildNoseBoxPath(width = 800f, height = 1200f, isMirrored = true)
        assertNotNull(noseRight)
        assertFalse(noseRight.isEmpty)

        val noseLandscape = buildNoseBoxPath(width = 1920f, height = 1080f, isMirrored = true)
        assertNotNull(noseLandscape)
        val noseZero = buildNoseBoxPath(width = 0f, height = 0f, isMirrored = false)
        assertNotNull(noseZero)
    }

    @Test
    fun testBuildFrankfortLinePath() {
        // Test default parameter
        val lineDefault = buildFrankfortLinePath(width = 800f, height = 1200f)
        assertNotNull(lineDefault)
        assertFalse(lineDefault.isEmpty)

        val lineLeft = buildFrankfortLinePath(width = 800f, height = 1200f, isMirrored = false)
        assertNotNull(lineLeft)
        assertFalse(lineLeft.isEmpty)

        val lineRight = buildFrankfortLinePath(width = 800f, height = 1200f, isMirrored = true)
        assertNotNull(lineRight)
        assertFalse(lineRight.isEmpty)

        val lineLandscape = buildFrankfortLinePath(width = 1920f, height = 1080f, isMirrored = false)
        assertNotNull(lineLandscape)
        val lineZero = buildFrankfortLinePath(width = 0f, height = 0f, isMirrored = false)
        assertNotNull(lineZero)
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

        // Test boundary conditions and varied dimensions
        assertNotNull(buildFrontalFaceOvalPath(width = 1920f, height = 1080f))
        assertNotNull(buildFrontalFaceOvalPath(width = 0f, height = 0f))
        assertNotNull(buildBipupillaryLinePath(width = 1920f, height = 1080f))
        assertNotNull(buildBipupillaryLinePath(width = 0f, height = 0f))
        assertNotNull(buildFacialMidlinePath(width = 1920f, height = 1080f))
        assertNotNull(buildFacialMidlinePath(width = 0f, height = 0f))
        assertNotNull(buildNoseAndMouthGuidesPath(width = 1920f, height = 1080f))
        assertNotNull(buildNoseAndMouthGuidesPath(width = 0f, height = 0f))
    }
}
