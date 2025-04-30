package ru.hse.miem.miptweather.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.hse.miem.miptweather.navigation.AppNavigation
import ru.hse.miem.miptweather.presentation.theme.MIPTWeatherTheme

@Composable
fun MiptWeatherApp() {
    MIPTWeatherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation()
        }
    }
}