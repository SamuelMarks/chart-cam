/**
 * @file UUID.js.kt
 * UUID generation utilities for JS platform.
 */
package io.healthplatform.chartcam.utils

import kotlin.random.Random

private const val HEX_CHARS = "0123456789abcdef"
private const val BYTE_COUNT = 16
private const val UUID_LEN = 36
private const val BYTE_MASK = 0xff
private const val NIBBLE_SHIFT = 4
private const val NIBBLE_MASK = 0x0f
private const val VERSION_INDEX = 6
private const val VARIANT_INDEX = 8
private const val V4_CLEAR_MASK = 0x0f
private const val V4_SET_BITS = 0x40
private const val VAR_CLEAR_MASK = 0x3f
private const val VAR_SET_BITS = 0x80
private const val HYPHEN_INDEX_1 = 4
private const val HYPHEN_INDEX_2 = 6
private const val HYPHEN_INDEX_3 = 8
private const val HYPHEN_INDEX_4 = 10

private val HYPHEN_POSITIONS = intArrayOf(HYPHEN_INDEX_1, HYPHEN_INDEX_2, HYPHEN_INDEX_3, HYPHEN_INDEX_4)

/**
 * Platform-specific object for generating universally unique identifiers.
 */
actual object UUID {
    /**
     * Generates a random UUID string (version 4).
     *
     * @return A randomly generated UUID string.
     */
    actual fun randomUUID(): String {
        val bytes = ByteArray(BYTE_COUNT)
        Random.nextBytes(bytes)
        bytes[VERSION_INDEX] = ((bytes[VERSION_INDEX].toInt() and V4_CLEAR_MASK) or V4_SET_BITS).toByte()
        bytes[VARIANT_INDEX] = ((bytes[VARIANT_INDEX].toInt() and VAR_CLEAR_MASK) or VAR_SET_BITS).toByte()
        return buildString(UUID_LEN) {
            for (i in 0 until BYTE_COUNT) {
                if (HYPHEN_POSITIONS.contains(i)) append('-')
                val b = bytes[i].toInt() and BYTE_MASK
                append(HEX_CHARS[b shr NIBBLE_SHIFT])
                append(HEX_CHARS[b and NIBBLE_MASK])
            }
        }
    }
}
