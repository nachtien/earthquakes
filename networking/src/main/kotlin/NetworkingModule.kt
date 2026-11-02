package com.achtien.networking

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

fun createHttpClient(
    httpClientEngine: HttpClientEngine,
    json: Json,
) = HttpClient(httpClientEngine) {
    install(ContentNegotiation) {
        json(json)
    }

    install(DefaultRequest) {
        url("https://earthquake.usgs.gov/")
    }
}

@Module
@ComponentScan
class NetworkingModule {

    @Single
    fun httpClientEngine(): HttpClientEngine = Android.create()

    @Single
    fun json() = Json { ignoreUnknownKeys = true }

    @Single
    fun httpClient(httpClientEngine: HttpClientEngine, json: Json) =
        createHttpClient(httpClientEngine, json)
}
