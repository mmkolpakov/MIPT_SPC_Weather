package ru.hse.miem.miptweather.domain.model

enum class WeatherCapability {
    CURRENT_WEATHER,    // Текущая погода (обычно как часть почасового прогноза)
    HOURLY_FORECAST,    // Почасовой прогноз
    DAILY_FORECAST,     // Дневной прогноз
    HISTORICAL_DATA,    // Исторические данные (по дням/часам)
    AIR_QUALITY,        // Качество воздуха (AQI) - отдельный API
    UV_INDEX,           // УФ-индекс
    FEELS_LIKE,         // Ощущаемая температура
    HUMIDITY,           // Влажность
    PRESSURE,           // Давление
    WIND,               // Скорость и направление ветра
    PRECIPITATION,      // Количество осадков (дождь, снег)
    CLOUDINESS          // Облачность (%)
}