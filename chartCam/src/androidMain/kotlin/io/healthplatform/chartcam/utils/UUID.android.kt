package io.healthplatform.chartcam.utils

import java.util.UUID as JavaUUID

actual object UUID {
    actual fun randomUUID(): String = JavaUUID.randomUUID().toString()
}