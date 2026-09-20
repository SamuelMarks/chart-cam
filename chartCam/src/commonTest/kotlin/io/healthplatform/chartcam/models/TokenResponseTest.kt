/**
 * @file TokenResponseTest.kt
 * Unit tests verifying TokenResponse data model serialization, equality, and components.
 */

package io.healthplatform.chartcam.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Test suite for [TokenResponse].
 */
class TokenResponseTest {
    /**
     * Verifies construction, property access, and destructuring components.
     */
    @Test
    fun testPropertiesAndDestructuring() {
        val response =
            TokenResponse(
                accessToken = "access-123",
                refreshToken = "refresh-456",
                expiresIn = 3600,
                tokenType = "Bearer",
            )

        assertEquals("access-123", response.accessToken)
        assertEquals("refresh-456", response.refreshToken)
        assertEquals(3600, response.expiresIn)
        assertEquals("Bearer", response.tokenType)

        val (access, refresh, expires, type) = response
        assertEquals("access-123", access)
        assertEquals("refresh-456", refresh)
        assertEquals(3600, expires)
        assertEquals("Bearer", type)
    }

    /**
     * Verifies equals, hashCode, copy, and toString semantics.
     */
    @Test
    fun testEqualsHashCodeAndCopy() {
        val original =
            TokenResponse(
                accessToken = "tok-1",
                refreshToken = "ref-1",
                expiresIn = 1800,
                tokenType = "Bearer",
            )
        val identical =
            TokenResponse(
                accessToken = "tok-1",
                refreshToken = "ref-1",
                expiresIn = 1800,
                tokenType = "Bearer",
            )
        val different = original.copy(accessToken = "tok-2")
        val differentRefresh = original.copy(refreshToken = "ref-2")
        val differentExpiry = original.copy(expiresIn = 7200)
        val differentType = original.copy(tokenType = "MAC")

        assertEquals(original, original)
        assertEquals(original, identical)
        assertEquals(original.hashCode(), identical.hashCode())

        assertNotEquals(original, different)
        assertNotEquals(original, differentRefresh)
        assertNotEquals(original, differentExpiry)
        assertNotEquals(original, differentType)
        assertFalse(original.equals(null))
        assertFalse(original.equals("some string"))

        assertTrue(original.toString().contains("tok-1"))
    }

    /**
     * Verifies serialization and deserialization round-trip with kotlinx.serialization.
     */
    @Test
    fun testSerializationRoundTrip() {
        val token =
            TokenResponse(
                accessToken = "json-tok",
                refreshToken = "json-ref",
                expiresIn = 900,
                tokenType = "Bearer",
            )
        val serialized = Json.encodeToString(TokenResponse.serializer(), token)
        assertTrue(serialized.contains("json-tok"))

        val deserialized = Json.decodeFromString(TokenResponse.serializer(), serialized)
        assertEquals(token, deserialized)

        val desc = TokenResponse.serializer().descriptor
        assertEquals("io.healthplatform.chartcam.models.TokenResponse", desc.serialName)
        assertEquals(4, desc.elementsCount)

        val incompleteJson = """{"accessToken":"tok"}"""
        val failRes = runCatching { Json.decodeFromString<TokenResponse>(incompleteJson) }
        assertTrue(failRes.isFailure)
    }
}
