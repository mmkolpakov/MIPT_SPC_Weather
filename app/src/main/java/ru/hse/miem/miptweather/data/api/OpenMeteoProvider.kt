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
import ru.hse.miem.miptweather.data.dto.OpenMeteoResponse
import ru.hse.miem.miptweather.data.mapper.toWeatherData
import ru.hse.miem.miptweather.domain.model.DateRange
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.model.WeatherCapability
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.domain.util.Result
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Named

class OpenMeteoProvider @Inject constructor(
    @Named("open-meteo") private val client: HttpClient
) : WeatherProvider {
    override val id: String = "open-meteo"
    override val name: String = "Open-Meteo"
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
    override val priority: Int = 10

    private companion object {
        const val TAG = "OpenMeteoProvider"
        val HOURLY_PARAMS = listOf(
            "temperature_2m", "relativehumidity_2m", "apparent_temperature",
            "precipitation", "weathercode", "pressure_msl", "cloudcover",
            "windspeed_10m", "winddirection_10m", "uv_index"
        ).joinToString(",")

        val DAILY_PARAMS = listOf(
            "weathercode", "temperature_2m_max", "temperature_2m_min",
            "apparent_temperature_max", "apparent_temperature_min",
            "sunrise", "sunset", "uv_index_max", "precipitation_sum",
            "windspeed_10m_max", "precipitation_probability_max"
        ).joinToString(",")
    }

    override suspend fun getWeatherData(
        location: Location,
        dateRange: DateRange
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Requesting Open-Meteo for ${location.latitude},${location.longitude}, range: $dateRange")

        try {
            val response = client.get("forecast") {
                parameter("latitude", location.latitude)
                parameter("longitude", location.longitude)
                parameter("timezone", location.timezone.ifEmpty { "auto" })
                parameter("start_date", dateRange.startDate.toString())
                parameter("end_date", dateRange.endDate.toString())
                parameter("hourly", HOURLY_PARAMS)
                parameter("daily", DAILY_PARAMS)
            }

            if (response.status.isSuccess()) {
                val data = response.body<OpenMeteoResponse>()
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
            HttpStatusCode.BadRequest.value -> BadRequestException(message ?: "Bad request (check location, dates, variables)", cause)
            else -> ApiException("API Error $code: ${message ?: "Unknown API error"}", code, cause)
        }
    }
}