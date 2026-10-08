package io.github.gauthiercpx.roundtrip.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ErrorBody(val error: String)

@Serializable
data class Readiness(val missing: List<String>)

fun Application.roundTripModule(config: AppConfig, service: DepartureService) {
    install(ContentNegotiation) {
        json(Json { encodeDefaults = true })
    }

    routing {
        get("/healthz") {
            call.respondText("ok")
        }

        // Ready means required config is loaded; upstream reachability is deliberately not checked.
        get("/readyz") {
            val missing = config.missingConfig()
            val status = if (missing.isEmpty()) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            call.respond(status, Readiness(missing))
        }

        get("/departures") {
            if (!isAuthorized(call.request.headers[HttpHeaders.Authorization], config.apiToken)) {
                call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer")
                call.respond(HttpStatusCode.Unauthorized, ErrorBody("Unauthorized"))
                return@get
            }
            val stops = try {
                parseStopIds(call.request.queryParameters["stops"])
            } catch (e: InvalidStopsException) {
                call.respond(HttpStatusCode.BadRequest, ErrorBody(e.message ?: "Invalid 'stops' parameter"))
                return@get
            }
            call.respond(service.departures(stops))
        }
    }
}
