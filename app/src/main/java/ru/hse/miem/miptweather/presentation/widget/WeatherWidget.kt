package ru.hse.miem.miptweather.presentation.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.hse.miem.miptweather.MainActivity
import ru.hse.miem.miptweather.R
import ru.hse.miem.miptweather.domain.model.DailyWeather
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.model.UnitPreferences
import ru.hse.miem.miptweather.domain.model.WeatherCondition
import ru.hse.miem.miptweather.domain.model.WeatherData
import ru.hse.miem.miptweather.presentation.common.*
import ru.hse.miem.miptweather.presentation.settings.PressureUnit
import ru.hse.miem.miptweather.presentation.settings.SpeedUnit
import ru.hse.miem.miptweather.presentation.settings.TemperatureUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.glance.layout.Alignment

private val locationLatKey = ActionParameters.Key<Double>("widget_locationLatKey")
private val locationLonKey = ActionParameters.Key<Double>("widget_locationLonKey")

class WeatherWidget : GlanceAppWidget() {

    companion object {
        val KEY_WEATHER_DATA = stringPreferencesKey("widget_weather_data")
        val KEY_ERROR_MESSAGE = stringPreferencesKey("widget_error_message")
        val KEY_LAST_UPDATE_TIME = longPreferencesKey("widget_last_update_time")

        private val SIZE_COMPACT = DpSize(80.dp, 80.dp)
        private val SIZE_STANDARD = DpSize(160.dp, 100.dp)
        private val SIZE_EXPANDED = DpSize(240.dp, 120.dp)
        private val SIZE_INFORMATIVE = DpSize(300.dp, 150.dp)
    }

    sealed class WidgetState {
        data object Loading : WidgetState()
        data class Success(val data: WeatherData, val lastUpdateMillis: Long?) : WidgetState()
        data class Error(val message: String, val lastUpdateMillis: Long?) : WidgetState()
        data object NoData : WidgetState()
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_COMPACT, SIZE_STANDARD, SIZE_EXPANDED, SIZE_INFORMATIVE)
    )

    private val jsonConfig = Json { ignoreUnknownKeys = true; isLenient = true }
    private val updateAction = actionRunCallback<UpdateWidgetAction>()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            var widgetState by remember { mutableStateOf<WidgetState>(WidgetState.Loading) }

            LaunchedEffect(prefs) {
                widgetState = withContext(Dispatchers.Default) {
                    val weatherJson = prefs[KEY_WEATHER_DATA]
                    val errorMsg = prefs[KEY_ERROR_MESSAGE]
                    val lastUpdate = prefs[KEY_LAST_UPDATE_TIME]

                    when {
                        errorMsg != null -> WidgetState.Error(errorMsg, lastUpdate)
                        weatherJson != null -> {
                            try {
                                val data = jsonConfig.decodeFromString<WeatherData>(weatherJson)
                                WidgetState.Success(data, lastUpdate)
                            } catch (e: Exception) {
                                WidgetState.Error(context.getString(R.string.widget_error_data), lastUpdate)
                            }
                        }
                        prefs.contains(KEY_LAST_UPDATE_TIME) || prefs.contains(KEY_ERROR_MESSAGE) -> WidgetState.NoData
                        else -> WidgetState.Loading
                    }
                }
            }

            val size = LocalSize.current
            val widgetType = determineWidgetType(size)
            val currentLoc = (widgetState as? WidgetState.Success)?.data?.location

            val openAppAction = actionStartActivity<MainActivity>(
                parameters = if (currentLoc != null) {
                    actionParametersOf(
                        locationLatKey to currentLoc.latitude,
                        locationLonKey to currentLoc.longitude
                    )
                } else {
                    actionParametersOf()
                }
            )

            GlanceTheme {
                WidgetScaffold(
                    state = widgetState,
                    type = widgetType,
                    onWidgetClick = openAppAction,
                    onRefreshClick = updateAction,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(16.dp)
                )
            }
        }
    }

    private fun determineWidgetType(size: DpSize): WidgetType {
        val minDimension = minOf(size.width, size.height)
        return when {
            minDimension < SIZE_STANDARD.width -> WidgetType.COMPACT
            minDimension < SIZE_EXPANDED.width -> WidgetType.STANDARD
            minDimension < SIZE_INFORMATIVE.width -> WidgetType.EXPANDED
            else -> WidgetType.INFORMATIVE
        }
    }
}

