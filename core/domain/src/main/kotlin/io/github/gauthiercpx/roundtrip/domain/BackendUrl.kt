package io.github.gauthiercpx.roundtrip.domain

import java.net.URI
import java.net.URISyntaxException

/**
 * Returns the backend base URL with a trailing slash, or null when it is not a plain `https` origin or path.
 * Cleartext is rejected on purpose: Android blocks it by default, and the Tailscale endpoint is served over TLS.
 */
fun normalizeBackendUrl(raw: String): String? {
    val trimmed = raw.trim()
    return parseUri(trimmed)?.takeIf { it.isUsableBackend() }?.let { trimmed.trimEnd('/') + "/" }
}

private fun parseUri(raw: String): URI? = try {
    URI(raw)
} catch (_: URISyntaxException) {
    null
}

private fun URI.isUsableBackend(): Boolean = scheme.equals("https", ignoreCase = true) &&
    !host.isNullOrEmpty() &&
    userInfo == null &&
    query == null &&
    fragment == null
