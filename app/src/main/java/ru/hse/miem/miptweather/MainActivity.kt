package ru.hse.miem.miptweather

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.presentation.MiptWeatherApp
import ru.hse.miem.miptweather.presentation.weather.WeatherViewModel
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val weatherViewModel: WeatherViewModel by viewModels()

    companion object {
        const val WIDGET_LOCATION_LAT_KEY = "widget_locationLatKey"
        const val WIDGET_LOCATION_LON_KEY = "widget_locationLonKey"
        private const val TAG = "MainActivity"
        private const val COORDINATE_COMPARISON_TOLERANCE = 0.00001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called. Initial Intent: ${intent?.action} ${intent?.extras}")
        enableEdgeToEdge()
        lifecycleScope.launch {
            handleIntent(intent)
        }

        setContent {
            LaunchedEffect(intent) {
                Log.d(TAG, "LaunchedEffect in setContent for initial intent.")
            }

            MiptWeatherApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent called. New Intent: ${intent.action} ${intent.extras}")
        lifecycleScope.launch {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val extras = intent?.extras
        if (extras == null) {
            Log.d(TAG, "handleIntent: Intent or extras are null.")
            return
        }

        if (extras.containsKey(WIDGET_LOCATION_LAT_KEY) && extras.containsKey(WIDGET_LOCATION_LON_KEY)) {
            val latitude = extras.getDouble(WIDGET_LOCATION_LAT_KEY)
            val longitude = extras.getDouble(WIDGET_LOCATION_LON_KEY)
            Log.d(TAG, "handleIntent: Received location from intent: Lat=$latitude, Lon=$longitude")

            val locationFromWidget = Location(
                latitude = latitude,
                longitude = longitude
            )

            val currentLocation = weatherViewModel.uiState.value.location

            if (currentLocation == null ||
                !areCoordinatesEqual(currentLocation.latitude, locationFromWidget.latitude) ||
                !areCoordinatesEqual(currentLocation.longitude, locationFromWidget.longitude)) {

                Log.i(TAG, "Setting new location from widget: $locationFromWidget")
                weatherViewModel.setLocation(locationFromWidget)
            } else {
                Log.d(TAG, "Location from widget ($latitude, $longitude) is the same as current, skipping update.")
            }
        } else {
            Log.d(TAG, "handleIntent: Intent extras do not contain required location keys.")
        }
    }

    private fun areCoordinatesEqual(coord1: Double, coord2: Double, tolerance: Double = COORDINATE_COMPARISON_TOLERANCE): Boolean {
        return abs(coord1 - coord2) < tolerance
    }
}