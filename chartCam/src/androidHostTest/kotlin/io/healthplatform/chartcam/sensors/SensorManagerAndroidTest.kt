/**
 * @file SensorManagerAndroidTest.kt
 * Contains declarations for SensorManagerAndroidTest.kt.
 */
package io.healthplatform.chartcam.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.test.core.app.ApplicationProvider
import io.healthplatform.chartcam.AndroidAppInit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Android host tests for SensorManager.
 */
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SensorManagerAndroidTest {
    /**
     * Test AndroidSensorManager flow and calculations.
     */
    @Test
    fun testAndroidSensorManager() =
        runBlocking {
            val mockContext = Mockito.mock(Context::class.java)
            val mockSensorManager = Mockito.mock(android.hardware.SensorManager::class.java)
            val mockSensor = Mockito.mock(Sensor::class.java)

            Mockito.`when`(mockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mockSensorManager)
            Mockito.`when`(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor)

            val androidSensorManager = AndroidSensorManager(mockContext)

            androidSensorManager.startListening()
            Mockito.verify(mockSensorManager).registerListener(
                androidSensorManager as SensorEventListener,
                mockSensor,
                android.hardware.SensorManager.SENSOR_DELAY_UI,
            )

            // Test onAccuracyChanged
            androidSensorManager.onAccuracyChanged(mockSensor, 1) // Should not throw

            // Test onSensorChanged using reflection to create SensorEvent
            val constructor = SensorEvent::class.java.getDeclaredConstructors().first { it.parameterCount == 1 }
            constructor.isAccessible = true
            val sensorEvent = constructor.newInstance(3) as SensorEvent

            val sensorField = SensorEvent::class.java.getField("sensor")
            sensorField.isAccessible = true
            sensorField.set(sensorEvent, mockSensor)

            Mockito.`when`(mockSensor.type).thenReturn(Sensor.TYPE_ACCELEROMETER)

            sensorEvent.values[0] = 0f
            sensorEvent.values[1] = 9.8f
            sensorEvent.values[2] = 9.8f

            androidSensorManager.onSensorChanged(sensorEvent)

            val orientation = androidSensorManager.orientation.first()
            // y=9.8, z=9.8 => roll = atan2(0, ...) = 0
            assertEquals(0.0, orientation.roll, 0.1)
            // pitch = atan2(9.8, 9.8) = 45 degrees
            assertEquals(45.0, orientation.pitch, 0.1)

            androidSensorManager.stopListening()
            Mockito.verify(mockSensorManager).unregisterListener(androidSensorManager as SensorEventListener)
        }

    /**
     * Tests AndroidSensorManager when sensor event is null or non-accelerometer.
     */
    @Test
    fun testAndroidSensorManagerNullEventAndNonAccelerometer() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockSensorManager = Mockito.mock(android.hardware.SensorManager::class.java)
        Mockito.`when`(mockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mockSensorManager)

        val androidSensorManager = AndroidSensorManager(mockContext)
        androidSensorManager.onSensorChanged(null)

        val mockGyro = Mockito.mock(Sensor::class.java)
        Mockito.`when`(mockGyro.type).thenReturn(Sensor.TYPE_GYROSCOPE)

        val constructor = SensorEvent::class.java.getDeclaredConstructors().first { it.parameterCount == 1 }
        constructor.isAccessible = true
        val sensorEvent = constructor.newInstance(3) as SensorEvent
        val sensorField = SensorEvent::class.java.getField("sensor")
        sensorField.isAccessible = true
        sensorField.set(sensorEvent, mockGyro)

        androidSensorManager.onSensorChanged(sensorEvent)
    }

    /**
     * Tests AndroidSensorManager when default accelerometer sensor is null.
     */
    @Test
    fun testAndroidSensorManagerNullAccelerometer() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockSensorManager = Mockito.mock(android.hardware.SensorManager::class.java)
        Mockito.`when`(mockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mockSensorManager)
        Mockito.`when`(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(null)

        val androidSensorManager = AndroidSensorManager(mockContext)
        androidSensorManager.startListening()
        Mockito.verify(mockSensorManager, Mockito.never()).registerListener(
            Mockito.any(SensorEventListener::class.java),
            Mockito.any(Sensor::class.java),
            Mockito.anyInt(),
        )
    }

    /**
     * Tests rememberSensorManager composable lifecycle integration via headless Compose runtime.
     */
    @Test
    fun testRememberSensorManagerComposable() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            AndroidAppInit.init(context)

            val applier =
                object : androidx.compose.runtime.AbstractApplier<Unit>(Unit) {
                    override fun insertTopDown(index: Int, instance: Unit) {}

                    override fun insertBottomUp(index: Int, instance: Unit) {}

                    override fun remove(index: Int, count: Int) {}

                    override fun move(from: Int, to: Int, count: Int) {}

                    override fun onClear() {}
                }
            val recomposer = androidx.compose.runtime.Recomposer(kotlinx.coroutines.Dispatchers.Unconfined)
            val composition = androidx.compose.runtime.Composition(applier, recomposer)
            // allow-exception
            try {
                composition.setContent {
                    val manager = rememberSensorManager()
                    assertNotNull(manager)
                }
            } finally {
                composition.dispose()
            }
        }
}
