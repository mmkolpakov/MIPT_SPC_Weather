package ru.hse.miem.miptweather.presentation.weather

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.cartesian.layer.*
import com.patrykandpatrick.vico.compose.common.component.*
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.axis.*
import com.patrykandpatrick.vico.core.cartesian.data.*
import com.patrykandpatrick.vico.core.cartesian.layer.*
import com.patrykandpatrick.vico.core.cartesian.marker.*
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.component.LineComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import ru.hse.miem.miptweather.R
import ru.hse.miem.miptweather.domain.model.DailyWeather
import ru.hse.miem.miptweather.presentation.theme.WeatherTheme
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun WeatherChartsScreen(
    daily: List<DailyWeather>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pages: List<@Composable () -> Unit> = listOf(
        { AreaLineChart(daily) },
        { StackedColumnChart(daily) },
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    val scope = rememberCoroutineScope()
    val spacing = WeatherTheme.spacing

    Box(modifier) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            flingBehavior = fling,
            contentPadding = PaddingValues(horizontal = spacing.small),
            pageSpacing = spacing.small,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            pages[page]()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = spacing.medium),
        ) {
            val pageText = stringResource(R.string.chart_page_indicator, pagerState.currentPage + 1, pages.size)
            Text(
                text = pageText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.small)
            )

            PageDots(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                onDotClick = { scope.launch { pagerState.animateScrollToPage(it) } },
            )
        }

        var hasUserSwiped by remember { mutableStateOf(false) }
        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage > 0 && !hasUserSwiped) {
                hasUserSwiped = true
            }
        }

        SwipeHint(
            visible = pagerState.currentPage == 0 && !hasUserSwiped,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = spacing.medium),
        )
    }
}

@Composable
private fun PageDots(
    pageCount: Int,
    currentPage: Int,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .semantics {
                contentDescription = context.getString(R.string.chart_pager_dots_a11y, currentPage + 1, pageCount)
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val size by animateDpAsState(
                if (selected) 12.dp else 10.dp,
                label = "dotSize"
            )
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f),
                label = "dotColor",
            )

            val dotDescription = if (selected) {
                context.getString(R.string.chart_page_dot_selected, index + 1, pageCount)
            } else {
                context.getString(R.string.chart_page_dot, index + 1, pageCount)
            }

            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
                    .pointerInput(Unit) { detectTapGestures { onDotClick(index) } }
                    .semantics {
                        contentDescription = dotDescription
                    },
            )
        }
    }
}

