package ru.hse.miem.miptweather.data.api

open class ApiException(
    message: String,
    val code: Int? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class NoConnectivityException(
    message: String = "No internet connection available",
    cause: Throwable? = null
) : ApiException(message, cause = cause)

class TimeoutException(
    message: String = "Request timed out",
    cause: Throwable? = null
) : ApiException(message, cause = cause)

class InvalidApiKeyException(
    message: String = "Invalid or missing API key",
    cause: Throwable? = null
) : ApiException(message, code = 401, cause = cause)

class RateLimitExceededException(
    message: String = "API rate limit exceeded",
    cause: Throwable? = null
) : ApiException(message, code = 429, cause = cause)

class BadRequestException(
    message: String = "Bad request parameters",
    cause: Throwable? = null
) : ApiException(message, code = 400, cause = cause)

class ServerErrorException(
    message: String = "API server error",
    code: Int? = 500,
    cause: Throwable? = null
) : ApiException(message, code, cause)