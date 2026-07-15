package io.healthplatform.chartcam.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecureStorageCommonTest {
    class MockSecureStorage : SecureStorage {
        val map = mutableMapOf<String, String>()

        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        override fun getString(key: String): String? = map[key]

        override fun delete(key: String) {
            map.remove(key)
        }
    }

    @Test
    fun testSecureStorageInterface() {
        val storage = MockSecureStorage()

        assertNull(storage.getString("missing"))

        storage.save("key", "value")
        assertEquals("value", storage.getString("key"))

        storage.delete("key")
        assertNull(storage.getString("key"))
    }
}
