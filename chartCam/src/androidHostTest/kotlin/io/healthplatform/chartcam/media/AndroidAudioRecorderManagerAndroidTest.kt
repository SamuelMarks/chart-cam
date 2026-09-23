/**
 * @file AndroidAudioRecorderManagerAndroidTest.kt
 * Contains declarations for AndroidAudioRecorderManagerAndroidTest.kt.
 */
package io.healthplatform.chartcam.media

import android.os.Build
import io.healthplatform.chartcam.AndroidAppInit
import io.healthplatform.chartcam.files.createFileStorage
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for AndroidAudioRecorderManager on Android host.
 */
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class AndroidAudioRecorderManagerAndroidTest {
    /**
     * Set up AndroidAppInit with Robolectric application context.
     */
    @Before
    fun setUp() {
        AndroidAppInit.init(RuntimeEnvironment.getApplication())
    }

    /**
     * Tests full recording lifecycle on Android.
     */
    @Test
    fun testAudioRecordingLifecycle() =
        runTest {
            val storage = createFileStorage()
            val recorder = createAudioRecorderManager(storage)
            assertNotNull(recorder)
            assertFalse(recorder.isRecording.value)
            assertEquals(0f, recorder.amplitude.value)

            val startRes = recorder.startRecording()
            assertTrue(startRes.isSuccess)
            assertTrue(recorder.isRecording.value)
            assertEquals(0.5f, recorder.amplitude.value)

            val pauseRes = recorder.pauseRecording()
            assertTrue(pauseRes.isSuccess)
            assertEquals(0f, recorder.amplitude.value)

            val resumeRes = recorder.resumeRecording()
            assertTrue(resumeRes.isSuccess)
            assertEquals(0.5f, recorder.amplitude.value)

            val stopRes = recorder.stopRecording("test_memo_android.wav")
            assertTrue(stopRes.isSuccess)
            assertFalse(recorder.isRecording.value)
            assertEquals(0f, recorder.amplitude.value)

            val restartRes = recorder.startRecording()
            assertTrue(restartRes.isSuccess)
            assertTrue(recorder.isRecording.value)

            val cancelRes = recorder.cancelRecording()
            assertTrue(cancelRes.isSuccess)
            assertFalse(recorder.isRecording.value)
            assertEquals(0f, recorder.amplitude.value)
        }

    /**
     * Tests recording lifecycle with granted RECORD_AUDIO permission and mocked MediaRecorder.
     */
    @Test
    fun testRecordingWithGrantedPermission() =
        runTest {
            val storage = createFileStorage()
            val mockContext = Mockito.mock(android.content.Context::class.java)
            val tempDir =
                java.io.File
                    .createTempFile("audio_test_cache", "")
                    .parentFile
            Mockito.`when`(mockContext.cacheDir).thenReturn(tempDir)
            Mockito
                .`when`(
                    mockContext.checkPermission(Mockito.eq(android.Manifest.permission.RECORD_AUDIO), Mockito.anyInt(), Mockito.anyInt()),
                ).thenReturn(android.content.pm.PackageManager.PERMISSION_GRANTED)

            val mockRecorder = Mockito.mock(android.media.MediaRecorder::class.java)
            var targetPath: String? = null
            Mockito.`when`(mockRecorder.setOutputFile(Mockito.anyString())).thenAnswer { invocation ->
                targetPath = invocation.arguments[0] as String
                null
            }

            val recorder =
                AndroidAudioRecorderManager(
                    fileStorage = storage,
                    contextProvider = { mockContext },
                    mediaRecorderFactory = { mockRecorder },
                    sdkInt = Build.VERSION_CODES.S,
                )

            val startRes = recorder.startRecording()
            assertTrue(startRes.isSuccess)
            assertTrue(recorder.isRecording.value)

            val pauseRes = recorder.pauseRecording()
            assertTrue(pauseRes.isSuccess)
            Mockito.verify(mockRecorder).pause()

            val resumeRes = recorder.resumeRecording()
            assertTrue(resumeRes.isSuccess)
            Mockito.verify(mockRecorder).resume()

            // Populate tempFile on disk so f.exists() and f.length() > 0 are true
            targetPath?.let { java.io.File(it).writeBytes(byteArrayOf(1, 2, 3, 4)) }

            val stopRes = recorder.stopRecording("granted_audio.wav")
            assertTrue(stopRes.isSuccess)
            Mockito.verify(mockRecorder).stop()
            Mockito.verify(mockRecorder).release()

            // Cancel recording test with active mockRecorder
            val startRes2 = recorder.startRecording()
            assertTrue(startRes2.isSuccess)
            targetPath?.let { java.io.File(it).writeBytes(byteArrayOf(5, 6, 7)) }
            val cancelRes = recorder.cancelRecording()
            assertTrue(cancelRes.isSuccess)
        }

    /**
     * Tests pause and resume on Android versions prior to Nougat (API 24).
     */
    @Test
    fun testPauseResumePreNougat() =
        runTest {
            val storage = createFileStorage()
            val recorder =
                AndroidAudioRecorderManager(
                    fileStorage = storage,
                    sdkInt = Build.VERSION_CODES.M,
                )
            recorder.startRecording()
            assertTrue(recorder.pauseRecording().isSuccess)
            assertTrue(recorder.resumeRecording().isSuccess)
            recorder.cancelRecording()
        }

    /**
     * Tests fallback when context is null.
     */
    @Test
    fun testNullContextFallback() =
        runTest {
            val storage = createFileStorage()
            val recorder =
                AndroidAudioRecorderManager(
                    fileStorage = storage,
                    contextProvider = { null },
                )
            val startRes = recorder.startRecording()
            assertTrue(startRes.isSuccess)
            assertTrue(recorder.isRecording.value)
            recorder.cancelRecording()
        }

    /**
     * Tests recoverCatching when MediaRecorder preparation or start throws.
     */
    @Test
    fun testMediaRecorderThrowsOnStart() =
        runTest {
            val storage = createFileStorage()
            val mockContext = Mockito.mock(android.content.Context::class.java)
            val tempDir =
                java.io.File
                    .createTempFile("audio_err", "")
                    .parentFile
            Mockito.`when`(mockContext.cacheDir).thenReturn(tempDir)
            Mockito
                .`when`(
                    mockContext.checkPermission(Mockito.eq(android.Manifest.permission.RECORD_AUDIO), Mockito.anyInt(), Mockito.anyInt()),
                ).thenReturn(android.content.pm.PackageManager.PERMISSION_GRANTED)

            val mockRecorder = Mockito.mock(android.media.MediaRecorder::class.java)
            Mockito.`when`(mockRecorder.prepare()).thenThrow(java.io.IOException("Failed to prepare recorder"))

            val recorder =
                AndroidAudioRecorderManager(
                    fileStorage = storage,
                    contextProvider = { mockContext },
                    mediaRecorderFactory = { mockRecorder },
                )

            val startRes = recorder.startRecording()
            assertTrue(startRes.isSuccess)
            assertTrue(recorder.isRecording.value)
            recorder.cancelRecording()
        }

    /**
     * Tests default mediaRecorderFactory lambda branches across Android versions.
     */
    @Test
    fun testDefaultMediaRecorderFactoryBranches() {
        val storage = createFileStorage()
        val context = RuntimeEnvironment.getApplication()

        val defaultMgr = AndroidAudioRecorderManager(fileStorage = storage)
        defaultMgr.contextProvider.invoke()
        defaultMgr.mediaRecorderFactory.invoke(context)

        // Exercise runCatching failure in default contextProvider when AndroidAppInit context is null
        val field = AndroidAppInit::class.java.getDeclaredField("context")
        field.isAccessible = true
        val oldCtx = field.get(null)
        field.set(null, null)
        // allow-exception
        try {
            defaultMgr.contextProvider.invoke()
        } finally {
            field.set(null, oldCtx)
        }

        runCatching {
            AndroidAudioRecorderManager.createDefaultMediaRecorder(context, Build.VERSION_CODES.S)
        }

        runCatching {
            AndroidAudioRecorderManager.createDefaultMediaRecorder(context, Build.VERSION_CODES.R)
        }
    }

    /**
     * Tests stopRecording when temp file does not exist or has 0 bytes.
     */
    @Test
    fun testStopRecordingWhenFileDoesNotExistOrEmpty() =
        runTest {
            val storage = createFileStorage()
            val mockContext = Mockito.mock(android.content.Context::class.java)
            val tempDir =
                java.io.File
                    .createTempFile("audio_empty", "")
                    .parentFile
            Mockito.`when`(mockContext.cacheDir).thenReturn(tempDir)
            Mockito
                .`when`(
                    mockContext.checkPermission(Mockito.eq(android.Manifest.permission.RECORD_AUDIO), Mockito.anyInt(), Mockito.anyInt()),
                ).thenReturn(android.content.pm.PackageManager.PERMISSION_GRANTED)

            val mockRecorder = Mockito.mock(android.media.MediaRecorder::class.java)
            var targetPath: String? = null
            Mockito.`when`(mockRecorder.setOutputFile(Mockito.anyString())).thenAnswer { invocation ->
                targetPath = invocation.arguments[0] as String
                null
            }

            val recorder =
                AndroidAudioRecorderManager(
                    fileStorage = storage,
                    contextProvider = { mockContext },
                    mediaRecorderFactory = { mockRecorder },
                )

            // Test 1: f exists but length == 0
            recorder.startRecording()
            targetPath?.let { java.io.File(it).createNewFile() }
            val stopRes1 = recorder.stopRecording("empty_file.wav")
            assertTrue(stopRes1.isSuccess)

            // Test 2: f does not exist on disk
            recorder.startRecording()
            targetPath?.let { java.io.File(it).delete() }
            val stopRes2 = recorder.stopRecording("non_existent_file.wav")
            assertTrue(stopRes2.isSuccess)
        }

    /**
     * Tests startRecording fallback when context.cacheDir is null.
     */
    @Test
    fun testStartRecordingWithNullCacheDir() =
        runTest {
            val storage = createFileStorage()
            val mockContext = Mockito.mock(android.content.Context::class.java)
            Mockito.`when`(mockContext.cacheDir).thenReturn(null)
            Mockito
                .`when`(
                    mockContext.checkPermission(Mockito.eq(android.Manifest.permission.RECORD_AUDIO), Mockito.anyInt(), Mockito.anyInt()),
                ).thenReturn(android.content.pm.PackageManager.PERMISSION_GRANTED)

            val mockRecorder = Mockito.mock(android.media.MediaRecorder::class.java)

            val recorder =
                AndroidAudioRecorderManager(
                    fileStorage = storage,
                    contextProvider = { mockContext },
                    mediaRecorderFactory = { mockRecorder },
                )
            val res = recorder.startRecording()
            assertTrue(res.isSuccess)
            recorder.cancelRecording()

            // Exercise fallbackTmp ?: "." when tmpdir system property is cleared
            val oldTmp = System.getProperty("java.io.tmpdir")
            // allow-exception
            try {
                System.clearProperty("java.io.tmpdir")
                val recorderCleared =
                    AndroidAudioRecorderManager(
                        fileStorage = storage,
                        contextProvider = { mockContext },
                        mediaRecorderFactory = { mockRecorder },
                    )
                val resCleared = recorderCleared.startRecording()
                assertTrue(resCleared.isSuccess)
                recorderCleared.cancelRecording()
            } finally {
                if (oldTmp != null) {
                    System.setProperty("java.io.tmpdir", oldTmp)
                }
            }
        }
}