@Composable
private fun SwipeHint(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    var showHint by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            showHint = true
            delay(3000)
            showHint = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "swipeAnimation")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swipeOffset"
    )

    AnimatedVisibility(
        visible = visible && showHint,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 },
        modifier = modifier,
    ) {
        val swipeLeftHint = stringResource(R.string.swipe_left_hint)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
                    shape = RoundedCornerShape(20),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics {
                    contentDescription = swipeLeftHint
                },
        ) {
            Icon(
                imageVector = Icons.Filled.Swipe,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.offset(x = offsetX.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.swipe_left_hint),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun ChartLegend(
    items: List<Pair<Color, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .padding(top = 4.dp, bottom = 8.dp)
            .semantics {
                contentDescription = items.joinToString(", ") { it.second }
            }
    ) {
        items.forEach { (color, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(color, shape = CircleShape)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AreaLineChart(data: List<DailyWeather>) {
    val context = LocalContext.current
    ChartCard(
        title = stringResource(R.string.chart_temperature_variations),
        legendItems = listOf(
            WeatherTheme.weatherColors.hot to stringResource(R.string.chart_legend_max_temp),
            WeatherTheme.weatherColors.cold to stringResource(R.string.chart_legend_min_temp)
        )
    ) {
        val wc = WeatherTheme.weatherColors
        val maxC = wc.hot
        val minC = wc.cold

        val producer = remember { CartesianChartModelProducer() }
        val dates = remember(data) { data.map { it.date } }

        LaunchedEffect(data) {
            val x = data.indices.map(Int::toFloat)
            producer.runTransaction {
                lineSeries {
                    series(x, data.map { it.maxTemperature.toFloat() })
                    series(x, data.map { it.minTemperature.toFloat() })
                }
            }
        }

        val lineMax = rememberBasicLine(
            color = maxC,
            areaFill = LineCartesianLayer.AreaFill.single(fill(maxC.copy(alpha = .25f))),
        )
        val lineMin = rememberBasicLine(minC)

        val chartDescription = context.getString(
            R.string.chart_temperature_a11y,
            buildA11y(data)
        )

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(lineMax, lineMin),
                ),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = rememberDateFormatter(dates),
                ),
                marker = rememberTempMarker(dates),
            ),
            modelProducer = producer,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .semantics { contentDescription = chartDescription }
                .pointerInput(Unit) {
                    detectTapGestures { /* Реакция на тап (опционально) */ }
                },
        )
    }
}

@Composable
private fun StackedColumnChart(data: List<DailyWeather>) {
    val context = LocalContext.current
    ChartCard(
        title = stringResource(R.string.chart_min_max_diff),
        legendItems = listOf(
            WeatherTheme.weatherColors.cold to stringResource(R.string.chart_legend_min_temp),
            WeatherTheme.weatherColors.hot to stringResource(R.string.chart_legend_temp_diff)
        )
    ) {
        val wc = WeatherTheme.weatherColors
        val minC = wc.cold
        val deltaC = wc.hot

        val minValues = data.map { it.minTemperature.toFloat() }
        val deltaValues = data.map { abs(it.maxTemperature - it.minTemperature).toFloat() }
        val dates = remember(data) { data.map { it.date } }

        val producer = remember { CartesianChartModelProducer() }
        LaunchedEffect(data) {
            producer.runTransaction {
                columnSeries {
                    series(minValues)
                    series(deltaValues)
                }
            }
        }

        // Адаптивно вычисляем толщину колонок на основе доступного пространства и количества точек
        val availableWidth = with(LocalDensity.current) { 200.dp.toPx() } // Примерная ширина графика
        val columnWidth = 20.dp.coerceAtMost((availableWidth / (data.size * 1.5f)).dp)

        val columns = ColumnCartesianLayer.ColumnProvider.series(
            rememberColumnComponent(minC, columnWidth),
            rememberColumnComponent(deltaC, columnWidth),
        )

        val chartDescription = context.getString(
            R.string.chart_min_max_diff_a11y,
            buildA11y(data)
        )

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(columnProvider = columns, mergeMode = { ColumnCartesianLayer.MergeMode.Stacked }),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = rememberDateFormatter(dates),
                ),
                marker = rememberStackedColumnMarker(dates),
            ),
            modelProducer = producer,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .semantics { contentDescription = chartDescription }
                .pointerInput(Unit) {
                    detectTapGestures { /* Реакция на тап (опционально) */ }
                },
        )
    }
}

