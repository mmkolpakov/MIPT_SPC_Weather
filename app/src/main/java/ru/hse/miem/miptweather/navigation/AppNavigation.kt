package ru.hse.miem.miptweather.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.presentation.location.LocationScreen
import ru.hse.miem.miptweather.presentation.location.LocationViewModel
import ru.hse.miem.miptweather.presentation.onboarding.OnboardingScreen
import ru.hse.miem.miptweather.presentation.settings.SettingsScreen
import ru.hse.miem.miptweather.presentation.weather.WeatherScreen
import ru.hse.miem.miptweather.presentation.weather.WeatherViewModel

sealed interface AppDestination {
    val route: String

    data object Onboarding : AppDestination { override val route = "onboarding" }
    data object Weather : AppDestination { override val route = "weather" }
    data object LocationManagement : AppDestination { override val route = "location" }
    data object Settings : AppDestination { override val route = "settings" }

    data object WeatherWithArgs : AppDestination {
        override val route = "weather_with_args"
        const val LAT_ARG = "latitude"
        const val LON_ARG = "longitude"
        const val NAME_ARG = "name"
        const val TZ_ARG = "timezone"
        val routeWithArgs = "$route/{$LAT_ARG}/{$LON_ARG}?$NAME_ARG={$NAME_ARG}&$TZ_ARG={$TZ_ARG}"

        val arguments = listOf(
            navArgument(LAT_ARG) { type = NavType.FloatType },
            navArgument(LON_ARG) { type = NavType.FloatType },
            navArgument(NAME_ARG) { type = NavType.StringType; nullable = true },
            navArgument(TZ_ARG) { type = NavType.StringType; nullable = true }
        )

        fun createRoute(location: Location): String {
            val encodedName = Uri.encode(location.name) ?: ""
            val encodedTz = Uri.encode(location.timezone) ?: ""
            return "$route/${location.latitude}/${location.longitude}?$NAME_ARG=$encodedName&$TZ_ARG=$encodedTz"
        }
    }
}

private const val PREFS_NAME = "app_prefs"
private const val KEY_FIRST_LAUNCH = "first_launch"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val isFirstLaunch = remember { preferences.getBoolean(KEY_FIRST_LAUNCH, true) }

    val startDestination = if (isFirstLaunch) AppDestination.Onboarding.route else AppDestination.Weather.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        addOnboardingRoute(navController, preferences)
        addWeatherRoute(navController)
        addLocationManagementRoute(navController)
        addSettingsRoute(navController)
        addWeatherWithArgsRoute(navController)
    }
}

private fun NavGraphBuilder.addOnboardingRoute(
    navController: NavHostController,
    preferences: android.content.SharedPreferences
) {
    composable(AppDestination.Onboarding.route) {
        OnboardingScreen(
            onComplete = {
                preferences.edit { putBoolean(KEY_FIRST_LAUNCH, false) }
                navController.navigate(AppDestination.Weather.route) {
                    popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                }
            }
        )
    }
}

private fun NavGraphBuilder.addWeatherRoute(navController: NavHostController) {
    composable(AppDestination.Weather.route) {
        val weatherViewModel: WeatherViewModel = hiltViewModel()
        val uiState by weatherViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.requiresLocationSelection) {
            if (uiState.requiresLocationSelection) {
                navController.navigate(AppDestination.LocationManagement.route) {
                    launchSingleTop = true
                }
            }
        }

        if (!uiState.requiresLocationSelection) {
            WeatherScreen(
                viewModel = weatherViewModel,
                onNavigateToLocationManagement = { navController.navigate(AppDestination.LocationManagement.route) },
                onNavigateToSettings = { navController.navigate(AppDestination.Settings.route) }
            )
        }
    }
}

private fun NavGraphBuilder.addLocationManagementRoute(navController: NavHostController) {
    composable(AppDestination.LocationManagement.route) {
        val viewModel: LocationViewModel = hiltViewModel()
        LocationScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onLocationSelected = { location ->
                navController.navigate(AppDestination.WeatherWithArgs.createRoute(location)) {
                    popUpTo(AppDestination.Weather.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}

private fun NavGraphBuilder.addSettingsRoute(navController: NavHostController) {
    composable(AppDestination.Settings.route) {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

private fun NavGraphBuilder.addWeatherWithArgsRoute(navController: NavHostController) {
    composable(
        route = AppDestination.WeatherWithArgs.routeWithArgs,
        arguments = AppDestination.WeatherWithArgs.arguments
    ) { backStackEntry ->
        val arguments = backStackEntry.arguments
        val latitude = arguments?.getFloat(AppDestination.WeatherWithArgs.LAT_ARG)?.toDouble()
        val longitude = arguments?.getFloat(AppDestination.WeatherWithArgs.LON_ARG)?.toDouble()
        val name = arguments?.getString(AppDestination.WeatherWithArgs.NAME_ARG) ?: ""
        val timezone = arguments?.getString(AppDestination.WeatherWithArgs.TZ_ARG) ?: ""

        val weatherViewModel: WeatherViewModel = hiltViewModel()

        LaunchedEffect(latitude, longitude) {
            if (latitude != null && longitude != null) {
                val location = Location(
                    latitude = latitude,
                    longitude = longitude,
                    name = Uri.decode(name),
                    countryCode = "",
                    timezone = Uri.decode(timezone)
                )
                if (weatherViewModel.uiState.value.location != location) {
                    weatherViewModel.setLocation(location)
                }
            } else {
                navController.popBackStack(AppDestination.Weather.route, false)
            }
        }

        WeatherScreen(
            viewModel = weatherViewModel,
            onNavigateToLocationManagement = { navController.navigate(AppDestination.LocationManagement.route) },
            onNavigateToSettings = { navController.navigate(AppDestination.Settings.route) }
        )
    }
}