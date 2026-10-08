package io.github.gauthiercpx.roundtrip.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AppConfigTest {
    @Test
    fun toStringNeverContainsTheSecretValues() {
        val config = AppConfig.fromEnvironment(
            mapOf("PRIM_API_KEY" to "dummy-secret-value", "API_TOKEN" to "dummy-token-value"),
        )

        assertFalse(config.toString().contains("dummy-secret-value"))
        assertFalse(config.toString().contains("dummy-token-value"))
    }

    @Test
    fun reportsNothingMissingWhenKeyAndTokenAreSet() {
        val config = AppConfig.fromEnvironment(
            mapOf("PRIM_API_KEY" to "dummy-secret-value", "API_TOKEN" to "dummy-token-value"),
        )

        assertEquals(emptyList(), config.missingConfig())
    }

    @Test
    fun treatsBlankKeyAsMissing() {
        val config = AppConfig.fromEnvironment(mapOf("PRIM_API_KEY" to "  ", "API_TOKEN" to "dummy-token-value"))

        assertEquals(listOf("PRIM_API_KEY"), config.missingConfig())
    }

    @Test
    fun treatsBlankTokenAsMissing() {
        val config = AppConfig.fromEnvironment(mapOf("PRIM_API_KEY" to "dummy-secret-value", "API_TOKEN" to " "))

        assertEquals(listOf("API_TOKEN"), config.missingConfig())
    }

    @Test
    fun reportsBothMissingWhenNothingIsSet() {
        assertEquals(listOf("PRIM_API_KEY", "API_TOKEN"), AppConfig.fromEnvironment(emptyMap()).missingConfig())
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
