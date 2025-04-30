package ru.hse.miem.miptweather.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class WeatherColors(
    val hot: Color,
    val warm: Color,
    val mild: Color,
    val cool: Color,
    val cold: Color,
    val freezing: Color,
    val rainy: Color,
    val snowy: Color,
    val stormy: Color,
    val foggy: Color
)

@Immutable
data class Spacing(
    val default: Dp = 16.dp,
    val small: Dp = 8.dp,
    val xsmall: Dp = 4.dp,
    val medium: Dp = 24.dp,
    val large: Dp = 32.dp,
    val xlarge: Dp = 48.dp
)

@Immutable
data class Elevations(
    val none: Dp = 0.dp,
    val small: Dp = 1.dp,
    val medium: Dp = 2.dp,
    val large: Dp = 4.dp,
    val extraLarge: Dp = 8.dp
)

@Immutable
data class TemperatureThresholds(
    val hot: Float = 30f,
    val warm: Float = 20f,
    val mild: Float = 10f,
    val cool: Float = 0f,
    val cold: Float = -10f
)

val LocalWeatherColors = compositionLocalOf<WeatherColors> { error("No WeatherColors provided") }
val LocalSpacing = compositionLocalOf { Spacing() }
val LocalElevations = compositionLocalOf { Elevations() }
val LocalTemperatureThresholds = compositionLocalOf { TemperatureThresholds() }

private val WeatherLightColorScheme = lightColorScheme(
    primary = Blue60,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = LightBlue40,
    onSecondary = Color.White,
    secondaryContainer = LightBlue90,
    onSecondaryContainer = LightBlue10,
    tertiary = Cyan40,
    onTertiary = Color.White,
    tertiaryContainer = Cyan90,
    onTertiaryContainer = Cyan10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Color(0xFFF8F9FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4CF)
)

private val WeatherDarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = LightBlue80,
    onSecondary = LightBlue20,
    secondaryContainer = LightBlue30,
    onSecondaryContainer = LightBlue90,
    tertiary = Cyan80,
    onTertiary = Cyan20,
    tertiaryContainer = Cyan30,
    onTertiaryContainer = Cyan90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4CF),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454E)
)

val Shapes = Shapes()

@Composable
fun MIPTWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WeatherDarkColorScheme
        else -> WeatherLightColorScheme
    }

    val weatherColors = remember(darkTheme, colorScheme.primary) {
        val baseHot = Color(0xFFE53935)
        val baseWarm = Color(0xFFFB8C00)
        val baseMild = Color(0xFF43A047)
        val baseCool = Color(0xFF1E88E5)
        val baseCold = Color(0xFF3949AB)
        val baseFreezing = Color(0xFF8E24AA)
        val baseRainy = Color(0xFF039BE5)
        val baseSnowy = Color(0xFFB3E5FC)
        val baseStormy = Color(0xFF546E7A)
        val baseFoggy = Color(0xFF9E9E9E)
        val harmonizationFactor = if (darkTheme) 0.15f else 0.1f
        WeatherColors(
            hot = baseHot.harmonizeWith(colorScheme.primary, harmonizationFactor),
            warm = baseWarm.harmonizeWith(colorScheme.primary, harmonizationFactor),
            mild = baseMild.harmonizeWith(colorScheme.primary, harmonizationFactor),
            cool = baseCool.harmonizeWith(colorScheme.primary, harmonizationFactor),
            cold = baseCold.harmonizeWith(colorScheme.primary, harmonizationFactor),
            freezing = baseFreezing.harmonizeWith(colorScheme.primary, harmonizationFactor),
            rainy = baseRainy.harmonizeWith(colorScheme.primary, harmonizationFactor),
            snowy = baseSnowy.harmonizeWith(colorScheme.primary, if (darkTheme) 0.3f else 0.2f),
            stormy = baseStormy.harmonizeWith(colorScheme.primary, harmonizationFactor),
            foggy = baseFoggy.harmonizeWith(colorScheme.primary, harmonizationFactor)
        )
    }

    val spacing = remember { Spacing() }
    val elevations = remember { Elevations() }
    val tempThresholds = remember { TemperatureThresholds() }

    CompositionLocalProvider(
        LocalWeatherColors provides weatherColors,
        LocalSpacing provides spacing,
        LocalElevations provides elevations,
        LocalTemperatureThresholds provides tempThresholds
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

object WeatherTheme {
    val weatherColors: WeatherColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWeatherColors.current

    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val elevations: Elevations
        @Composable
        @ReadOnlyComposable
        get() = LocalElevations.current

    val tempThresholds: TemperatureThresholds
        @Composable
        @ReadOnlyComposable
        get() = LocalTemperatureThresholds.current
}

private fun Color.harmonizeWith(primary: Color, fraction: Float): Color {
    val overlayColor = this.copy(alpha = 1f)
    val backgroundColor = primary.copy(alpha = fraction)
    return overlayColor.compositeOver(backgroundColor)
}