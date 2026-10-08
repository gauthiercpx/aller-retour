package io.github.gauthiercpx.roundtrip.server

import java.security.MessageDigest

private const val BEARER_PREFIX = "Bearer "

/**
 * True when [authorizationHeader] is `Bearer <expected>`. A missing [expectedToken] never matches, so an
 * unconfigured server rejects everything instead of accepting everyone.
 *
 * Both sides are hashed before comparing, so the comparison takes the same time whatever the input length or
 * the position of the first wrong character.
 */
fun isAuthorized(authorizationHeader: String?, expectedToken: String?): Boolean {
    if (expectedToken == null || authorizationHeader == null) return false
    if (!authorizationHeader.startsWith(BEARER_PREFIX, ignoreCase = true)) return false
    val presented = authorizationHeader.substring(BEARER_PREFIX.length).trim()
    return MessageDigest.isEqual(sha256(presented), sha256(expectedToken))
}

private fun sha256(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
