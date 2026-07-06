package io.healthplatform.chartcam.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class SensorManagerAndroidTest {
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
}
