package io.github.gauthiercpx.roundtrip.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackendUrlTest {
    @Test
    fun addsTrailingSlashAndTrimsWhitespace() {
        assertEquals("https://rt.tail1234.ts.net/", normalizeBackendUrl("  https://rt.tail1234.ts.net "))
    }

    @Test
    fun keepsASinglePathSegmentWithOneTrailingSlash() {
        assertEquals("https://host.example/api/", normalizeBackendUrl("https://host.example/api//"))
    }

    @Test
    fun rejectsCleartext() {
        assertNull(normalizeBackendUrl("http://rt.tail1234.ts.net"))
    }

    @Test
    fun acceptsCleartextOnLoopbackForLocalDevelopment() {
        assertEquals("http://localhost:8080/", normalizeBackendUrl("http://localhost:8080"))
        assertEquals("http://127.0.0.1:8080/", normalizeBackendUrl("http://127.0.0.1:8080/"))
    }

    @Test
    fun rejectsCleartextOnLookalikeHosts() {
        assertNull(normalizeBackendUrl("http://localhost.evil.example"))
        assertNull(normalizeBackendUrl("http://192.168.1.10:8080"))
    }

    @Test
    fun rejectsMissingHost() {
        assertNull(normalizeBackendUrl("https://"))
    }

    @Test
    fun rejectsCredentialsQueryAndFragment() {
        assertNull(normalizeBackendUrl("https://user:pw@host.example"))
        assertNull(normalizeBackendUrl("https://host.example?x=1"))
        assertNull(normalizeBackendUrl("https://host.example#top"))
    }

    @Test
    fun rejectsBlankAndMalformedInput() {
        assertNull(normalizeBackendUrl(""))
        assertNull(normalizeBackendUrl("https://exa mple.com"))
    }
}
