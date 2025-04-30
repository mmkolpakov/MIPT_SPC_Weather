package ru.hse.miem.miptweather.domain.usecase

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.domain.repository.LocationRepository
import ru.hse.miem.miptweather.domain.util.Result
import javax.inject.Inject

class GetCurrentLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    @ApplicationContext private val context: Context
) {
    private companion object { const val TAG = "GetCurrentLocation" }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    operator fun invoke(): Flow<Result<Location>> = callbackFlow {
        send(Result.Loading)

        if (!hasLocationPermission()) {
            send(Result.Error(SecurityException("Location permission not granted.")))
            close()
            return@callbackFlow
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        try {
            Log.d(TAG, "Requesting current location...")
            val locationResult = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).await()

            if (locationResult != null) {
                Log.d(TAG, "Location received: ${locationResult.latitude}, ${locationResult.longitude}")
                val domainLocation = Location(
                    latitude = locationResult.latitude,
                    longitude = locationResult.longitude
                )
                val savedLocation = locationRepository.findSavedLocationByCoords(domainLocation.latitude, domainLocation.longitude)
                send(Result.Success(savedLocation ?: domainLocation))
            } else {
                Log.w(TAG, "FusedLocationProviderClient returned null location.")
                send(Result.Error(Exception("Could not get current location.")))
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission error during request", e)
            send(Result.Error(e))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current location", e)
            send(Result.Error(e))
        } finally {
            close()
        }

        awaitClose {
            Log.d(TAG, "Flow cancelled, cancelling location request.")
            cancellationTokenSource.cancel()
        }
    }
}