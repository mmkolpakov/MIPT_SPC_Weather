package ru.hse.miem.miptweather.data.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.hse.miem.miptweather.data.dto.OpenWeatherMapResponse
import ru.hse.miem.miptweather.data.mapper.toWeatherData
import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.model.WeatherCapability
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.domain.util.Result
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Named

class OpenWeatherMapProvider @Inject constructor(
    @Named("openweathermap") private val client: HttpClient,
    @Named("OpenWeatherMapApiKey") private val apiKey: String
) : WeatherProvider {
    override val id: String = "openweathermap"
    override val name: String = "OpenWeatherMap"
    override val capabilities = setOf(
        WeatherCapability.CURRENT_WEATHER,
        WeatherCapability.HOURLY_FORECAST, // До 48 часов
        WeatherCapability.DAILY_FORECAST,  // До 8 дней
        WeatherCapability.UV_INDEX,
        WeatherCapability.FEELS_LIKE,
        WeatherCapability.HUMIDITY,
        WeatherCapability.PRESSURE,
        WeatherCapability.WIND,
        WeatherCapability.PRECIPITATION,
        WeatherCapability.CLOUDINESS
    )
    override val priority: Int = 5

    private companion object { const val TAG = "OpenWeatherMapProvider" }

    override suspend fun getWeatherData(
        location: Location,
        dateRange: DateRange
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.e(TAG, "API key is missing.")
            return@withContext Result.Error(InvalidApiKeyException())
        }

        Log.d(TAG, "Requesting OWM OneCall for ${location.latitude},${location.longitude}")

        try {
            val response = client.get("onecall") {
                parameter("lat", location.latitude)
                parameter("lon", location.longitude)
                parameter("units", "metric")
                parameter("exclude", "minutely,alerts")
                parameter("appid", apiKey)
            }

            if (response.status.isSuccess()) {
                val data = response.body<OpenWeatherMapResponse>()
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                Result.Success(data.toWeatherData(location, dateRange, id, now))
            } else {
                Log.w(TAG, "API request failed: ${response.status}")
                Result.Error(mapHttpStatusCodeToException(response.status.value, response.body()))
            }
        } catch (e: ClientRequestException) {
            Log.e(TAG, "Client request failed: ${e.response.status}", e)
            Result.Error(mapHttpStatusCodeToException(e.response.status.value, e.message, e))
        } catch (e: ServerResponseException) {
            Log.e(TAG, "Server error: ${e.response.status}", e)
            Result.Error(ServerErrorException(code = e.response.status.value, cause = e))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network error: No internet connection.", e)
            Result.Error(NoConnectivityException(cause = e))
        } catch (e: ConnectTimeoutException) {
            Log.e(TAG, "Network error: Connection timed out.", e)
            Result.Error(TimeoutException(cause = e))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Network error: Socket timed out.", e)
            Result.Error(TimeoutException(cause = e))
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred", e)
            Result.Error(ApiException("Unexpected error: ${e.localizedMessage}", cause = e))
        }
    }

    private fun mapHttpStatusCodeToException(code: Int, message: String? = null, cause: Throwable? = null): ApiException {
        return when (code) {
            HttpStatusCode.BadRequest.value -> BadRequestException(message ?: "Bad request", cause)
            HttpStatusCode.Unauthorized.value -> InvalidApiKeyException(message ?: "Unauthorized", cause)
            HttpStatusCode.NotFound.value -> BadRequestException(message ?: "Not Found (check location/endpoint)", cause)
            HttpStatusCode.TooManyRequests.value -> RateLimitExceededException(message ?: "Rate limit exceeded", cause)
            else -> ApiException("API Error $code: ${message ?: "Unknown API error"}", code, cause)
        }
    }
}