enum class WidgetType { COMPACT, STANDARD, EXPANDED, INFORMATIVE }

@Composable
private fun WidgetScaffold(
    state: WeatherWidget.WidgetState,
    type: WidgetType,
    onWidgetClick: androidx.glance.action.Action,
    onRefreshClick: androidx.glance.action.Action,
    modifier: GlanceModifier
) {
    Box(modifier = modifier.clickable(onWidgetClick)) {
        when (state) {
            is WeatherWidget.WidgetState.Loading -> LoadingWidget()
            is WeatherWidget.WidgetState.Success -> WidgetContentByType(type, state.data, state.lastUpdateMillis)
            is WeatherWidget.WidgetState.Error -> ErrorWidget(state.message, state.lastUpdateMillis)
            is WeatherWidget.WidgetState.NoData -> NoDataWidget()
        }

        if (state !is WeatherWidget.WidgetState.Loading) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = stringResource(R.string.widget_refresh_cd),
                modifier = GlanceModifier
                    .padding(6.dp)
                    .size(18.dp)
                    .clickable(onRefreshClick)
            )
        }
    }
}


@Composable
private fun WidgetContentByType(type: WidgetType, data: WeatherData, lastUpdateMillis: Long?) {
    val context = LocalContext.current
    val updateTimeStr = remember(lastUpdateMillis) { formatUpdateTime(lastUpdateMillis, context) }
    val units = remember { UnitPreferences() }

    Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
        when (type) {
            WidgetType.COMPACT -> CompactWidgetContent(data, units)
            WidgetType.STANDARD -> StandardWidgetContent(data, units)
            WidgetType.EXPANDED -> ExpandedWidgetContent(data, units)
            WidgetType.INFORMATIVE -> InformativeWidgetContent(data, units)
        }

        updateTimeStr?.let {
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = it,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                ),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 1
            )
        }
    }
}

@Composable
fun LoadingWidget() {
    val widgetLoading = stringResource(R.string.widget_loading)
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp)
            .semantics { contentDescription = widgetLoading },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = GlanceTheme.colors.primary)
    }
}

@Composable
fun NoDataWidget() {
    val widgetNoDataA11y = stringResource(R.string.widget_no_data_a11y)
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp)
            .semantics { contentDescription = widgetNoDataA11y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_cloud_off),
            contentDescription = null,
            modifier = GlanceModifier.size(32.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            stringResource(R.string.widget_no_data),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
        )
        Text(
            stringResource(R.string.widget_tap_to_update_short),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
    }
}

