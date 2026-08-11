/**
 * @file PlatformTestUtils.kt
 * Contains declarations for PlatformTestUtils.kt.
 */
package io.healthplatform.chartcam

/**
 * Cleans up the test environment.
 * Implementation is expected to be provided by each target platform.
 */
expect fun cleanupTestEnv()
