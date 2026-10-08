package io.github.gauthiercpx.roundtrip.server

import io.github.gauthiercpx.roundtrip.server.reference.OpenDataLineColors
import io.github.gauthiercpx.roundtrip.server.upstream.prim.PrimClient
import io.github.gauthiercpx.roundtrip.server.upstream.star.StarClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import java.time.Clock

fun main() {
    val log = LoggerFactory.getLogger("io.github.gauthiercpx.roundtrip.server.Main")
    val config = AppConfig.fromEnvironment()
    log.info("Starting with {}; missing config: {}", config, config.missingConfig())

    // PRIM only accepts TLS 1.3 (a TLS 1.2 handshake gets a protocol_version alert). Ktor's CIO engine has
    // its own TLS stack without 1.3, so use the Java engine, which relies on the JDK's TLS implementation.
    val http = HttpClient(Java) {
        install(HttpTimeout) {
            connectTimeoutMillis = 3_000
            requestTimeoutMillis = 5_000
        }
    }
    val service = DepartureService.create(
        prim = PrimClient(http, config.primApiKey),
        star = StarClient(http),
        lineColors = OpenDataLineColors(http, Clock.systemUTC()),
        clock = Clock.systemUTC(),
    )

    embeddedServer(Netty, port = config.port) {
        monitor.subscribe(ApplicationStopped) { http.close() }
        roundTripModule(config, service)
    }.start(wait = true)
}
