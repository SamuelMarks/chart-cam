/**
 * @file PermissionManagerAndroidTest.kt
 * Contains declarations for PermissionManagerAndroidTest.kt.
 */
package io.healthplatform.chartcam.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android host tests for PermissionManager.
 */
@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class PermissionManagerAndroidTest {
    /**
     * Verifies PermissionDeniedException creation on Android.
     */
    @Test
    fun testPermissionDeniedException() {
        val ex = PermissionDeniedException()
        assertNotNull(ex)
        val exCustom = PermissionDeniedException(message = "Custom reason")
        assertEquals("Custom reason", exCustom.message)
    }

    /**
     * Verifies rememberPermissionManager composable execution.
     */
    @Test
    fun testRememberPermissionManagerComposable() {
        val activity =
            org.robolectric.Robolectric
                .buildActivity(androidx.activity.ComponentActivity::class.java)
                .setup()
                .get()

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
            var rememberedMgr: PermissionManager? = null
            var internalMgr: PermissionManager? = null
            composition.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalContext provides activity,
                ) {
                    rememberedMgr = rememberPermissionManager()
                    internalMgr =
                        rememberPermissionManagerInternal(
                            launcherFactory = { onResult ->
                                onResult(true)
                                val action: (String) -> Unit = { _ -> onResult(true) }
                                action
                            },
                        )
                }
            }
            assertNotNull(rememberedMgr)
            assertNotNull(internalMgr)

            (rememberedMgr as? AndroidPermissionManager)?.let { mgr ->
                mgr.requestLauncher("android.permission.CAMERA")
                mgr.onPermissionResult(true)
            }

            runBlocking {
                val res = internalMgr.requestCameraPermission()
                assertTrue(res.isSuccess)
            }
        } finally {
            composition.dispose()
        }
    }

    /**
     * Verifies getCameraPermissionStatus when permission is granted and denied.
     */
    @Test
    fun testGetCameraPermissionStatus() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito
            .`when`(mockContext.checkPermission(Mockito.eq(Manifest.permission.CAMERA), Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        val managerGranted = AndroidPermissionManager(mockContext) {}
        assertEquals(PermissionStatus.GRANTED, managerGranted.getCameraPermissionStatus())

        Mockito
            .`when`(mockContext.checkPermission(Mockito.eq(Manifest.permission.CAMERA), Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        val managerDenied = AndroidPermissionManager(mockContext) {}
        assertEquals(PermissionStatus.DENIED, managerDenied.getCameraPermissionStatus())
    }

    /**
     * Verifies requestCameraPermission when permission is already granted.
     */
    @Test
    fun testRequestCameraPermissionAlreadyGranted() =
        runBlocking {
            val mockContext = Mockito.mock(Context::class.java)
            Mockito
                .`when`(mockContext.checkPermission(Mockito.eq(Manifest.permission.CAMERA), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED)

            val manager = AndroidPermissionManager(mockContext) {}
            val result = manager.requestCameraPermission()
            assertTrue(result.isSuccess)
        }

    /**
     * Verifies requestCameraPermission when permission is requested and granted.
     */
    @Test
    fun testRequestCameraPermissionGrantedFlow() =
        runBlocking {
            val mockContext = Mockito.mock(Context::class.java)
            Mockito
                .`when`(mockContext.checkPermission(Mockito.eq(Manifest.permission.CAMERA), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(PackageManager.PERMISSION_DENIED)

            var requestedPermission: String? = null
            lateinit var manager: AndroidPermissionManager
            manager =
                AndroidPermissionManager(mockContext) { perm ->
                    requestedPermission = perm
                    manager.onPermissionResult(true)
                }

            val result = manager.requestCameraPermission()
            assertTrue(result.isSuccess)
            assertEquals(Manifest.permission.CAMERA, requestedPermission)
        }

    /**
     * Verifies requestCameraPermission when permission is requested and denied.
     */
    @Test
    fun testRequestCameraPermissionDeniedFlow() =
        runBlocking {
            val mockContext = Mockito.mock(Context::class.java)
            Mockito
                .`when`(mockContext.checkPermission(Mockito.eq(Manifest.permission.CAMERA), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(PackageManager.PERMISSION_DENIED)

            lateinit var manager: AndroidPermissionManager
            manager =
                AndroidPermissionManager(mockContext) {
                    manager.onPermissionResult(false)
                }

            val result = manager.requestCameraPermission()
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is PermissionDeniedException)
        }

    /**
     * Verifies onPermissionResult when callback is null.
     */
    @Test
    fun testOnPermissionResultNullCallback() {
        val mockContext = Mockito.mock(Context::class.java)
        val manager = AndroidPermissionManager(mockContext) {}
        manager.onPermissionResult(true)
    }

    /**
     * Verifies openSettings launches application details intent.
     */
    @Test
    fun testOpenSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = AndroidPermissionManager(context) {}
        manager.openSettings()
    }
}
