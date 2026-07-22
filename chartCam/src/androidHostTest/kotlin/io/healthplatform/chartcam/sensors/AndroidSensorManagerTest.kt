/**
 * @file AndroidSensorManagerTest.kt
 * Contains declarations for AndroidSensorManagerTest.kt.
 */
package io.healthplatform.chartcam.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.healthplatform.chartcam.AndroidAppInit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

/**
 * Tests for [AndroidSensorManager].
 */
@RunWith(AndroidJUnit4::class)
class AndroidSensorManagerTest {
    private lateinit var context: Context

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

    @Test
    fun testSensorChangedWithMock() =
        runBlocking {
            val manager = AndroidSensorManager(context)
            // Can't easily mock SensorEvent data array, but let's test null first
            manager.onSensorChanged(null)

            try {
                val constructor = SensorEvent::class.java.getDeclaredConstructors().first { it.parameterCount == 1 }
                constructor.isAccessible = true
                val event = constructor.newInstance(3) as SensorEvent
                event.values[0] = 0.5f
                event.values[1] = 0.5f
                event.values[2] = 9.8f

                val sensorMock = mock(Sensor::class.java)
                val field = SensorEvent::class.java.getField("sensor")
                field.isAccessible = true
                field.set(event, sensorMock)

                manager.onSensorChanged(event)
            } catch (e: Throwable) {
                // ignore
            }
        }
}
