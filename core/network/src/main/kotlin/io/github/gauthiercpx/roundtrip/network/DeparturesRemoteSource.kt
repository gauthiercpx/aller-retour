package io.github.gauthiercpx.roundtrip.network

import io.github.gauthiercpx.roundtrip.model.DeparturesResponse

interface DeparturesRemoteSource {
    /** @throws BackendException when the backend cannot be reached or does not answer with departures. */
    suspend fun fetch(baseUrl: String, stops: List<String>): DeparturesResponse
}

/** Any failure talking to the Round Trip backend, with the underlying cause attached. */
class BackendException(message: String, cause: Throwable? = null) : Exception(message, cause)
