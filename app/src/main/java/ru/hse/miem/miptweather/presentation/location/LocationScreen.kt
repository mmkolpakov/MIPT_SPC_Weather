package ru.hse.miem.miptweather.presentation.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EditLocation
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import ru.hse.miem.miptweather.R
import ru.hse.miem.miptweather.domain.model.Location
import ru.hse.miem.miptweather.presentation.theme.WeatherTheme
import java.util.Locale

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun LocationScreen(
    viewModel: LocationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onLocationSelected: (Location) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()
    val spacing = WeatherTheme.spacing
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showForm by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val strPermissionDenied = stringResource(R.string.location_permission_denied)
    val strSettingsAction = stringResource(R.string.settings_action)
    val strRetryAction = stringResource(R.string.action_retry)
    val strNavigateBack = stringResource(R.string.cd_navigate_back)
    val strScreenTitle = stringResource(R.string.location_screen_title)
    val strFabAddLabel = stringResource(R.string.add_location_fab)
    val strFabAddCd = stringResource(R.string.cd_add_new_location)
    val strSavingError = uiState.saveErrorResId?.let { stringResource(it) }
    val strGpsError = uiState.locationError

    fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(this)
        }
    }

    fun requestLocationPermissions() {
        when {
            locationPermissionsState.allPermissionsGranted -> viewModel.useCurrentLocation()
            locationPermissionsState.shouldShowRationale -> scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = strPermissionDenied,
                    actionLabel = strRetryAction,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    locationPermissionsState.launchMultiplePermissionRequest()
                }
            }
            else -> scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = strPermissionDenied,
                    actionLabel = strSettingsAction,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    openAppSettings()
                }
            }
        }
    }

    LaunchedEffect(savedLocations.size, locationPermissionsState.allPermissionsGranted) {
        if (savedLocations.isEmpty() && !locationPermissionsState.allPermissionsGranted) {
        }
    }

    LaunchedEffect(strSavingError) {
        strSavingError?.let {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSaveError()
        }
    }

    LaunchedEffect(strGpsError) {
        strGpsError?.let {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearLocationError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is LocationViewModelEvent.LocationSaved) {
                showForm = false
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strScreenTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = strNavigateBack }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showForm) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.resetDraft()
                        showForm = true
                    },
                    icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null) },
                    text = { Text(strFabAddLabel) },
                    modifier = Modifier.semantics { contentDescription = strFabAddCd }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = showForm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AddLocationForm(
                    uiState = uiState,
                    onLatitudeChange = viewModel::updateLatitude,
                    onLongitudeChange = viewModel::updateLongitude,
                    onNameChange = viewModel::updateLocationName,
                    onSave = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.saveLocation()
                    },
                    onCancel = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.resetDraft()
                        showForm = false
                    },
                    onUseCurrentLocation = {
                        if (!locationPermissionsState.allPermissionsGranted) {
                            requestLocationPermissions()
                        } else {
                            viewModel.useCurrentLocation()
                        }
                    },
                    focusManager = focusManager,
                    keyboardController = keyboardController
                )
            }

            AnimatedVisibility(
                visible = !showForm,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (savedLocations.isEmpty()) {
                        EmptyLocationsState(
                            onAddLocationClick = {
                                viewModel.resetDraft()
                                showForm = true
                            },
                            onRequestGpsClick = ::requestLocationPermissions,
                            canRequestGps = !locationPermissionsState.allPermissionsGranted
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = spacing.small)
                        ) {
                            itemsIndexed(
                                items = savedLocations,
                                key = { _, loc -> loc.id }
                            ) { index, location ->
                                LocationItem(
                                    location = location,
                                    onClick = { onLocationSelected(location) },
                                    onEditClick = {
                                        viewModel.startEditing(location)
                                        showForm = true
                                    },
                                    onDeleteClick = { viewModel.deleteLocation(location) }
                                )
                                if (index < savedLocations.lastIndex) {
                                    HorizontalDivider(Modifier.padding(horizontal = spacing.default))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AddLocationForm(
    uiState: LocationUiState,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    keyboardController: SoftwareKeyboardController?
) {
    val spacing = WeatherTheme.spacing
    val strCdForm = stringResource(R.string.cd_add_location_form)
    val strUseCurrentCd = stringResource(R.string.cd_use_current_location_button)
    val strLatitudeCd = stringResource(R.string.cd_latitude_input_field)
    val strLongitudeCd = stringResource(R.string.cd_longitude_input_field)
    val strNameCd = stringResource(R.string.cd_location_name_input_field)
    val strCancelCd = stringResource(R.string.cd_cancel_adding_location)
    val strSaveCd = stringResource(R.string.cd_save_location)
    val strUpdateCd = stringResource(R.string.cd_update_location)
    val strTitle = stringResource(if (uiState.isEditing) R.string.edit_location_title else R.string.add_location_title)
    val strUseCurrent = stringResource(R.string.use_current_location_button)
    val strManualHint = stringResource(R.string.manual_coordinates_hint)
    val strLatLabel = stringResource(R.string.latitude_label)
    val strLonLabel = stringResource(R.string.longitude_label)
    val strLatRangeHint = stringResource(R.string.latitude_range_hint)
    val strLonRangeHint = stringResource(R.string.longitude_range_hint)
    val strLatError = uiState.latitudeErrorResId?.let { stringResource(it) }
    val strLonError = uiState.longitudeErrorResId?.let { stringResource(it) }
    val strNameLabel = stringResource(R.string.location_name_label)
    val strNamePlaceholder = stringResource(R.string.location_name_placeholder)
    val strSave = stringResource(R.string.save_button)
    val strUpdate = stringResource(R.string.update_button)
    val strCancel = stringResource(android.R.string.cancel)
    val isFormValid = uiState.latitude.isNotBlank() && uiState.longitude.isNotBlank() &&
            uiState.latitudeErrorResId == null && uiState.longitudeErrorResId == null
    val isGpsLoading = uiState.isSaving && uiState.locationError == null && uiState.saveErrorResId == null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(spacing.default)
            .semantics { contentDescription = strCdForm }
    ) {
        Text(strTitle, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(spacing.default))
        Button(
            onClick = onUseCurrentLocation,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics { contentDescription = strUseCurrentCd },
            enabled = !uiState.isSaving
        ) {
            if (isGpsLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
                Spacer(Modifier.width(spacing.small))
                Text(stringResource(R.string.loading_location))
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(Modifier.width(spacing.small))
                Text(strUseCurrent)
            }
        }
        Spacer(Modifier.height(spacing.default))
        Text(strManualHint, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(spacing.small))
        OutlinedTextField(
            value = uiState.latitude,
            onValueChange = onLatitudeChange,
            label = { Text(strLatLabel) },
            isError = strLatError != null,
            supportingText = { Text(strLatError ?: strLatRangeHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            enabled = !isGpsLoading,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = strLatitudeCd }
        )
        Spacer(Modifier.height(spacing.small))
        OutlinedTextField(
            value = uiState.longitude,
            onValueChange = onLongitudeChange,
            label = { Text(strLonLabel) },
            isError = strLonError != null,
            supportingText = { Text(strLonError ?: strLonRangeHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            enabled = !isGpsLoading,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = strLongitudeCd }
        )
        Spacer(Modifier.height(spacing.small))
        OutlinedTextField(
            value = uiState.locationName,
            onValueChange = onNameChange,
            label = { Text(strNameLabel) },
            placeholder = { Text(strNamePlaceholder) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
                if (isFormValid) onSave()
            }),
            singleLine = true,
            enabled = !isGpsLoading,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = strNameCd }
        )
        Spacer(Modifier.height(spacing.medium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.isSaving && !isGpsLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(spacing.default))
            }
            TextButton(
                onClick = onCancel,
                enabled = !uiState.isSaving,
                modifier = Modifier.semantics { contentDescription = strCancelCd }
            ) {
                Text(strCancel)
            }
            Spacer(Modifier.width(spacing.small))
            Button(
                onClick = onSave,
                enabled = isFormValid && !uiState.isSaving,
                modifier = Modifier.semantics {
                    contentDescription = if (uiState.isEditing) strUpdateCd else strSaveCd
                }
            ) {
                Text(if (uiState.isEditing) strUpdate else strSave)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationItem(
    location: Location,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val spacing = WeatherTheme.spacing
    val name = location.getDisplayName()
    val coords = String.format(
        Locale.US,
        stringResource(R.string.coordinates_format),
        location.latitude,
        location.longitude
    )
    val strCdItem = stringResource(R.string.cd_location_item, name, coords)
    val strCdEdit = stringResource(R.string.cd_edit_location, name)
    val strCdDelete = stringResource(R.string.cd_delete_location, name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onEditClick
            )
            .padding(horizontal = spacing.default, vertical = spacing.small)
            .semantics { contentDescription = strCdItem },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.LocationCity,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(40.dp)
                .padding(end = spacing.default),
            contentDescription = null
        )

        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(
                location.countryCode.takeIf(String::isNotBlank),
                location.timezone.takeIf(String::isNotBlank)
            ).joinToString(" / ")
            if (details.isNotEmpty()) {
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(coords, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }

        Row {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.semantics { contentDescription = strCdEdit }
            ) {
                Icon(Icons.Outlined.EditLocation, tint = MaterialTheme.colorScheme.secondary, contentDescription = null)
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.semantics { contentDescription = strCdDelete }
            ) {
                Icon(Icons.Filled.DeleteOutline, tint = MaterialTheme.colorScheme.error, contentDescription = null)
            }
        }
    }
}

@Composable
private fun EmptyLocationsState(
    onAddLocationClick: () -> Unit,
    onRequestGpsClick: () -> Unit,
    canRequestGps: Boolean
) {
    val spacing = WeatherTheme.spacing
    val strTitle = stringResource(R.string.no_saved_locations_title)
    val strMessage = stringResource(R.string.no_saved_locations_message)
    val strButtonAdd = stringResource(R.string.add_first_location_button)
    val strButtonGps = stringResource(R.string.use_current_location_button)
    val strCdAdd = stringResource(R.string.cd_add_first_location)
    val strCdGps = stringResource(R.string.cd_use_current_location_button)
    val strNoLocations = stringResource(R.string.cd_no_saved_locations)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.default)
            .semantics { contentDescription = strNoLocations },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.LocationOff, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(64.dp), contentDescription = null)
        Spacer(Modifier.height(spacing.default))
        Text(strTitle, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.small))
        Text(strMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.medium))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onAddLocationClick,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = strCdAdd }
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                Spacer(Modifier.width(spacing.small))
                Text(strButtonAdd)
            }
            if (canRequestGps) {
//                Spacer(Modifier.height(spacing.small))
//                OutlinedButton(
//                    onClick = onRequestGpsClick,
//                    modifier = Modifier
//                        .heightIn(min = 48.dp)
//                        .semantics { contentDescription = strCdGps }
//                ) {
//                    Icon(Icons.Default.MyLocation, contentDescription = null)
//                    Spacer(Modifier.width(spacing.small))
//                    Text(strButtonGps)
//                }
            }
        }
    }
}