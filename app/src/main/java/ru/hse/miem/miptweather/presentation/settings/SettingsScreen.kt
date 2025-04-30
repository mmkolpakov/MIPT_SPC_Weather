package ru.hse.miem.miptweather.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.hse.miem.miptweather.BuildConfig
import ru.hse.miem.miptweather.R
import ru.hse.miem.miptweather.presentation.theme.WeatherTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val cdNavBack = stringResource(R.string.cd_navigate_back)
    val cdLoading = stringResource(R.string.loading)
    val cdSettingUpdated = stringResource(R.string.setting_updated)
    val cdErrorLoading = stringResource(R.string.error_loading_settings)
    val cdRetry = stringResource(R.string.action_retry)

    LaunchedEffect(uiState.errorLoading) {
        if (uiState.errorLoading) {
            val result = snackbarHostState.showSnackbar(
                message = cdErrorLoading,
                actionLabel = cdRetry,
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retryLoadSettings()
            }
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = cdNavBack }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().semantics { contentDescription = cdLoading },
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(WeatherTheme.spacing.default),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(WeatherTheme.spacing.small))
                            Text(cdErrorLoading, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(WeatherTheme.spacing.default))
                            Button(onClick = viewModel::retryLoadSettings) {
                                Text(cdRetry)
                            }
                        }
                    }
                }
                else -> {
                    SettingsContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState,
                        onProviderSelected = { provider ->
                            viewModel.setPreferredProvider(provider)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = cdSettingUpdated,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onTemperatureUnitSelected = viewModel::setTemperatureUnit,
                        onSpeedUnitSelected = viewModel::setSpeedUnit,
                        onPressureUnitSelected = viewModel::setPressureUnit
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    uiState: SettingsUiState,
    onProviderSelected: (String) -> Unit,
    onTemperatureUnitSelected: (TemperatureUnit) -> Unit,
    onSpeedUnitSelected: (SpeedUnit) -> Unit,
    onPressureUnitSelected: (PressureUnit) -> Unit,
) {
    val spacing = WeatherTheme.spacing
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(spacing.default),
        verticalArrangement = Arrangement.spacedBy(spacing.default)
    ) {
        DataProviderCard(
            availableProviders = uiState.availableProviders,
            preferredProvider = uiState.preferredProvider,
            onProviderSelected = onProviderSelected
        )
        UnitsCard(
            selectedTempUnit = uiState.temperatureUnit,
            selectedSpeedUnit = uiState.speedUnit,
            selectedPressureUnit = uiState.pressureUnit,
            onTemperatureUnitSelected = onTemperatureUnitSelected,
            onSpeedUnitSelected = onSpeedUnitSelected,
            onPressureUnitSelected = onPressureUnitSelected
        )
        AboutAppCard()
    }
}

