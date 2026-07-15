package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertTrue

class UUIDTest {
    @Test
    fun testRandomUUID() {
        val uuid = UUID.randomUUID()
        assertTrue(uuid.isNotEmpty())
    }
}
