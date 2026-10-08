package io.github.gauthiercpx.roundtrip.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BearerAuthTest {
    @Test
    fun acceptsTheExpectedToken() {
        assertTrue(isAuthorized("Bearer s3cret-token", "s3cret-token"))
    }

    @Test
    fun acceptsTheSchemeInAnyCase() {
        assertTrue(isAuthorized("bearer s3cret-token", "s3cret-token"))
    }

    @Test
    fun rejectsAWrongToken() {
        assertFalse(isAuthorized("Bearer wrong-token", "s3cret-token"))
    }

    @Test
    fun rejectsATokenThatOnlySharesAPrefix() {
        assertFalse(isAuthorized("Bearer s3cret", "s3cret-token"))
        assertFalse(isAuthorized("Bearer s3cret-token-and-more", "s3cret-token"))
    }

    @Test
    fun rejectsAMissingHeader() {
        assertFalse(isAuthorized(null, "s3cret-token"))
    }

    @Test
    fun rejectsOtherSchemes() {
        assertFalse(isAuthorized("Basic s3cret-token", "s3cret-token"))
        assertFalse(isAuthorized("s3cret-token", "s3cret-token"))
    }

    @Test
    fun rejectsAnEmptyBearerToken() {
        assertFalse(isAuthorized("Bearer ", "s3cret-token"))
    }

    @Test
    fun rejectsEverythingWhenNoTokenIsConfigured() {
        assertFalse(isAuthorized("Bearer anything", null))
        assertFalse(isAuthorized("Bearer ", null))
    }
}
