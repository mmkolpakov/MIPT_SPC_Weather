package ru.hse.miem.miptweather.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.hse.miem.miptweather.R
import ru.hse.miem.miptweather.presentation.theme.MIPTWeatherTheme
import ru.hse.miem.miptweather.presentation.theme.WeatherTheme

private object ShimmerDefaults {
    const val ANIMATION_DURATION_MS = 1300
    const val BASE_ALPHA = 0.7f
    const val SHIMMER_ALPHA = 0.3f
}

@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    durationMillis: Int = ShimmerDefaults.ANIMATION_DURATION_MS
): Brush {
    val density = LocalDensity.current
    val targetValuePx = remember(density) {
        with(density) { 1000.dp.toPx() }
    }

    return if (showShimmer) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValuePx,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslation"
        )

        val colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ShimmerDefaults.BASE_ALPHA),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ShimmerDefaults.SHIMMER_ALPHA),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ShimmerDefaults.BASE_ALPHA),
        )

        Brush.linearGradient(
            colors = colors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation, y = translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ShimmerDefaults.BASE_ALPHA),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ShimmerDefaults.BASE_ALPHA)
            ),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun SkeletonPlaceholder(
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .background(shimmerBrush())
    )
}

@Composable
fun WeatherCardSkeleton(modifier: Modifier = Modifier) {
    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium)
    ) {
        Column(modifier = Modifier.padding(spacing.default)) {
            SkeletonPlaceholder(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(0.7f)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(spacing.default))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonPlaceholder(
                    modifier = Modifier
                        .height(32.dp)
                        .width(100.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(spacing.default))
                Column {
                    SkeletonPlaceholder(
                        modifier = Modifier
                            .height(16.dp)
                            .width(120.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                    SkeletonPlaceholder(
                        modifier = Modifier
                            .height(16.dp)
                            .width(80.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                SkeletonPlaceholder(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(spacing.default))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SkeletonPlaceholder(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(spacing.xsmall))
                        SkeletonPlaceholder(
                            modifier = Modifier
                                .height(12.dp)
                                .width(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartSkeleton(modifier: Modifier = Modifier) {
    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium)
    ) {
        Column(modifier = Modifier.padding(spacing.default)) {
            SkeletonPlaceholder(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(0.4f)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(spacing.default))
            SkeletonPlaceholder(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun HourlyForecastSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    val spacing = WeatherTheme.spacing
    val elevations = WeatherTheme.elevations
    val fontScale = LocalDensity.current.fontScale

    val itemWidth = remember(fontScale) {
        (48 * fontScale).coerceAtLeast(48f).dp
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevations.medium)
    ) {
        Column(modifier = Modifier.padding(spacing.default)) {
            SkeletonPlaceholder(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(spacing.default))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.default)
            ) {
                repeat(itemCount) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(itemWidth)
                    ) {
                        SkeletonPlaceholder(
                            modifier = Modifier
                                .height(16.dp)
                                .width(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        SkeletonPlaceholder(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        SkeletonPlaceholder(
                            modifier = Modifier
                                .height(16.dp)
                                .width(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherScreenSkeleton(modifier: Modifier = Modifier) {
    val spacing = WeatherTheme.spacing
    val cdLoading = stringResource(R.string.loading)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.default)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = cdLoading
            },
        verticalArrangement = Arrangement.spacedBy(spacing.default)
    ) {
        WeatherCardSkeleton()
        HourlyForecastSkeleton()
        ChartSkeleton()
    }
}

@Composable
fun LinearProgressIndicatorWithLabel(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val progressHeight = 4.dp
    val labelHeight = 16.dp
    val totalHeight = if (label != null) progressHeight + labelHeight + 4.dp else progressHeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(progressHeight),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (label != null) {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(labelHeight)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Skeleton Light")
@Composable
private fun SkeletonPreviewLight() {
    MIPTWeatherTheme(darkTheme = false) {
        SkeletonPreviewContent()
    }
}

@Preview(showBackground = true, name = "Skeleton Dark")
@Composable
private fun SkeletonPreviewDark() {
    MIPTWeatherTheme(darkTheme = true) {
        SkeletonPreviewContent()
    }
}

@Composable
private fun SkeletonPreviewContent() {
    val spacing = WeatherTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.default),
        verticalArrangement = Arrangement.spacedBy(spacing.default)
    ) {
        WeatherCardSkeleton()
        HourlyForecastSkeleton()
        ChartSkeleton()
    }
}

@Preview(showBackground = true, name = "Progress Indicator")
@Composable
private fun ProgressIndicatorPreview() {
    MIPTWeatherTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LinearProgressIndicatorWithLabel(progress = 0.35f, label = "Loading: 35%")
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicatorWithLabel(progress = 0.75f, label = "Loading: 75%")
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicatorWithLabel(progress = 0.95f)
        }
    }
}