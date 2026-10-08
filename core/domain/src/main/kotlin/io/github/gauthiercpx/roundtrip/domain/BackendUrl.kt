package io.github.gauthiercpx.roundtrip.domain

import java.net.URI
import java.net.URISyntaxException

/**
 * Returns the backend base URL with a trailing slash, or null when it is not an `https` origin or path.
 * Cleartext is rejected on purpose: Android blocks it by default, and the Tailscale endpoint is served over TLS.
 * The one exception is a loopback host, used with `adb reverse` against a backend on the dev machine; release
 * builds still refuse the connection because only the debug build permits cleartext.
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

private val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1")

private fun URI.isAllowedScheme(): Boolean = scheme.equals("https", ignoreCase = true) ||
    (scheme.equals("http", ignoreCase = true) && host in LOOPBACK_HOSTS)

private fun URI.isUsableBackend(): Boolean = isAllowedScheme() &&
    !host.isNullOrEmpty() &&
    userInfo == null &&
    query == null &&
    fragment == null
