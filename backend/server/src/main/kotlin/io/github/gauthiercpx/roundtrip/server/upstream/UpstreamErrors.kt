package io.github.gauthiercpx.roundtrip.server.upstream

import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import java.io.IOException

class UpstreamHttpException(upstream: String, status: Int) : IOException("$upstream returned HTTP $status")

/** HTTP 200, but nothing usable in it: an error flag, a missing envelope, or no mappable row. */
class UpstreamDataException(upstream: String, detail: String) : IOException("$upstream returned unusable data: $detail")

class RateLimitedException(upstream: String) : Exception("$upstream local rate limit reached; upstream not called")

class MissingConfigException(variable: String) : Exception("$variable is not set")

internal fun HttpResponse.ensureSuccess(upstream: String) {
    if (!status.isSuccess()) throw UpstreamHttpException(upstream, status.value)
}
