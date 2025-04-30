package ru.hse.miem.miptweather.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1/"
    private const val OPENWEATHERMAP_BASE_URL = "https://api.openweathermap.org/data/3.0/"
    private const val WEATHERAPI_BASE_URL = "https://api.weatherapi.com/v1/"
    private const val DEFAULT_TIMEOUT = 15_000L

    private fun createBaseHttpClient(json: Json): HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.BODY
            // level = LogLevel.INFO // TODO Менее подробное логирование для production
        }
        install(HttpTimeout) {
            requestTimeoutMillis = DEFAULT_TIMEOUT
            connectTimeoutMillis = DEFAULT_TIMEOUT
            socketTimeoutMillis = DEFAULT_TIMEOUT
        }
         defaultRequest { header("User-Agent", "MIPTWeatherApp/1.0") }
    }

    @Provides
    @Singleton
    @Named("open-meteo")
    fun provideOpenMeteoClient(json: Json): HttpClient = HttpClient(Android) {
        apply(createBaseHttpClient(json))
        defaultRequest {
            url(OPEN_METEO_BASE_URL)
        }
    }

    @Provides
    @Singleton
    @Named("openweathermap")
    fun provideOpenWeatherMapClient(json: Json): HttpClient = HttpClient(Android) {
        apply(createBaseHttpClient(json))
        defaultRequest {
            url(OPENWEATHERMAP_BASE_URL)
        }
    }

    @Provides
    @Singleton
    @Named("weatherapi")
    fun provideWeatherApiClient(json: Json): HttpClient = HttpClient(Android) {
        apply(createBaseHttpClient(json))
        defaultRequest {
            url(WEATHERAPI_BASE_URL)
        }
    }
}