@Composable
private fun DataProviderCard(
    availableProviders: List<String>,
    preferredProvider: String,
    onProviderSelected: (String) -> Unit
) {
    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations
    val cdProviderSelection = stringResource(R.string.cd_provider_selection)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = cdProviderSelection },
        elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium)
    ) {
        Column(modifier = Modifier.padding(spacing.default)) {
            SettingsSectionHeader(
                icon = Icons.Default.CloudSync,
                title = stringResource(R.string.settings_data_provider)
            )
            Text(
                text = stringResource(R.string.settings_data_provider_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(spacing.small))

            Column(
                modifier = Modifier.selectableGroup()
            ) {
                availableProviders.forEach { providerId ->
                    ProviderSelectionItem(
                        providerId = providerId,
                        isSelected = preferredProvider == providerId,
                        onClick = { onProviderSelected(providerId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderSelectionItem(
    providerId: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val spacing = WeatherTheme.spacing
    val providerDisplayName = getProviderDisplayName(providerId)
    val providerDescription = getProviderDescription(providerId)

    val a11yDescription = if (isSelected) {
        stringResource(R.string.cd_provider_item_selected, providerDisplayName)
    } else {
        stringResource(R.string.cd_provider_item, providerDisplayName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = spacing.small)
            .semantics { contentDescription = a11yDescription },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(spacing.default))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = providerDisplayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = providerDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnitsCard(
    selectedTempUnit: TemperatureUnit,
    selectedSpeedUnit: SpeedUnit,
    selectedPressureUnit: PressureUnit,
    onTemperatureUnitSelected: (TemperatureUnit) -> Unit,
    onSpeedUnitSelected: (SpeedUnit) -> Unit,
    onPressureUnitSelected: (PressureUnit) -> Unit,
) {
    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations
    val cdUnitsSelection = stringResource(R.string.cd_units_selection)

    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = cdUnitsSelection },
        elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium)
    ) {
        Column(modifier = Modifier.padding(spacing.default)) {
            SettingsSectionHeader(
                icon = Icons.Outlined.Straighten,
                title = stringResource(R.string.settings_units)
            )

            UnitSelectionGroup(
                title = stringResource(R.string.settings_unit_temperature),
                options = TemperatureUnit.entries.toList(),
                selectedOption = selectedTempUnit,
                onOptionSelected = onTemperatureUnitSelected,
                optionLabel = { getTemperatureUnitLabel(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = spacing.small))
            UnitSelectionGroup(
                title = stringResource(R.string.settings_unit_speed),
                options = SpeedUnit.entries.toList(),
                selectedOption = selectedSpeedUnit,
                onOptionSelected = onSpeedUnitSelected,
                optionLabel = { getSpeedUnitLabel(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = spacing.small))
            UnitSelectionGroup(
                title = stringResource(R.string.settings_unit_pressure),
                options = PressureUnit.entries.toList(),
                selectedOption = selectedPressureUnit,
                onOptionSelected = onPressureUnitSelected,
                optionLabel = { getPressureUnitLabel(it) }
            )
        }
    }
}

@Composable
private fun <T> UnitSelectionGroup(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: @Composable (T) -> String
) {
    val spacing = WeatherTheme.spacing
    Column(Modifier.padding(vertical = spacing.small)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(top = spacing.xsmall),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                val label = optionLabel(option)
                val cdUnitOption = if (isSelected) stringResource(R.string.cd_unit_option_selected, label) else stringResource(R.string.cd_unit_option, label)
                Row(
                    Modifier
                        .selectable(
                            selected = isSelected,
                            onClick = { onOptionSelected(option) },
                            role = Role.RadioButton
                        )
                        .semantics { contentDescription = cdUnitOption }
                        .padding(vertical = spacing.xsmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { onOptionSelected(option) })
                    Spacer(Modifier.width(spacing.small))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}


@Composable
private fun AboutAppCard() {
    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val versionInfo = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    val cdAboutApp = stringResource(R.string.cd_about_app, versionInfo)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = cdAboutApp },
        elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium)
    ) {
        Column(modifier = Modifier.padding(spacing.default)) {
            SettingsSectionHeader(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_about)
            )

            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = versionInfo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = stringResource(R.string.settings_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.default))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(spacing.default))

            Text(
                text = stringResource(R.string.settings_developer),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.settings_developer_name),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.settings_copyright, currentYear.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = WeatherTheme.spacing.small)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(WeatherTheme.spacing.small))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun getProviderDisplayName(providerId: String): String = when (providerId) {
    "open-meteo" -> stringResource(R.string.provider_open_meteo)
    "openweathermap" -> stringResource(R.string.provider_openweathermap)
    "weatherapi" -> stringResource(R.string.provider_weatherapi)
    else -> providerId
}

@Composable
private fun getProviderDescription(providerId: String): String = when (providerId) {
    "open-meteo" -> stringResource(R.string.provider_open_meteo_desc)
    "openweathermap" -> stringResource(R.string.provider_openweathermap_desc)
    "weatherapi" -> stringResource(R.string.provider_weatherapi_desc)
    else -> stringResource(R.string.provider_generic_desc)
}

@Composable
private fun getTemperatureUnitLabel(unit: TemperatureUnit): String = when(unit) {
    TemperatureUnit.CELSIUS -> stringResource(R.string.unit_celsius)
    TemperatureUnit.FAHRENHEIT -> stringResource(R.string.unit_fahrenheit)
}

@Composable
private fun getSpeedUnitLabel(unit: SpeedUnit): String = when(unit) {
    SpeedUnit.METERS_PER_SECOND -> stringResource(R.string.unit_ms)
    SpeedUnit.KILOMETERS_PER_HOUR -> stringResource(R.string.unit_kmh)
    SpeedUnit.MILES_PER_HOUR -> stringResource(R.string.unit_mph)
}

@Composable
private fun getPressureUnitLabel(unit: PressureUnit): String = when(unit) {
    PressureUnit.HECTOPASCALS -> stringResource(R.string.unit_hpa)
    PressureUnit.MILLIMETERS_OF_MERCURY -> stringResource(R.string.unit_mmhg)
    PressureUnit.INCHES_OF_MERCURY -> stringResource(R.string.unit_inhg)
}