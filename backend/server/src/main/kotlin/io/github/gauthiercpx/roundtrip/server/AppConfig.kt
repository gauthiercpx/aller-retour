package io.github.gauthiercpx.roundtrip.server

/** Runtime configuration. Secrets come from environment variables only. */
class AppConfig(
    val primApiKey: String?,
    /** Bearer token the app must send to /departures. Required: without it every request is refused. */
    val apiToken: String?,
    val port: Int,
) {
    /** Names (never values) of required settings that are not set. */
    fun missingConfig(): List<String> = buildList {
        if (primApiKey == null) add("PRIM_API_KEY")
        if (apiToken == null) add("API_TOKEN")
    }

    override fun toString(): String =
        "AppConfig(primApiKey=${if (primApiKey == null) "unset" else "set"}, " +
            "apiToken=${if (apiToken == null) "unset" else "set"}, port=$port)"

    companion object {
        private const val DEFAULT_PORT = 8080

        fun fromEnvironment(env: Map<String, String> = System.getenv()): AppConfig {
            val port = env["PORT"]?.let { raw ->
                raw.toIntOrNull()?.takeIf { it in 1..65535 }
                    ?: error("PORT must be an integer between 1 and 65535, got '$raw'")
            } ?: DEFAULT_PORT
            return AppConfig(
                primApiKey = env["PRIM_API_KEY"]?.takeIf { it.isNotBlank() },
                apiToken = env["API_TOKEN"]?.takeIf { it.isNotBlank() },
                port = port,
            )
        }
    }
}