@Composable
private fun ChartCard(
    title: String,
    legendItems: List<Pair<Color, String>> = emptyList(),
    content: @Composable () -> Unit,
) {
    val spacing = WeatherTheme.spacing
    val elev = WeatherTheme.elevations
    Card(
        elevation = CardDefaults.cardElevation(elev.medium)
    ) {
        Column(Modifier.padding(spacing.default)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (legendItems.isNotEmpty()) {
                ChartLegend(
                    items = legendItems,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Spacer(Modifier.height(spacing.small))
            }

            content()
        }
    }
}

@Composable
private fun rememberBasicLine(
    color: Color,
    areaFill: LineCartesianLayer.AreaFill? = null,
): LineCartesianLayer.Line {

    val graph = LineCartesianLayer.rememberLine(
        stroke = LineCartesianLayer.LineStroke.Continuous(thicknessDp = 4f),
        fill = LineCartesianLayer.LineFill.single(fill(color)),
        areaFill = areaFill,
        pointProvider = rememberPointProvider(color),
    )

    return remember(color, areaFill) {
        graph
    }
}

@Composable
private fun rememberPointProvider(color: Color): LineCartesianLayer.PointProvider {
    val component = rememberShapeComponent(fill(color), CorneredShape.Pill)
    return remember(color) {
        LineCartesianLayer.PointProvider.single(
            LineCartesianLayer.Point(component, 8f),
        )
    }
}

@Composable
private fun rememberColumnComponent(color: Color, thickness: Dp): LineComponent =
    rememberLineComponent(
        fill = fill(color),
        thickness = thickness,
        shape = CorneredShape.rounded(allPercent = 25),
    )

@Composable
private fun rememberDateFormatter(dates: List<LocalDate>): CartesianValueFormatter {
    val fmt = remember { DateTimeFormatter.ofPattern("dd MMM") }
    return CartesianValueFormatter { _, v, _ ->
        dates.getOrNull(v.toInt())
            ?.toJavaLocalDate()
            ?.format(fmt)
            ?: ""
    }
}

private fun buildA11y(data: List<DailyWeather>): String = buildString {
    data.forEachIndexed { i, d ->
        append(
            "${d.date.toJavaLocalDate().format(DateTimeFormatter.ofPattern("dd MMM"))}: " +
                    "макс. ${d.maxTemperature.toInt()}°, мин. ${d.minTemperature.toInt()}°",
        )
        if (i < data.lastIndex) append("; ")
    }
}

@Composable
private fun rememberTempMarker(
    dates: List<LocalDate>
): DefaultCartesianMarker {
    val labelComp = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        background = rememberShapeComponent(
            fill(MaterialTheme.colorScheme.surfaceContainerHigh),
            CorneredShape.rounded(allPercent = 30),
        ),
        padding = insets(horizontal = 8.dp, vertical = 4.dp),
    )

    val df = remember { DecimalFormat("#.#°") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM") }
    val guideline = rememberAxisGuidelineComponent()

    return remember(labelComp, df, dateFormatter) {
        DefaultCartesianMarker(
            label = labelComp,
            indicatorSizeDp = 14f,
            guideline = guideline,
            valueFormatter = DefaultCartesianMarker.ValueFormatter { _, targets ->
                val lineTargets = targets.filterIsInstance<LineCartesianLayerMarkerTarget>()
                val index = if (lineTargets.isNotEmpty() && lineTargets.first().points.isNotEmpty()) {
                    lineTargets.first().points.first().entry.x.toInt()
                } else -1

                val date = if (index >= 0 && index < dates.size) {
                    dates[index].toJavaLocalDate().format(dateFormatter)
                } else ""

                buildString {
                    append(date)
                    if (date.isNotEmpty()) append("\n")
                    lineTargets.flatMap { it.points }.forEachIndexed { idx, pt ->
                        append(if (idx == 0) "↑ " else "↓ ")
                        append(df.format(pt.entry.y))
                        if (idx == 0) append("   ")
                    }
                }
            }
        )
    }
}

@Composable
private fun rememberStackedColumnMarker(
    dates: List<LocalDate>
): DefaultCartesianMarker {
    val labelComp = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        background = rememberShapeComponent(
            fill(MaterialTheme.colorScheme.surfaceContainerHigh),
            CorneredShape.rounded(allPercent = 30),
        ),
        padding = insets(horizontal = 8.dp, vertical = 4.dp),
    )

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM") }
    val guideline = rememberAxisGuidelineComponent()
    val df = remember { DecimalFormat("#.#°") }

    return remember(labelComp, dateFormatter, df) {
        DefaultCartesianMarker(
            label = labelComp,
            indicatorSizeDp = 14f,
            guideline = guideline,
            valueFormatter = DefaultCartesianMarker.ValueFormatter { _, targets ->
                val columnTargets = targets.filterIsInstance<ColumnCartesianLayerMarkerTarget>()
                val index = if (columnTargets.isNotEmpty()) {
                    columnTargets.first().x.toInt()
                } else -1

                val date = if (index >= 0 && index < dates.size) {
                    dates[index].toJavaLocalDate().format(dateFormatter)
                } else ""

                buildString {
                    append(date)
                    if (date.isNotEmpty()) append("\n")
                }
            },
        )
    }
}