@Composable
fun ErrorWidget(errorMessage: String, lastUpdateMillis: Long?) {
    val context = LocalContext.current
    val updateTimeStr = remember(lastUpdateMillis) { formatUpdateTime(lastUpdateMillis, context) }
    val widgetErrorA11y = stringResource(R.string.widget_error_a11y, errorMessage)

    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp)
            .background(GlanceTheme.colors.errorContainer)
            .semantics { contentDescription = widgetErrorA11y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_error_outline),
            contentDescription = null,
            modifier = GlanceModifier.size(32.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.error)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            stringResource(R.string.widget_error_short),
            style = TextStyle(
                color = GlanceTheme.colors.error,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
        )
        Text(
            errorMessage,
            style = TextStyle(color = GlanceTheme.colors.onErrorContainer, fontSize = 12.sp),
            maxLines = 2,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        updateTimeStr?.let {
            Text(
                text = it,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                ),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CompactWidgetContent(weatherData: WeatherData, units: UnitPreferences) {
    val currentHour = weatherData.hourly.firstOrNull()
    val currentTemp = currentHour?.temperature
    val condition = currentHour?.weatherCondition
    val conditionDesc = getWeatherDescription(condition)
    val formattedTemp = formatTemperature(currentTemp, units.temperatureUnit, showUnit = false)

    val widgetCompactA11y = stringResource(R.string.widget_compact_a11y, formattedTemp, conditionDesc)

    Row(
        modifier = GlanceModifier.fillMaxSize()
            .semantics() {
                contentDescription = widgetCompactA11y
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (condition != null) {
            Image(
                provider = ImageProvider(WeatherIcons.getIconResource(condition)),
                contentDescription = null,
                modifier = GlanceModifier.size(32.dp)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
        }
        Text(
            text = formattedTemp,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun StandardWidgetContent(weatherData: WeatherData, units: UnitPreferences) {
    val currentHour = weatherData.hourly.firstOrNull()
    val daily = weatherData.daily.firstOrNull()
    val locationName = weatherData.location.getDisplayName()
    val conditionDesc = getWeatherDescription(currentHour?.weatherCondition)
    val formattedTemp = formatTemperature(currentHour?.temperature, units.temperatureUnit)
    val formattedMax = formatTemperature(daily?.maxTemperature, units.temperatureUnit, showUnit = false)
    val formattedMin = formatTemperature(daily?.minTemperature, units.temperatureUnit, showUnit = false)

    val widgetStandardA11y = stringResource(R.string.widget_standard_a11y, locationName, formattedTemp, conditionDesc, formattedMax, formattedMin)

    Column(
        modifier = GlanceModifier.fillMaxSize()
            .semantics() {
                contentDescription = widgetStandardA11y
            }
    ) {
        Text(
            text = locationName,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedTemp,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                if (currentHour?.weatherCondition != null) {
                    Image(
                        provider = ImageProvider(WeatherIcons.getIconResource(currentHour.weatherCondition)),
                        contentDescription = null,
                        modifier = GlanceModifier.size(36.dp)
                    )
                }
            }

            if (daily != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.widget_max_temp_short, formattedMax),
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        stringResource(R.string.widget_min_temp_short, formattedMin),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
                    )
                }
            }
        }
        if (currentHour?.weatherCondition != null) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                conditionDesc,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun ExpandedWidgetContent(weatherData: WeatherData, units: UnitPreferences) {
    val currentHour = weatherData.hourly.firstOrNull()
    val dailyToday = weatherData.daily.firstOrNull()
    val forecastDays = weatherData.daily.drop(1).take(3)
    val dayFormatter = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val locationName = weatherData.location.getDisplayName()
    val conditionDesc = getWeatherDescription(currentHour?.weatherCondition)
    val formattedTemp = formatTemperature(currentHour?.temperature, units.temperatureUnit)
    val formattedMaxToday = formatTemperature(dailyToday?.maxTemperature, units.temperatureUnit, showUnit = false)
    val formattedMinToday = formatTemperature(dailyToday?.minTemperature, units.temperatureUnit, showUnit = false)

    val widgetExpandedA11y = stringResource(R.string.widget_expanded_a11y, locationName, formattedTemp, conditionDesc, formattedMaxToday, formattedMinToday)

    Column(
        modifier = GlanceModifier.fillMaxSize()
            .semantics() {
                contentDescription = widgetExpandedA11y
            }
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column {
                Text(
                    locationName,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    formattedTemp,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                )
                if (currentHour?.weatherCondition != null) {
                    Text(
                        conditionDesc,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (currentHour?.weatherCondition != null) {
                    Image(
                        provider = ImageProvider(WeatherIcons.getIconResource(currentHour.weatherCondition)),
                        contentDescription = null,
                        modifier = GlanceModifier.size(40.dp)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
                if (dailyToday != null) {
                    Text(
                        stringResource(R.string.widget_max_temp_short, formattedMaxToday),
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        stringResource(R.string.widget_min_temp_short, formattedMinToday),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
                    )
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(10.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (forecastDays.isNotEmpty()) {
                forecastDays.forEach { daily ->
                    DailyForecastColumn(daily, units, dayFormatter)
                }
                repeat(maxOf(0, 3 - forecastDays.size)) {
                    Spacer(GlanceModifier.defaultWeight())
                }
            } else {
                Text(stringResource(R.string.widget_no_forecast), style = TextStyle(fontSize=12.sp, color = GlanceTheme.colors.onSurfaceVariant))
            }
        }
    }
}

@Composable
fun InformativeWidgetContent(weatherData: WeatherData, units: UnitPreferences) {
    val currentHour = weatherData.hourly.firstOrNull()
    val dailyToday = weatherData.daily.firstOrNull()
    val forecastDays = weatherData.daily.drop(1).take(4)
    val dayFormatter = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val locationName = weatherData.location.getDisplayName()
    val conditionDesc = getWeatherDescription(currentHour?.weatherCondition)

    val formattedTemp = formatTemperature(currentHour?.temperature, units.temperatureUnit)
    val formattedMaxToday = formatTemperature(dailyToday?.maxTemperature, units.temperatureUnit, showUnit = false)
    val formattedMinToday = formatTemperature(dailyToday?.minTemperature, units.temperatureUnit, showUnit = false)
    val formattedHumidity = currentHour?.humidity?.let { stringResource(R.string.humidity_format, it) }
    val formattedPressure = formatPressure(currentHour?.pressure, units.pressureUnit)
    val formattedWind = formatSpeed(currentHour?.windSpeed, units.speedUnit)
    val formattedUV = currentHour?.uvIndex?.roundToInt()?.toString()
    val formattedPop = dailyToday?.pop?.let { stringResource(R.string.percent_format, it) }

    val widgetA11yBase = stringResource(R.string.widget_a11y_base, locationName, formattedTemp, conditionDesc)

    val widget_a11y_minmax = stringResource(R.string.widget_a11y_minmax, formattedMaxToday, formattedMinToday)



    Column(
        modifier = GlanceModifier.fillMaxSize()
            .semantics() {
                contentDescription = listOfNotNull(
                    widgetA11yBase,
                    formattedMaxToday.let { max -> formattedMinToday.let { min -> widget_a11y_minmax } },
//                    formattedHumidity?.let { stringResource(R.string.widget_a11y_humidity, it) },
//                    formattedWind?.let { stringResource(R.string.widget_a11y_wind, it) },
//                    formattedPressure?.let { stringResource(R.string.widget_a11y_pressure, it) },
//                    formattedUV?.let { stringResource(R.string.widget_a11y_uv, it) },
//                    formattedPop?.let { stringResource(R.string.widget_a11y_pop, it) }
                ).joinToString(". ")
            }
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = GlanceModifier.defaultWeight().padding(end = 6.dp)) {
                Text(locationName, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(formattedTemp, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold))
                if (dailyToday != null) {
                    Text(
                        stringResource(R.string.widget_minmax_temp, formattedMaxToday, formattedMinToday),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1
                    )
                }
                if (currentHour?.weatherCondition != null) {
                    Text(conditionDesc, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp), maxLines = 1)
                }
            }
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                formattedHumidity?.let { WidgetInfoLine(stringResource(R.string.widget_humidity_short), it) }
                formattedPressure?.let { WidgetInfoLine(stringResource(R.string.widget_pressure_short), it) }
                formattedWind?.let { WidgetInfoLine(stringResource(R.string.widget_wind_short), it) }
                formattedUV?.let { WidgetInfoLine(stringResource(R.string.widget_uv_short), it) }
                formattedPop?.let { WidgetInfoLine(stringResource(R.string.widget_pop_short), it) }
            }
        }
        Spacer(modifier = GlanceModifier.height(10.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (forecastDays.isNotEmpty()) {
                forecastDays.forEach { daily ->
                    DailyForecastColumn(daily, units, dayFormatter)
                }
                repeat(maxOf(0, 4 - forecastDays.size)) {
                    Spacer(GlanceModifier.defaultWeight())
                }
            } else {
                Text(stringResource(R.string.widget_no_forecast), style = TextStyle(fontSize=12.sp, color = GlanceTheme.colors.onSurfaceVariant))
            }
        }
    }
}

@Composable
private fun DailyForecastColumn(daily: DailyWeather, units: UnitPreferences, dayFormatter: SimpleDateFormat) {
    val instant = daily.date.atStartOfDayIn(TimeZone.UTC)
    val javaDate = Date.from(instant.toJavaInstant())
    val dayOfWeek = remember(daily.date) {
        try { dayFormatter.format(javaDate) } catch (e: Exception) { "N/A" }
    }
    val maxTemp = formatTemperature(daily.maxTemperature, units.temperatureUnit, showUnit = false)
    val minTemp = formatTemperature(daily.minTemperature, units.temperatureUnit, showUnit = false)
    val desc = stringResource(R.string.widget_forecast_item_a11y, dayOfWeek, maxTemp, minTemp, getWeatherDescription(daily.weatherCondition))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier
            .padding(horizontal = 2.dp)
            .semantics() {
                contentDescription = desc
            }
    ) {
        Text(
            dayOfWeek,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        if (daily.weatherCondition != null) {
            Image(
                provider = ImageProvider(WeatherIcons.getIconResource(daily.weatherCondition)),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp).padding(vertical = 2.dp)
            )
        } else {
            Spacer(modifier = GlanceModifier.height(28.dp))
        }
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            maxTemp,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        )
        Text(
            minTemp,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        )
    }
}

@Composable
private fun WidgetInfoLine(label: String, value: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = value,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

private fun formatUpdateTime(timeMillis: Long?, context: Context): String? {
    if (timeMillis == null) return null
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return context.getString(R.string.widget_last_update, sdf.format(Date(timeMillis)))
}

private fun formatTemperature(temperature: Float?, unit: TemperatureUnit, showUnit: Boolean = true): String {
    if (temperature == null || temperature.isNaN()) return "—"
    val tempValue = temperature.roundToInt()
    return when {
        !showUnit -> "$tempValue°"
        unit == TemperatureUnit.CELSIUS -> "$tempValue°C"
        unit == TemperatureUnit.FAHRENHEIT -> "$tempValue°F"
        else -> {"$tempValue°"}
    }
}

private fun formatSpeed(speed: Float?, unit: SpeedUnit): String {
    if (speed == null || speed.isNaN()) return "—"
    val speedValue = speed.roundToInt()
    return when (unit) {
        SpeedUnit.METERS_PER_SECOND -> "$speedValue m/s"
        SpeedUnit.KILOMETERS_PER_HOUR -> "$speedValue km/h"
        SpeedUnit.MILES_PER_HOUR -> "$speedValue mph"
    }
}

private fun formatPressure(pressure: Float?, unit: PressureUnit): String {
    if (pressure == null || pressure.isNaN()) return "—"
    val pressureValue = pressure.roundToInt()
    return when (unit) {
        PressureUnit.HECTOPASCALS -> "$pressureValue hPa"
        PressureUnit.MILLIMETERS_OF_MERCURY -> "$pressureValue mmHg"
        PressureUnit.INCHES_OF_MERCURY -> "%.2f".format(Locale.US, pressure) + " inHg"
    }
}

private fun getWeatherDescription(condition: WeatherCondition?): String {
    return when (condition) {
        WeatherCondition.CLEAR_SKY_DAY -> "Clear"
        WeatherCondition.CLEAR_SKY_NIGHT -> "Clear"
        WeatherCondition.FEW_CLOUDS_DAY -> "Few clouds"
        WeatherCondition.FEW_CLOUDS_NIGHT -> "Few clouds"
        WeatherCondition.SCATTERED_CLOUDS -> "Scattered clouds"
        WeatherCondition.BROKEN_CLOUDS -> "Broken clouds"
        WeatherCondition.OVERCAST_CLOUDS -> "Overcast"
        WeatherCondition.MIST -> "Mist"
        WeatherCondition.FOG -> "Fog"
        WeatherCondition.SMOKE -> "Smoke"
        WeatherCondition.HAZE -> "Haze"
        WeatherCondition.LIGHT_RAIN -> "Light rain"
        WeatherCondition.MODERATE_RAIN -> "Rain"
        WeatherCondition.HEAVY_RAIN -> "Heavy rain"
        WeatherCondition.LIGHT_SNOW -> "Light snow"
        WeatherCondition.SNOW -> "Snow"
        WeatherCondition.HEAVY_SNOW -> "Heavy snow"
        WeatherCondition.SLEET -> "Sleet"
        WeatherCondition.THUNDERSTORM -> "Thunderstorm"
        else -> "Unknown"
    }
}