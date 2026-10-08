package io.github.gauthiercpx.roundtrip.server

/** Runtime configuration. Secrets come from environment variables only. */
class AppConfig(
    val primApiKey: String?,
    val port: Int,
) {
    /** Names (never values) of required settings that are not set. */
    fun missingConfig(): List<String> = buildList {
        if (primApiKey == null) add("PRIM_API_KEY")
    }

    override fun toString(): String =
        "AppConfig(primApiKey=${if (primApiKey == null) "unset" else "set"}, port=$port)"

    companion object {
        private const val DEFAULT_PORT = 8080

        fun fromEnvironment(env: Map<String, String> = System.getenv()): AppConfig {
            val port = env["PORT"]?.let { raw ->
                raw.toIntOrNull()?.takeIf { it in 1..65535 }
                    ?: error("PORT must be an integer between 1 and 65535, got '$raw'")
            } ?: DEFAULT_PORT
            return AppConfig(
                primApiKey = env["PRIM_API_KEY"]?.takeIf { it.isNotBlank() },
                port = port,
            )
        }
    }
}
