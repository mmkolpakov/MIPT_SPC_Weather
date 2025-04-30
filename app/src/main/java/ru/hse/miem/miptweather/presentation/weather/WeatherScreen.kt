package ru.hse.miem.miptweather.presentation.weather

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import ru.hse.miem.miptweather.R
import ru.hse.miem.miptweather.domain.model.*
import ru.hse.miem.miptweather.presentation.common.*
import ru.hse.miem.miptweather.presentation.components.WeatherScreenSkeleton
import ru.hse.miem.miptweather.presentation.settings.PressureUnit
import ru.hse.miem.miptweather.presentation.settings.SpeedUnit
import ru.hse.miem.miptweather.presentation.settings.TemperatureUnit
import ru.hse.miem.miptweather.presentation.theme.WeatherTheme
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel(),
    onNavigateToLocationManagement: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentError = uiState.errorState
    var errorShown by remember { mutableStateOf<ErrorType?>(null) }

    LaunchedEffect(uiState.requiresLocationSelection) {
        if (uiState.requiresLocationSelection) {
            onNavigateToLocationManagement()
        }
    }

    LaunchedEffect(currentError) {
        if (currentError != null && currentError != errorShown) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val message = getErrorMessage(currentError, context)
                val actionLabel = if (currentError is ErrorType.Network && currentError.isConnectivityIssue) {
                    context.getString(R.string.settings_action)
                } else {
                    context.getString(android.R.string.ok)
                }

                val result = snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short,
                    actionLabel = actionLabel
                )
                if (result == SnackbarResult.ActionPerformed && currentError is ErrorType.Network && currentError.isConnectivityIssue) {
                }
                errorShown = currentError
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            WeatherTopAppBar(
                title = uiState.location?.getDisplayName() ?: stringResource(R.string.app_name),
                onNavigateToLocationManagement = onNavigateToLocationManagement,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        floatingActionButton = {
            if (!uiState.isInitialLoading && (uiState.weatherData != null || uiState.errorState != null)) {
                RefreshFab(
                    onClick = viewModel::refreshWeatherData,
                    isLoading = uiState.isLoading
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val pullRefreshState = rememberPullToRefreshState()

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = uiState.isLoading && !uiState.isInitialLoading && uiState.weatherData != null,
                    onRefresh = viewModel::refreshWeatherData)
        ) {
            WeatherScreenContent(
                uiState = uiState,
                onRetry = viewModel::refreshWeatherData,
                onSelectLocation = onNavigateToLocationManagement,
                modifier = Modifier.fillMaxSize()
            )

            Indicator(
                isRefreshing = uiState.isLoading && !uiState.isInitialLoading && uiState.weatherData != null,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun WeatherScreenContent(
    modifier: Modifier = Modifier,
    uiState: WeatherUiState,
    onRetry: () -> Unit,
    onSelectLocation: () -> Unit
) {
    val spacing = WeatherTheme.spacing

    Crossfade(targetState = uiState, label = "WeatherScreenContentState") { state ->
        when {
            state.isInitialLoading && state.location == null -> {
                InitialLoadingState(modifier.padding(spacing.default))
            }
            state.isLoading && state.weatherData == null -> {
                WeatherScreenSkeleton(modifier = modifier.padding(spacing.default))
            }
            state.errorState != null && state.weatherData == null -> {
                ErrorState(
                    errorType = state.errorState,
                    onRetry = onRetry,
                    modifier = modifier.padding(spacing.default)
                )
            }
            state.weatherData != null -> {
                WeatherDataContent(
                    weatherData = state.weatherData,
                    analysis = state.analysis,
                    units = UnitPreferences(state.temperatureUnit, state.speedUnit, state.pressureUnit),
                    modifier = modifier.padding(horizontal = spacing.default)
                )
            }
            state.location == null -> {
                EmptyState(
                    onSelectLocation = onSelectLocation,
                    modifier = modifier.padding(spacing.default)
                )
            }
            else -> {
                WeatherScreenSkeleton(modifier = modifier.padding(spacing.default))
            }
        }
    }
}

@Composable
private fun InitialLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(WeatherTheme.spacing.default))
            Text(
                text = stringResource(R.string.loading_initial_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherTopAppBar(
    title: String,
    onNavigateToLocationManagement: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val cdLocationTitle = stringResource(R.string.cd_current_location_title, title)
    val cdManageLocations = stringResource(R.string.cd_manage_locations)
    val cdOpenSettings = stringResource(R.string.cd_open_settings)

    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { contentDescription = cdLocationTitle }
            )
        },
        actions = {
            IconButton(
                onClick = onNavigateToLocationManagement,
                modifier = Modifier.semantics { contentDescription = cdManageLocations }
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.semantics { contentDescription = cdOpenSettings }
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }
        }
    )
}

@Composable
private fun RefreshFab(onClick: () -> Unit, isLoading: Boolean) {
    val cdRefresh = stringResource(R.string.cd_refresh_weather)
    val cdLoading = stringResource(R.string.cd_loading_weather)

    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = if (isLoading) cdLoading else cdRefresh
        }
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "RefreshFabContent"
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null)
            }
        }
    }
}

