package io.github.gauthiercpx.roundtrip.network

import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal interface DeparturesApi {
    @GET("departures")
    suspend fun departures(@Query("stops") stops: String): DeparturesResponse
}

@Singleton
class RetrofitDeparturesSource @Inject constructor(private val client: OkHttpClient, private val json: Json) :
    DeparturesRemoteSource {
    // The base URL is a user setting, so the Retrofit instance is rebuilt only when it changes.
    private var cachedApi: Pair<String, DeparturesApi>? = null

    // One throw per failure kind keeps the boundary mapping explicit.
    @Suppress("ThrowsCount")
    override suspend fun fetch(baseUrl: String, stops: List<String>): DeparturesResponse {
        val api = apiFor(baseUrl)
        try {
            return api.departures(stops.joinToString(","))
        } catch (e: HttpException) {
            throw BackendException("Backend answered HTTP ${e.code()}", e)
        } catch (e: IOException) {
            throw BackendException("Backend unreachable: ${e.message}", e)
        } catch (e: SerializationException) {
            throw BackendException("Backend response could not be read", e)
        }
    }

    @Synchronized
    private fun apiFor(baseUrl: String): DeparturesApi {
        cachedApi?.takeIf { it.first == baseUrl }?.let { return it.second }
        val api = try {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(DeparturesApi::class.java)
        } catch (e: IllegalArgumentException) {
            throw BackendException("Backend URL is invalid: $baseUrl", e)
        }
        cachedApi = baseUrl to api
        return api
    }
}
