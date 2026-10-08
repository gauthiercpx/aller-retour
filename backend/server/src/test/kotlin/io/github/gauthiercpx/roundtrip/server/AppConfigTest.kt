package io.github.gauthiercpx.roundtrip.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AppConfigTest {
    @Test
    fun toStringNeverContainsTheKeyValue() {
        val config = AppConfig.fromEnvironment(mapOf("PRIM_API_KEY" to "dummy-secret-value"))

        assertFalse(config.toString().contains("dummy-secret-value"))
    }

    @Test
    fun reportsNothingMissingWhenKeyIsSet() {
        val config = AppConfig.fromEnvironment(mapOf("PRIM_API_KEY" to "dummy-secret-value"))

        assertEquals(emptyList(), config.missingConfig())
    }

    @Test
    fun treatsBlankKeyAsMissing() {
        val config = AppConfig.fromEnvironment(mapOf("PRIM_API_KEY" to "  "))

        assertEquals(listOf("PRIM_API_KEY"), config.missingConfig())
    }

    @Test
    fun defaultsPortTo8080() {
        val config = AppConfig.fromEnvironment(emptyMap())

        assertEquals(8080, config.port)
    }

    @Test
    fun readsPortFromEnvironment() {
        val config = AppConfig.fromEnvironment(mapOf("PORT" to "9090"))

        assertEquals(9090, config.port)
    }

    @Test
    fun rejectsNonNumericPort() {
        assertFailsWith<IllegalStateException> { AppConfig.fromEnvironment(mapOf("PORT" to "http")) }
    }

    @Test
    fun rejectsOutOfRangePort() {
        assertFailsWith<IllegalStateException> { AppConfig.fromEnvironment(mapOf("PORT" to "70000")) }
    }
}