@Composable
fun ErrorState(
    errorType: ErrorType,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = WeatherTheme.spacing
    val context = LocalContext.current
    val errorMessage = getErrorMessage(errorType, context)
    val errorTitle = stringResource(R.string.error_title)
    val cdErrorState = stringResource(R.string.cd_error_state, errorMessage)
    val cdRetry = stringResource(R.string.cd_retry_loading)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.default)
            .semantics { contentDescription = cdErrorState },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(spacing.default))
        Text(
            errorTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = spacing.medium)
        )
        Spacer(modifier = Modifier.height(spacing.default))
        Button(
            onClick = onRetry,
            modifier = Modifier.semantics { contentDescription = cdRetry },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
private fun EmptyState(onSelectLocation: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = WeatherTheme.spacing
    val strTitle = stringResource(R.string.empty_state_title)
    val strMessage = stringResource(R.string.empty_state_message)
    val strButton = stringResource(R.string.action_select_location)
    val cdEmptyState = stringResource(R.string.cd_empty_state)
    val cdSelectLocation = stringResource(R.string.cd_select_location)


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.default)
            .semantics { contentDescription = cdEmptyState },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.LocationSearching,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(spacing.default))
        Text(strTitle, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            strMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.medium)
        )
        Spacer(modifier = Modifier.height(spacing.default))
        Button(
            onClick = onSelectLocation,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = cdSelectLocation }
        ) {
            Icon(Icons.Default.AddLocationAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(spacing.small))
            Text(strButton)
        }
    }
}

@Composable
fun WeatherDataContent(
    weatherData: WeatherData,
    analysis: WeatherAnalysis?,
    units: UnitPreferences,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val spacing = WeatherTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.default)
    ) {
        Spacer(modifier = Modifier.height(spacing.small))
        CurrentWeatherCard(weatherData, units)
        HourlyForecastRow(weatherData.hourly, units)
        analysis?.let { WeatherAnalysisCard(it, units) }
        if (weatherData.daily.isNotEmpty()) {
            WeatherChartsScreen(daily = weatherData.daily, modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp))
            DailyForecastList(weatherData, units)
        }
        Spacer(modifier = Modifier.height(spacing.default))
    }
}

