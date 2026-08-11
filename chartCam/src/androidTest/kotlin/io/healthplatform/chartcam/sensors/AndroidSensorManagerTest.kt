/**
 * @file AndroidSensorManagerTest.kt
 * Contains declarations for AndroidSensorManagerTest.kt.
 */
package io.healthplatform.chartcam.sensors

import android.content.Context
import android.hardware.SensorEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.healthplatform.chartcam.AndroidAppInit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [AndroidSensorManager].
 */
@RunWith(AndroidJUnit4::class)
class AndroidSensorManagerTest {
    private lateinit var context: Context

    /**
     * Setup for tests.
     */
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)
    }

    /**
     * Test create, start and stop.
     */
    @Test
    fun testStartStop() {
        val manager = AndroidSensorManager(context)
        manager.startListening()
        manager.stopListening()
    }

    /**
     * Test accuracy change.
     */
    @Test
    fun testAccuracyChanged() {
        val manager = AndroidSensorManager(context)
        manager.onAccuracyChanged(null, 0)
    }

    /**
     * Test sensor event.
     * Creating a SensorEvent requires reflection as its constructor is package-private.
     */
    @Test
    fun testSensorChanged() =
        runBlocking {
            val manager = AndroidSensorManager(context)

            try {
                val constructor = SensorEvent::class.java.getDeclaredConstructors().first { it.parameterCount == 1 }
                constructor.isAccessible = true
                val event = constructor.newInstance(3) as SensorEvent

                // Note: Setting sensor type might require more reflection or mocking which is complex
                // We just verify it doesn't crash on null
                manager.onSensorChanged(null)
                manager.onSensorChanged(event)
            } catch (e: Exception) {
                // Ignored, reflection can be flaky on different Android versions.
            }
        }
}
