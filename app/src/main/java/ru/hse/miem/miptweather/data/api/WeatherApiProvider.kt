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
import ru.hse.miem.miptweather.data.dto.WeatherApiResponse
import ru.hse.miem.miptweather.data.mapper.toWeatherData
import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.model.WeatherCapability
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.domain.util.Result
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Named

class WeatherApiProvider @Inject constructor(
    @Named("weatherapi") private val client: HttpClient,
    @Named("WeatherApiKey") private val apiKey: String
) : WeatherProvider {
    override val id: String = "weatherapi"
    override val name: String = "WeatherAPI.com"
    override val capabilities = setOf(
        WeatherCapability.CURRENT_WEATHER,
        WeatherCapability.HOURLY_FORECAST,
        WeatherCapability.DAILY_FORECAST,
        WeatherCapability.HISTORICAL_DATA,
        WeatherCapability.UV_INDEX,
        WeatherCapability.FEELS_LIKE,
        WeatherCapability.HUMIDITY,
        WeatherCapability.PRESSURE,
        WeatherCapability.WIND,
        WeatherCapability.PRECIPITATION,
        WeatherCapability.CLOUDINESS
    )
    override val priority: Int = 3

    private companion object { const val TAG = "WeatherApiProvider" }

    override suspend fun getWeatherData(
        location: Location,
        dateRange: DateRange
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.e(TAG, "API key is missing.")
            return@withContext Result.Error(InvalidApiKeyException())
        }

        Log.d(TAG, "Requesting weather history for ${location.latitude},${location.longitude}, date: ${dateRange.startDate}")

        try {
            val response = client.get("history.json") {
                parameter("key", apiKey)
                parameter("q", "${location.latitude},${location.longitude}")
                parameter("dt", dateRange.startDate.toString())
            }

            if (response.status.isSuccess()) {
                val data = response.body<WeatherApiResponse>()
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
            HttpStatusCode.Forbidden.value -> RateLimitExceededException(message ?: "Forbidden / Quota Exceeded", cause)
            HttpStatusCode.NotFound.value -> BadRequestException(message ?: "Not Found (check location/endpoint)", cause)
            else -> ApiException("API Error $code: ${message ?: "Unknown API error"}", code, cause)
        }
    }
}