@Composable
fun CurrentWeatherCard(weatherData: WeatherData, units: UnitPreferences) {
    val now = Clock.System.now()
    val systemTz = remember { TimeZone.currentSystemDefault() }
    val currentHourData = remember(weatherData.hourly, systemTz) {
        weatherData.hourly.minByOrNull {
            kotlin.math.abs(it.dateTime.toInstant(systemTz).toEpochMilliseconds() - now.toEpochMilliseconds())
        }
    } ?: weatherData.hourly.firstOrNull()

    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations
    val weatherColors = WeatherTheme.weatherColors
    val thresholds = WeatherTheme.tempThresholds

    if (currentHourData == null) return

    val tempCelsius = if (units.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
        (currentHourData.temperature - 32f) * 5f / 9f
    } else {
        currentHourData.temperature
    }

    val temperatureColor = remember(tempCelsius, thresholds) {
        when {
            tempCelsius >= thresholds.hot -> weatherColors.hot
            tempCelsius >= thresholds.warm -> weatherColors.warm
            tempCelsius >= thresholds.mild -> weatherColors.mild
            tempCelsius >= thresholds.cool -> weatherColors.cool
            tempCelsius <= thresholds.cold -> weatherColors.cold
            else -> weatherColors.freezing
        }
    }

    val formattedTemp = formatTemperature(currentHourData.temperature, units.temperatureUnit)
    val formattedFeelsLike = currentHourData.feelsLike?.let { formatTemperature(it, units.temperatureUnit) }
    val conditionDescription = getWeatherDescription(currentHourData.weatherCondition)

    val cardDescription = stringResource(
        R.string.cd_current_weather_card,
        formattedTemp,
        conditionDescription,
        formattedFeelsLike ?: stringResource(R.string.not_available_short)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = cardDescription },
        elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium)
    ) {
        Row(
            modifier = Modifier
                .padding(spacing.default)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.5f).padding(end = spacing.small)) {
                Text(
                    currentHourData.dateTime.formatAsTime(),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(spacing.xsmall))
                Text(
                    text = formattedTemp,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = temperatureColor
                )
                formattedFeelsLike?.let {
                    Spacer(modifier = Modifier.height(spacing.xsmall))
                    Text(
                        text = stringResource(R.string.feels_like, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = conditionDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = WeatherIcons.getIconResource(currentHourData.weatherCondition)),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp)
                )
                Spacer(modifier = Modifier.height(spacing.small))
                AdaptiveMetricGrid(currentHourData, units)
            }
        }
    }
}


@Composable
private fun AdaptiveMetricGrid(hourlyWeather: HourlyWeather, units: UnitPreferences) {
    val spacing = WeatherTheme.spacing

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        maxItemsInEachRow = 2
    ) {
        hourlyWeather.windSpeed?.let {
            val formattedSpeed = formatSpeed(it, units.speedUnit)
            InfoChip(
                icon = Icons.Outlined.Air,
                text = formattedSpeed,
                contentDescription = stringResource(R.string.cd_wind_speed, formattedSpeed)
            )
        }
        hourlyWeather.humidity?.let {
            val formattedHumidity = stringResource(R.string.humidity_format, it)
            InfoChip(
                icon = Icons.Outlined.WaterDrop,
                text = formattedHumidity,
                contentDescription = stringResource(R.string.cd_humidity, it)
            )
        }
        hourlyWeather.pressure?.let {
            val formattedPressure = formatPressure(it, units.pressureUnit)
            InfoChip(
                icon = Icons.Outlined.Speed,
                text = formattedPressure,
                contentDescription = stringResource(R.string.cd_pressure, formattedPressure)
            )
        }
        hourlyWeather.uvIndex?.let { uv ->
            val uvDesc = uv.roundToInt().toString()
            InfoChip(
                icon = Icons.Outlined.WbSunny,
                text = stringResource(R.string.uv_index_format, uvDesc),
                contentDescription = stringResource(R.string.cd_uv_index, uvDesc)
            )
        }
        hourlyWeather.precipitation?.takeIf { it > 0 }?.let { precip ->
            val formattedPrecip = stringResource(R.string.precipitation_format_mm, "%.1f".format(Locale.US, precip))
            InfoChip(
                icon = Icons.Outlined.Umbrella,
                text = formattedPrecip,
                contentDescription = stringResource(R.string.cd_precipitation, formattedPrecip)
            )
        }
    }
}

@Composable
fun InfoChip(
    icon: ImageVector,
    text: String,
    contentDescription: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}


@Composable
fun HourlyForecastRow(hourly: List<HourlyWeather>, units: UnitPreferences) {
    if (hourly.isEmpty()) return

    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations
    val title = stringResource(R.string.title_hourly_forecast)
    val cdHourlyList = stringResource(R.string.cd_hourly_forecast_list)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = spacing.small)
        )
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium),
            modifier = Modifier.semantics { contentDescription = cdHourlyList }
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = spacing.default, vertical = spacing.small),
                horizontalArrangement = Arrangement.spacedBy(spacing.default)
            ) {
                items(hourly, key = { it.dateTime.toString() }) { hour ->
                    HourlyForecastItem(hour, units)
                }
            }
        }
    }
}

