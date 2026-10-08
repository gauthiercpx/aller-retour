package io.github.gauthiercpx.roundtrip.domain

/**
 * Returns the token trimmed, or null when it cannot be sent as an HTTP header value (inner whitespace or
 * control characters, for example from a bad paste). A blank token is valid and means "send none".
 */
fun normalizeApiToken(raw: String): String? {
    val trimmed = raw.trim()
    return trimmed.takeIf { token -> token.none { it.isWhitespace() || it.isISOControl() } }
}
