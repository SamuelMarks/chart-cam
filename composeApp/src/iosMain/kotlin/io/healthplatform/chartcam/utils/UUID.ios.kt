package io.healthplatform.chartcam.utils

import platform.Foundation.NSUUID

actual object UUID {
    actual fun randomUUID(): String = NSUUID().UUIDString()
}