@Composable
fun HourlyForecastItem(hour: HourlyWeather, units: UnitPreferences) {
    val spacing = WeatherTheme.spacing
    val time = hour.dateTime.formatAsTime()
    val temp = formatTemperature(hour.temperature, units.temperatureUnit)
    val conditionDesc = getWeatherDescription(hour.weatherCondition)
    val iconRes = WeatherIcons.getIconResource(hour.weatherCondition)

    val cdItem = stringResource(
        R.string.cd_hourly_item,
        time,
        temp,
        conditionDesc
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .padding(horizontal = spacing.xsmall)
            .semantics(mergeDescendants = true) { contentDescription = cdItem }
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacing.xsmall))
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(spacing.xsmall))
        Text(
            text = temp,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
fun WeatherAnalysisCard(analysis: WeatherAnalysis, units: UnitPreferences) {
    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations
    val weatherColors = WeatherTheme.weatherColors

    val minTempFormatted = formatTemperature(analysis.minTemperature.temperature, units.temperatureUnit)
    val maxTempFormatted = formatTemperature(analysis.maxTemperature.temperature, units.temperatureUnit)
    val avgTempFormatted = formatTemperature(analysis.avgTemperature, units.temperatureUnit)
    val totalPrecipFormatted = analysis.totalPrecipitation?.takeIf { it > 0 }?.roundToInt()?.toString()

    val cdAnalysisCard = stringResource(R.string.cd_weather_analysis_card)


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = cdAnalysisCard },
        elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium)
    ) {
        Column(modifier = Modifier.padding(spacing.default)) {
            Text(
                stringResource(R.string.weather_analysis_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(spacing.default))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AnalysisItem(
                    icon = Icons.Default.Thermostat,
                    label = stringResource(R.string.analysis_min_temp),
                    value = minTempFormatted,
                    date = analysis.minTemperature.date.formatAsDayMonth(),
                    iconTint = weatherColors.cold
                )
                AnalysisItem(
                    icon = Icons.Default.LocalFireDepartment,
                    label = stringResource(R.string.analysis_max_temp),
                    value = maxTempFormatted,
                    date = analysis.maxTemperature.date.formatAsDayMonth(),
                    iconTint = weatherColors.hot
                )
            }
            Spacer(modifier = Modifier.height(spacing.default))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(spacing.default))

            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Text(
                    stringResource(R.string.analysis_avg_temp_full, avgTempFormatted),
                    style = MaterialTheme.typography.bodyMedium
                )
                totalPrecipFormatted?.let {
                    Text(
                        stringResource(R.string.analysis_total_precip_full_mm, it),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                analysis.windyDays?.takeIf { it > 0 }?.let {
                    Text(
                        stringResource(R.string.analysis_windy_days_full, it),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                analysis.overcastDays?.takeIf { it > 0 }?.let {
                    Text(
                        stringResource(R.string.analysis_overcast_days_full, it),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisItem(
    icon: ImageVector,
    label: String,
    value: String,
    date: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    val spacing = WeatherTheme.spacing
    val itemDescription = "$label: $value${date?.let { ", $it" } ?: ""}"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 70.dp, max = 110.dp)
            .padding(horizontal = spacing.xsmall)
            .semantics(mergeDescendants = true) { contentDescription = itemDescription }
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(spacing.xsmall))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        date?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DailyForecastList(weatherData: WeatherData, units: UnitPreferences) {
    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.getDefault()) }
    val title = stringResource(R.string.title_daily_forecast)
    val cdDailyList = stringResource(R.string.cd_daily_forecast_list_days, weatherData.daily.size)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = spacing.small, top = spacing.default)
        )
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium),
            modifier = Modifier.semantics { contentDescription = cdDailyList }
        ) {
            Column(modifier = Modifier.padding(vertical = spacing.small)) {
                weatherData.daily.forEachIndexed { index, daily ->
                    DailyForecastItem(
                        daily = daily,
                        units = units,
                        formatter = dayFormatter
                    )
                    if (index < weatherData.daily.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.default))
                    }
                }
            }
        }
    }
}

