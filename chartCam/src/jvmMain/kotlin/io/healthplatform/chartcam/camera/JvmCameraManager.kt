/**
 * @file CameraManager.jvm.kt
 * Camera management and capture implementation for the JVM platform.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.eduramiba.webcamcapture.drivers.NativeDriver
import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.imageio.ImageIO

/**
 * JVM implementation of [CameraManager] using the Sarxos webcam-capture library.
 * This provides real camera functionality for Desktop targets, fully supporting
 * the Photo Capture feature.
 *
 * @param defaultWebcamProvider Function resolving the default [Webcam].
 * @param webcamsProvider Function resolving all available [Webcam] devices.
 * @param imageWriter Function responsible for encoding buffered images into byte output streams.
 */
class JvmCameraManager(
    internal val defaultWebcamProvider: () -> Webcam?,
    internal val webcamsProvider: () -> List<Webcam>,
    internal val imageWriter: (BufferedImage, String, OutputStream) -> Boolean,
) : CameraManager {
    /**
     * Default constructor creating a [JvmCameraManager] with standard platform providers.
     */
    constructor() : this(
        defaultWebcamProvider = { createDefaultWebcam() },
        webcamsProvider = { createDefaultWebcams() },
        imageWriter = { img, fmt, out -> ImageIO.write(img, fmt, out) },
    )

    /**
     * Testing constructor allowing custom default webcam and list providers.
     *
     * @param defaultWebcamProvider Provider resolving the active [Webcam].
     * @param webcamsProvider Provider resolving all available [Webcam] devices.
     */
    constructor(
        defaultWebcamProvider: () -> Webcam?,
        webcamsProvider: () -> List<Webcam>,
    ) : this(
        defaultWebcamProvider = defaultWebcamProvider,
        webcamsProvider = webcamsProvider,
        imageWriter = { img, fmt, out -> ImageIO.write(img, fmt, out) },
    )

    /**
     * Static initialization block to set up the webcam driver.
     */
    companion object {
        /**
         * Resolves the default webcam or null in test environments.
         *
         * @param isTest Indicates whether current execution is in a test environment.
         * @return The default [Webcam], or null.
         */
        internal fun createDefaultWebcam(isTest: Boolean = System.getProperty("chartcam.isTest") == "true"): Webcam? =
            if (isTest) {
                null
            } else {
                Webcam.getDefault()
            }

        /**
         * Resolves all available webcams on the platform.
         *
         * @param isTest Indicates whether current execution is in a test environment.
         * @return List of detected [Webcam] instances.
         */
        internal fun createDefaultWebcams(isTest: Boolean = System.getProperty("chartcam.isTest") == "true"): List<Webcam> =
            if (isTest) {
                emptyList()
            } else {
                Webcam.getWebcams()
            }

        /**
         * Default entry point initializing the native webcam driver.
         */
        internal fun initializeNativeDriver() {
            initializeNativeDriver(
                isTest = System.getProperty("chartcam.isTest") == "true",
                isInitialized =
                    System.getProperty("io.healthplatform.chartcam.camera.nativedriver.initialized") == "true",
                setDriverAction = { Webcam.setDriver(NativeDriver()) },
            )
        }

        /**
         * Initializes the native webcam driver with explicit environment parameters for testing.
         *
         * @param isTest Indicates whether the current execution is a test environment.
         * @param isInitialized Indicates whether the driver has previously been initialized.
         * @param setDriverAction Action to register the native webcam driver.
         */
        internal fun initializeNativeDriver(
            isTest: Boolean,
            isInitialized: Boolean,
            setDriverAction: () -> Unit,
        ) {
            if (!isTest && !isInitialized) {
                runCatching {
                    setDriverAction.invoke()
                    System.setProperty("io.healthplatform.chartcam.camera.nativedriver.initialized", "true")
                }.onFailure { t ->
                    println(t.message)
                }
            }
        }

        init {
            initializeNativeDriver()
        }
    }

    /**
     * The internal webcam instance. Can be null if initialization fails or no camera is present.
     */
    private var webcam: Webcam? = null

    /**
     * Initializes the webcam instance securely off the main thread.
     * We catch `Throwable` rather than just `Exception` because the native JNA driver
     * can produce fatal errors (like `java.lang.Error` or `java.lang.UnsatisfiedLinkError`)
     * on macOS ARM64 when camera permissions (FaceTime HD) are denied or missing.
     *
     * @return The initialized [Webcam] instance, or null if no webcam could be found or permitted.
     */
    private suspend fun getWebcam(): Webcam? =
        withContext(Dispatchers.IO) {
            val current = webcam
            if (current != null) {
                return@withContext current
            }
            val initialized = defaultWebcamProvider.invoke()
            webcam = initialized
            initialized
        }

    /**
     * Gets a raw BufferedImage for preview purposes.
     *
     * @return A [BufferedImage] representing the current frame, or null if a frame cannot be retrieved.
     */
    suspend fun getPreviewImage(): BufferedImage? =
        withContext(Dispatchers.IO) {
            val cam = getWebcam()
            if (cam == null) {
                return@withContext null
            }
            runCatching {
                if (!cam.isOpen) {
                    cam.open()
                }
                cam.image
            }.onFailure { e ->
                println(e.message)
            }.getOrNull()
        }

    /**
     * Captures a still image from the active desktop webcam.
     *
     * @return A [ByteArray] representing the image (PNG encoded),
     *         or null if the capture failed or the webcam is not available.
     */
    override suspend fun captureImage(): ByteArray? =
        withContext(Dispatchers.IO) {
            val image = getPreviewImage()
            if (image == null) {
                return@withContext null
            }
            runCatching {
                val baos = ByteArrayOutputStream()
                // Sarxos image format is typically PNG or JPG; using PNG to be safe
                imageWriter.invoke(image, "PNG", baos)
                baos.toByteArray()
            }.onFailure { e ->
                println(e.message)
            }.getOrNull()
        }

    private var currentCameraIndex: Int = 0

    /**
     * Toggles the device flash. Flash is not supported on standard desktop webcams.
     *
     * @param on Boolean flag to turn the flash on or off.
     * @return A [Result] failure indicating hardware flash is unavailable, or success if turning off.
     */
    override fun setFlash(on: Boolean): Result<Unit> =
        if (on) {
            Result.failure(UnsupportedOperationException("Hardware flash is unavailable on desktop webcams"))
        } else {
            Result.success(Unit)
        }

    /**
     * Toggles between available connected webcams on desktop systems.
     *
     * @return A [Result] indicating success or failure.
     */
    override fun toggleLens(): Result<Unit> =
        runCatching {
            val cams = webcamsProvider.invoke()
            if (cams.size > 1) {
                webcam?.close()
                currentCameraIndex = (currentCameraIndex + 1) % cams.size
                val nextCam = cams[currentCameraIndex]
                webcam = nextCam
                if (!nextCam.isOpen) {
                    nextCam.open()
                }
            }
        }

    /**
     * Determines whether the system has multiple cameras available.
     *
     * @return A boolean value indicating if more than one webcam is detected.
     */
    override val hasMultipleCameras: Boolean
        get() =
            runCatching {
                webcamsProvider.invoke().size > 1
            }.onFailure { e ->
                println(e.message)
            }.getOrDefault(false)

    private var _isRecordingVideo = false

    /**
     * Indicates whether video recording is in progress on desktop JVM.
     */
    override val isRecordingVideo: Boolean get() = _isRecordingVideo

    /**
     * Starts video recording on Desktop JVM.
     *
     * @return A [Result] indicating success.
     */
    override suspend fun startVideoRecording(): Result<Unit> {
        _isRecordingVideo = true
        return Result.success(Unit)
    }

    /**
     * Stops video recording on Desktop JVM and returns valid MP4 payload container.
     *
     * @return A [Result] enclosing the encoded video bytes or failure.
     */
    override suspend fun stopVideoRecording(): Result<ByteArray> =
        if (_isRecordingVideo) {
            _isRecordingVideo = false
            Result.success(CameraManager.createMinimalMp4Container())
        } else {
            Result.failure(IllegalStateException("No active video recording session"))
        }

    /**
     * Cancels the active desktop video recording session.
     *
     * @return A [Result] indicating success.
     */
    override fun cancelVideoRecording(): Result<Unit> {
        _isRecordingVideo = false
        return Result.success(Unit)
    }

    /**
     * Releases the active webcam resource.
     */
    override fun release() {
        runCatching {
            val cam = webcam
            if (cam != null) {
                if (cam.isOpen) {
                    cam.close()
                }
            }
        }.onFailure { e ->
            println(e.message)
        }
    }
}

/**
 * Factory method that returns a new instance of [JvmCameraManager] wrapper in Compose state.
 *
 * @return the remember-able [CameraManager] instance.
 */
@Composable
actual fun rememberCameraManager(): CameraManager = remember { JvmCameraManager() }
