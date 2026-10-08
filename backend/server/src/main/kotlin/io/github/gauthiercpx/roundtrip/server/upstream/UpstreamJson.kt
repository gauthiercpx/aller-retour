package io.github.gauthiercpx.roundtrip.server.upstream

import kotlinx.serialization.json.Json

/** Upstream payloads carry many fields we do not use; only the mapped ones are declared. */
internal val UpstreamJson = Json { ignoreUnknownKeys = true }