@Composable
fun DailyForecastItem(daily: DailyWeather, units: UnitPreferences, formatter: DateTimeFormatter) {
    val spacing = WeatherTheme.spacing
    val weatherColors = WeatherTheme.weatherColors
    val thresholds = WeatherTheme.tempThresholds

    val minTempCelsius = if (units.temperatureUnit == TemperatureUnit.FAHRENHEIT) (daily.minTemperature - 32f) * 5f / 9f else daily.minTemperature
    val maxTempCelsius = if (units.temperatureUnit == TemperatureUnit.FAHRENHEIT) (daily.maxTemperature - 32f) * 5f / 9f else daily.maxTemperature

    val minTempColor = remember(minTempCelsius, thresholds) {
        when {
            minTempCelsius >= thresholds.hot -> weatherColors.hot
            minTempCelsius >= thresholds.warm -> weatherColors.warm
            minTempCelsius >= thresholds.mild -> weatherColors.mild
            minTempCelsius >= thresholds.cool -> weatherColors.cool
            minTempCelsius <= thresholds.cold -> weatherColors.cold
            else -> weatherColors.freezing
        }
    }

    val maxTempColor = remember(maxTempCelsius, thresholds) {
        when {
            maxTempCelsius >= thresholds.hot -> weatherColors.hot
            maxTempCelsius >= thresholds.warm -> weatherColors.warm
            maxTempCelsius >= thresholds.mild -> weatherColors.mild
            maxTempCelsius >= thresholds.cool -> weatherColors.cool
            maxTempCelsius <= thresholds.cold -> weatherColors.cold
            else -> weatherColors.freezing
        }
    }

    val formattedMinTemp = formatTemperature(daily.minTemperature, units.temperatureUnit)
    val formattedMaxTemp = formatTemperature(daily.maxTemperature, units.temperatureUnit)
    val conditionDesc = getWeatherDescription(daily.weatherCondition)
    val dateFormatted = daily.date.format(formatter)
    val popValue = daily.pop?.takeIf { it > 0 }

    val description = stringResource(
        R.string.cd_daily_forecast_item,
        dateFormatted,
        formattedMinTemp,
        formattedMaxTemp,
        conditionDesc,
        popValue ?: 0
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.default, vertical = spacing.default)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(2f)
                .padding(end = spacing.small)
        ) {
            Image(
                painter = painterResource(id = WeatherIcons.getIconResource(daily.weatherCondition)),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = spacing.default)
            )
            Column {
                Text(
                    dateFormatted,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    conditionDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.2f),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                formattedMaxTemp,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = maxTempColor
            )
            Text(
                " / ",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.xsmall)
            )
            Text(
                formattedMinTemp,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = minTempColor
            )
        }

        Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
            val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            popValue?.let { pop ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = spacing.small)
                ) {
                    val popColor = remember(pop) {
                        when {
                            pop >= 70 -> weatherColors.rainy
                            pop >= 40 -> Color(0xFF42A5F5)
                            else -> color
                        }
                    }
                    Icon(
                        Icons.Outlined.WaterDrop,
                        contentDescription = stringResource(R.string.cd_pop_icon),
                        modifier = Modifier.size(14.dp),
                        tint = popColor
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(id = R.string.percent_format, pop),
                        style = MaterialTheme.typography.bodySmall,
                        color = popColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun getErrorMessage(errorType: ErrorType, context: Context): String {
    return when (errorType) {
        is ErrorType.Network -> if (errorType.isConnectivityIssue) context.getString(R.string.error_network_connectivity) else context.getString(R.string.error_network_generic, errorType.message)
        is ErrorType.LocationNotFound -> context.getString(R.string.error_location_not_found, errorType.message)
        is ErrorType.DataProcessing -> context.getString(R.string.error_data_processing, errorType.message)
        is ErrorType.Unknown -> context.getString(R.string.error_unknown, errorType.message)
    }
}