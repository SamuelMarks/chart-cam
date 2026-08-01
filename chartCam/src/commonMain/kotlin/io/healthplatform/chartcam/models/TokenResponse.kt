/**
 * @file TokenResponse.kt
 * Contains declarations for TokenResponse.kt.
 */
package io.healthplatform.chartcam.models

import kotlinx.serialization.Serializable

/**
 * Represents the response received when authenticating, containing tokens.
 *
 * @property accessToken The token used to access protected resources.
 * @property refreshToken The token used to refresh the access token.
 * @property expiresIn The duration in seconds until the access token expires.
 * @property tokenType The type of the token, usually "Bearer".
 */
@Serializable
data class TokenResponse(
    /** The token used to access protected resources. */
    val accessToken: kotlin.String,
    /** The token used to refresh the access token. */
    val refreshToken: kotlin.String,
    /** The duration in seconds until the access token expires. */
    val expiresIn: Int,
    /** The type of the token, usually "Bearer". */
    val tokenType: kotlin.String,
)
