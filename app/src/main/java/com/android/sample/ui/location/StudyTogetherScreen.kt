package com.android.sample.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.theme.BreakYellow
import com.android.sample.ui.theme.IdleBlue
import com.android.sample.ui.theme.IndicatorRed
import com.android.sample.ui.theme.StudyGreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import okhttp3.OkHttpClient

// Parts of this code were written with ChatGPT assistance

// Test tags
private const val TAG_FAB_ADD = "fab_add_friend"
private const val TAG_FIELD_UID = "field_friend_uid"
private const val TAG_BTN_FRIENDS = "btn_friends"
private const val TAG_MAP_STUB = "map_stub"
private const val ON_CAMPUS = "on_campus_indicator"

// Map and camera constants
private const val DEFAULT_MAP_ZOOM = 16f
private const val CAMERA_ANIMATION_DURATION_MS = 600
private const val MARKER_ANCHOR_CENTER = 0.5f

// Marker sizes (in dp)
private const val USER_MARKER_SIZE_DP = 44f
private const val FRIEND_MARKER_SIZE_DP = 40f
private const val TODO_MARKER_SIZE_DP = 52f

// Icon sizes (in dp)
private const val ICON_SIZE_SMALL_DP = 6
private const val ICON_SIZE_MEDIUM_DP = 10
private const val ICON_SIZE_REGULAR_DP = 18

// Corner radius (in dp)
private const val CORNER_RADIUS_CARD_DP = 24
private const val CORNER_RADIUS_PILL_DP = 50
private const val CORNER_RADIUS_ROW_DP = 18

// Padding values (in dp)
private const val PADDING_SMALL_DP = 4
private const val PADDING_MEDIUM_DP = 8
private const val PADDING_STANDARD_DP = 12
private const val PADDING_LARGE_DP = 16
private const val PADDING_TOP_INDICATOR_DP = 12
private const val PADDING_TOP_FRIENDS_BUTTON_DP = 72
private const val PADDING_TOP_DROPDOWN_DP = 44

// Spacing values (in dp)
private const val SPACING_TINY_DP = 6
private const val SPACING_SMALL_DP = 8
private const val SPACING_MEDIUM_DP = 9

// Elevation (in dp)
private const val ELEVATION_CARD_DP = 6
private const val ELEVATION_INFO_CARD_DP = 8

// Size constraints (in dp)
private const val MIN_BUTTON_HEIGHT_DP = 36
private const val MIN_DROPDOWN_WIDTH_DP = 220
private const val MAX_DROPDOWN_WIDTH_DP = 320

// Alpha (transparency) ratios
private const val ALPHA_SURFACE_HIGH = 0.95f
private const val ALPHA_SURFACE_VERY_HIGH = 0.98f
private const val ALPHA_SURFACE_MEDIUM = 0.6f
private const val ALPHA_STATUS_CHIP_BG = 0.18f

// Z-index for map markers
private const val USER_MARKER_Z_INDEX = 1f

@Stable
private data class AddFriendUiState(
    val showDialog: Boolean,
    val friendUidInput: String,
)

@Stable
private data class StudyTogetherActions(
    val onFriendUidChange: (String) -> Unit,
    val onUserSelected: () -> Unit,
    val onFriendSelected: (FriendStatus) -> Unit,
    val onAddFriendFabClick: () -> Unit,
    val onDismissAddFriendDialog: () -> Unit,
    val onConfirmAddFriend: (String) -> Unit,
)

private data class TodoMarker(
    val id: String,
    val title: String,
    val locationName: String,
    val position: LatLng,
    val deadlineText: String,
)

/* ---------- Main Screen Entry Point ---------- */

@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class)
@Composable
fun StudyTogetherScreen(
    viewModel: StudyTogetherViewModel = viewModel(),
    /** Set false in tests to avoid GoogleMap swallowing injected touches. */
    showMap: Boolean = true,
    chooseLocation: Boolean = false,
    chosenLocation: LatLng = DEFAULT_LOCATION,
) {
  val permissions =
      rememberMultiplePermissionsState(
          listOf(
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
  val uiState by viewModel.uiState.collectAsState()

  var showAddDialog by remember { mutableStateOf(false) }
  var friendUidInput by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }

  val permissionsAlreadyGranted = remember { permissions.allPermissionsGranted }

  // Request permissions if not already granted
  RequestLocationPermissions(
      permissionsAlreadyGranted = permissionsAlreadyGranted,
      requestPermissions = { permissions.launchMultiplePermissionRequest() })

  // Track user location with continuous updates
  TrackUserLocation(
      permissionsGranted = permissions.allPermissionsGranted,
      chooseLocation = chooseLocation,
      chosenLocation = chosenLocation,
      onLocationUpdate = viewModel::consumeLocation)

  // Handle and display error messages
  HandleErrorMessages(
      errorMessage = uiState.errorMessage,
      snackbarHostState = snackbarHostState,
      onErrorConsumed = viewModel::consumeError)

  // Animate camera to follow user location
  val cameraPosition = rememberCameraPositionState()
  AnimateCameraToUser(
      userLatLng = uiState.effectiveUserLatLng, cameraPositionState = cameraPosition)

  val addFriendUiState =
      AddFriendUiState(
          showDialog = showAddDialog,
          friendUidInput = friendUidInput,
      )

  val actions =
      StudyTogetherActions(
          onFriendUidChange = { friendUidInput = it },
          onUserSelected = { viewModel.selectUser() },
          onFriendSelected = { viewModel.selectFriend(it) },
          onAddFriendFabClick = { showAddDialog = true },
          onDismissAddFriendDialog = { showAddDialog = false },
          onConfirmAddFriend = { uid ->
            viewModel.addFriendByUid(uid)
            friendUidInput = ""
            showAddDialog = false
          },
      )

  StudyTogetherContent(
      uiState = uiState,
      showMap = showMap,
      permissionsGranted = permissions.allPermissionsGranted,
      cameraPositionState = cameraPosition,
      snackbarHostState = snackbarHostState,
      addFriendUiState = addFriendUiState,
      actions = actions,
  )
}

/* ---------- Permission & Location Side Effects ---------- */

/** Request location permissions if not already granted */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestLocationPermissions(
    permissionsAlreadyGranted: Boolean,
    requestPermissions: () -> Unit
) {
  LaunchedEffect(Unit) {
    if (!permissionsAlreadyGranted) {
      requestPermissions()
    }
  }
}

/** Set up continuous location tracking with real-time updates */
@SuppressLint("MissingPermission")
@Composable
private fun TrackUserLocation(
    permissionsGranted: Boolean,
    chooseLocation: Boolean,
    chosenLocation: LatLng,
    onLocationUpdate: (Double, Double) -> Unit
) {
  val context = LocalContext.current

  LaunchedEffect(permissionsGranted) {
    if (permissionsGranted) {
      val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

      // First, get last known location for immediate display
      fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        val actualLoc = loc?.let { it.latitude to it.longitude }
        resolveLocationCoordinates(chooseLocation, chosenLocation, actualLoc)?.let { (lat, lng) ->
          onLocationUpdate(lat, lng)
        }
      }

      // Then set up continuous location updates
      val locationRequest =
          LocationRequest.Builder(
                  Priority.PRIORITY_HIGH_ACCURACY, 10000L // Update every 10 seconds
                  )
              .apply {
                setMinUpdateIntervalMillis(5000L) // But no more than every 5 seconds
                setMaxUpdateDelayMillis(15000L)
              }
              .build()

      val locationCallback =
          object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
              val actualLoc = locationResult.lastLocation?.let { it.latitude to it.longitude }
              resolveLocationCoordinates(chooseLocation, chosenLocation, actualLoc)?.let {
                  (lat, lng) ->
                onLocationUpdate(lat, lng)
              }
            }
          }

      // Start receiving location updates
      fusedLocationClient.requestLocationUpdates(
          locationRequest, locationCallback, android.os.Looper.getMainLooper())

      // Clean up when effect leaves composition
      try {
        kotlinx.coroutines.awaitCancellation()
      } finally {
        fusedLocationClient.removeLocationUpdates(locationCallback)
      }
    }
  }
}

/** Handle error messages from ViewModel, converting resource IDs to strings */
@Composable
private fun HandleErrorMessages(
    errorMessage: String?,
    snackbarHostState: SnackbarHostState,
    onErrorConsumed: () -> Unit
) {
  val context = LocalContext.current

  LaunchedEffect(errorMessage) {
    errorMessage?.let { raw ->
      // If the message looks like an Int (e.g. "2131755344"),
      // treat it as a string resource ID coming from `require { R.string.… }`.
      val msg =
          raw.toIntOrNull()?.let { resId ->
            try {
              context.getString(resId)
            } catch (_: Throwable) {
              raw // fallback if it's not a valid string resource
            }
          } ?: raw

      snackbarHostState.showSnackbar(msg)
      onErrorConsumed()
    }
  }
}

/** Animate camera to follow user location changes */
@Composable
private fun AnimateCameraToUser(userLatLng: LatLng, cameraPositionState: CameraPositionState) {
  LaunchedEffect(userLatLng) {
    cameraPositionState.safeAnimateTo(userLatLng, zoom = 16f, durationMs = 600)
  }
}

/* ---------- Main screen layout (reduced complexity) ---------- */

@Composable
private fun StudyTogetherContent(
    uiState: StudyTogetherUiState,
    showMap: Boolean,
    permissionsGranted: Boolean,
    cameraPositionState: CameraPositionState,
    snackbarHostState: SnackbarHostState,
    addFriendUiState: AddFriendUiState,
    actions: StudyTogetherActions,
) {
  val showAddDialog = addFriendUiState.showDialog
  val friendUidInput = addFriendUiState.friendUidInput

  Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (showMap) {
            StudyMap(
                cameraPositionState = cameraPositionState,
                permissionsGranted = permissionsGranted,
                uiState = uiState,
                onUserSelected = actions.onUserSelected,
                onFriendSelected = actions.onFriendSelected,
                modifier = Modifier.matchParentSize())
          } else {
            // Simple stub so layout stays similar in tests
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag(TAG_MAP_STUB))
          }

          // On-campus indicator (only show after location is initialized)
          if (uiState.isLocationInitialized) {
            OnCampusIndicator(
                modifier =
                    Modifier.align(Alignment.TopCenter)
                        .padding(top = PADDING_TOP_INDICATOR_DP.dp)
                        .testTag(ON_CAMPUS),
                uiState.isOnCampus)
          }

          // Compact friends dropdown
          EdumonFriendsDropdown(
              friends = uiState.friends,
              onPick = actions.onFriendSelected,
              modifier =
                  Modifier.align(Alignment.TopStart)
                      .padding(PADDING_STANDARD_DP.dp, top = PADDING_TOP_FRIENDS_BUTTON_DP.dp)
                      .testTag(TAG_BTN_FRIENDS))

          // Bottom info cards (user / friend status)
          BottomSelectionPanel(
              isUserSelected = uiState.isUserSelected,
              selectedFriend = uiState.selectedFriend,
          )

          // Add friend FAB
          AddFriendFab(
              onClick = actions.onAddFriendFabClick,
          )

          // Dialog to add friend by UID
          if (showAddDialog) {
            AddFriendDialog(
                friendUid = friendUidInput,
                onFriendUidChange = actions.onFriendUidChange,
                onConfirm = { actions.onConfirmAddFriend(it) },
                onDismiss = actions.onDismissAddFriendDialog)
          }
        }
      }
}

/* ---------- Map (user + friends) ---------- */

@Composable
private fun StudyMap(
    cameraPositionState: CameraPositionState,
    permissionsGranted: Boolean,
    uiState: StudyTogetherUiState,
    onUserSelected: () -> Unit,
    onFriendSelected: (FriendStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
  val userLatLng = uiState.effectiveUserLatLng
  val friends = uiState.friends
  val context = LocalContext.current

  // --- ToDo markers state + loading from repository ---
  val todoRepo = remember { AppRepositories.toDoRepository }
  val okHttpClient = remember { OkHttpClient() }
  val locationRepo = remember { NominatimLocationRepository(okHttpClient) }

  var todoMarkers by remember { mutableStateOf<List<TodoMarker>>(emptyList()) }

  // Collect todos and geocode their locations on a background dispatcher (inside repo)
  LaunchedEffect(todoRepo) {
    todoRepo.todos.collectLatest { todos ->
      val markers = mutableListOf<TodoMarker>()

      for (todo in todos) {
        val locName = todo.location ?: continue
        if (locName.isBlank()) continue

        val best =
            try {
              locationRepo.search(locName).firstOrNull()
            } catch (e: Exception) {
              Log.w(TAG, "Failed to geocode todo location: $locName", e)
              null
            }

        if (best != null) {
          markers +=
              TodoMarker(
                  id = todo.id,
                  title = todo.title,
                  locationName = best.name,
                  deadlineText = "Due: ${todo.dueDate}",
                  position = LatLng(best.latitude, best.longitude),
              )
        }
      }

      todoMarkers = markers
    }
  }

  // One MarkerState per friend id (prevents association crash)
  val markerStates = remember { mutableStateMapOf<String, MarkerState>() }
  LaunchedEffect(friends) {
    val ids = friends.map { it.id }.toSet()
    (markerStates.keys - ids).forEach { markerStates.remove(it) }
  }

  GoogleMap(
      modifier = modifier,
      cameraPositionState = cameraPositionState,
      properties =
          MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = permissionsGranted)) {
        // --- User marker (BitmapDescriptorFactory only used *inside* GoogleMap) ---
        val userIcon = remember {
          BitmapDescriptorFactory.fromBitmap(
              loadDrawableAsBitmap(context, R.drawable.edumon, sizeDp = USER_MARKER_SIZE_DP))
        }
        val userMarkerState = remember { MarkerState(position = userLatLng) }
        LaunchedEffect(userLatLng) { userMarkerState.position = userLatLng }

        Marker(
            state = userMarkerState,
            title = "You",
            icon = userIcon,
            anchor = Offset(MARKER_ANCHOR_CENTER, MARKER_ANCHOR_CENTER),
            zIndex = USER_MARKER_Z_INDEX,
            onClick = {
              onUserSelected()
              true
            })

        // --- To-Do markers ---
        val todoIcon = remember {
          BitmapDescriptorFactory.fromBitmap(
              loadDrawableAsBitmap(context, R.drawable.marker, sizeDp = TODO_MARKER_SIZE_DP))
        }
        todoMarkers.forEach { marker ->
          Marker(
              state = MarkerState(position = marker.position),
              title = marker.title,
              snippet = marker.deadlineText,
              icon = todoIcon,
              anchor = Offset(MARKER_ANCHOR_CENTER, MARKER_ANCHOR_CENTER),
          )
        }

        // --- Friend markers ---
        val friendsDistinct = remember(friends) { friends.distinctBy { it.id } }

        friendsDistinct.forEach { friend ->
          key(friend.id) {
            val target = LatLng(friend.latitude, friend.longitude)
            val state = markerStates.getOrPut(friend.id) { MarkerState(position = target) }
            LaunchedEffect(friend.id, friend.latitude, friend.longitude) { state.position = target }
            val iconRes = edumonFor(friend.id)
            val friendIcon =
                remember(friend.id) {
                  val bmp = loadDrawableAsBitmap(context, iconRes, sizeDp = FRIEND_MARKER_SIZE_DP)
                  BitmapDescriptorFactory.fromBitmap(bmp)
                }
            Marker(
                state = state,
                title = friend.name,
                icon = friendIcon,
                anchor = Offset(MARKER_ANCHOR_CENTER, MARKER_ANCHOR_CENTER),
                onClick = {
                  onFriendSelected(friend)
                  true
                })
          }
        }
      }
}

/* ---------- Bottom selection panel (cards) ---------- */

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BoxScope.BottomSelectionPanel(
    isUserSelected: Boolean,
    selectedFriend: FriendStatus?,
) {
  AnimatedVisibility(
      visible = selectedFriend != null || isUserSelected,
      enter = slideInVertically { it } + fadeIn(),
      exit = slideOutVertically { it } + fadeOut(),
      modifier = Modifier.align(Alignment.BottomCenter).padding(PADDING_LARGE_DP.dp)) {
        when {
          isUserSelected -> UserStatusCard(isStudyMode = true, modifier = Modifier.fillMaxWidth())
          selectedFriend != null ->
              FriendInfoCard(
                  name = selectedFriend.name,
                  mode = selectedFriend.mode,
                  modifier = Modifier.fillMaxWidth())
        }
      }
}

/* ---------- Add friend FAB + dialog ---------- */

@Composable
private fun BoxScope.AddFriendFab(onClick: () -> Unit) {
  ExtendedFloatingActionButton(
      onClick = onClick,
      icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
      text = { Text("Add friend") },
      modifier =
          Modifier.align(Alignment.BottomEnd).padding(PADDING_LARGE_DP.dp).testTag(TAG_FAB_ADD),
      shape = RoundedCornerShape(CORNER_RADIUS_CARD_DP.dp),
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary)
}

@Composable
private fun AddFriendDialog(
    friendUid: String,
    onFriendUidChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      shape = RoundedCornerShape(CORNER_RADIUS_CARD_DP.dp),
      title = { Text(text = "Add friend by UID", style = MaterialTheme.typography.titleMedium) },
      text = {
        OutlinedTextField(
            value = friendUid,
            onValueChange = onFriendUidChange,
            label = { Text("Friend UID") },
            singleLine = true,
            modifier = Modifier.testTag(TAG_FIELD_UID))
      },
      confirmButton = {
        TextButton(enabled = friendUid.isNotBlank(), onClick = { onConfirm(friendUid.trim()) }) {
          Text("Add")
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/* ---------- Friends dropdown (compact “edumon” row) ---------- */

@Composable
private fun EdumonFriendsDropdown(
    friends: List<FriendStatus>,
    onPick: (FriendStatus) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Friends"
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    // Top pill (same style as campus indicator card)
    FilledTonalButton(
        onClick = { expanded = !expanded },
        contentPadding =
            PaddingValues(horizontal = PADDING_STANDARD_DP.dp, vertical = PADDING_MEDIUM_DP.dp),
        modifier = Modifier.defaultMinSize(minHeight = MIN_BUTTON_HEIGHT_DP.dp),
        shape = RoundedCornerShape(CORNER_RADIUS_PILL_DP), // full pill
        colors =
            ButtonDefaults.filledTonalButtonColors(
                // Use the same color as your campus indicator card
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = ALPHA_SURFACE_HIGH),
                contentColor = MaterialTheme.colorScheme.onSurface,
            )) {
          Icon(
              painter = painterResource(id = R.drawable.edumon1),
              contentDescription = null,
              modifier = Modifier.size(ICON_SIZE_REGULAR_DP.dp))
          Spacer(Modifier.width(SPACING_TINY_DP.dp))
          Text("$label (${friends.size})")
        }

    // Our own "dropdown", drawn as a Card → only one background, fully rounded
    if (expanded) {
      Card(
          modifier =
              Modifier.align(Alignment.TopStart)
                  .padding(top = PADDING_TOP_DROPDOWN_DP.dp) // show under the pill
                  .widthIn(min = MIN_DROPDOWN_WIDTH_DP.dp, max = MAX_DROPDOWN_WIDTH_DP.dp),
          shape = RoundedCornerShape(CORNER_RADIUS_CARD_DP.dp),
          colors =
              CardDefaults.cardColors(
                  containerColor =
                      MaterialTheme.colorScheme.surface.copy(alpha = ALPHA_SURFACE_VERY_HIGH)),
          elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_CARD_DP.dp)) {
            if (friends.isEmpty()) {
              Text(
                  text = "No friends yet",
                  modifier =
                      Modifier.padding(
                          horizontal = PADDING_LARGE_DP.dp, vertical = PADDING_STANDARD_DP.dp),
                  style = MaterialTheme.typography.bodyMedium)
            } else {
              Column(
                  modifier =
                      Modifier.padding(
                          vertical = PADDING_MEDIUM_DP.dp, horizontal = PADDING_MEDIUM_DP.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier.padding(
                                start = PADDING_SMALL_DP.dp, bottom = PADDING_SMALL_DP.dp))

                    friends.forEach { friend ->
                      FriendDropdownRow(
                          friend = friend,
                          onClick = {
                            expanded = false
                            onPick(friend)
                          },
                          modifier = Modifier.fillMaxWidth())
                      Spacer(Modifier.height(SPACING_MEDIUM_DP.dp))
                    }
                  }
            }
          }
    }
  }
}

@Composable
private fun FriendDropdownRow(
    friend: FriendStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier =
          modifier
              .clip(RoundedCornerShape(CORNER_RADIUS_ROW_DP.dp))
              .clickable(onClick = onClick)
              .background(
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ALPHA_SURFACE_MEDIUM))
              .padding(horizontal = PADDING_STANDARD_DP.dp, vertical = PADDING_MEDIUM_DP.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = edumonFor(friend.id)),
            contentDescription = null,
            modifier = Modifier.size(ICON_SIZE_REGULAR_DP.dp))
        Spacer(Modifier.width(SPACING_SMALL_DP.dp))
        Text(
            friend.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(SPACING_SMALL_DP.dp))
        StatusChip(mode = friend.mode)
      }
}

@Composable
private fun OnCampusIndicator(modifier: Modifier = Modifier, onCampus: Boolean) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(CORNER_RADIUS_PILL_DP),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = ALPHA_SURFACE_HIGH)),
      elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_CARD_DP.dp)) {
        if (onCampus) {
          Row(
              modifier =
                  Modifier.padding(
                      horizontal = PADDING_STANDARD_DP.dp, vertical = PADDING_MEDIUM_DP.dp),
              verticalAlignment = Alignment.CenterVertically) {
                // Green dot
                Box(
                    modifier =
                        Modifier.size(ICON_SIZE_MEDIUM_DP.dp)
                            .clip(CircleShape)
                            .background(StudyGreen))
                Spacer(Modifier.width(SPACING_SMALL_DP.dp))
                Text(text = "On EPFL campus", style = MaterialTheme.typography.labelLarge)
              }
        } else {
          Row(
              modifier =
                  Modifier.padding(
                      horizontal = PADDING_STANDARD_DP.dp, vertical = PADDING_MEDIUM_DP.dp),
              verticalAlignment = Alignment.CenterVertically) {
                // Green dot
                Box(
                    modifier =
                        Modifier.size(ICON_SIZE_MEDIUM_DP.dp)
                            .clip(CircleShape)
                            .background(IndicatorRed))
                Spacer(Modifier.width(SPACING_SMALL_DP.dp))
                Text(text = "Outside of EPFL campus", style = MaterialTheme.typography.labelLarge)
              }
        }
      }
}

@Composable
private fun StatusChip(mode: FriendMode) {
  val (label, bg) =
      when (mode) {
        FriendMode.STUDY -> "Studying" to StudyGreen
        FriendMode.BREAK -> "Break" to BreakYellow
        FriendMode.IDLE -> "Idle" to IdleBlue
      }

  Box(
      modifier =
          Modifier.clip(RoundedCornerShape(CORNER_RADIUS_PILL_DP))
              .background(bg.copy(alpha = ALPHA_STATUS_CHIP_BG))
              .padding(horizontal = ICON_SIZE_MEDIUM_DP.dp, vertical = PADDING_SMALL_DP.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(ICON_SIZE_SMALL_DP.dp).clip(CircleShape).background(bg))
          Spacer(Modifier.width(SPACING_TINY_DP.dp))
          Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
      }
}

/* ---------- Info cards ---------- */

@Composable
fun UserStatusCard(isStudyMode: Boolean, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(CORNER_RADIUS_CARD_DP.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_INFO_CARD_DP.dp)) {
        Text(
            text = if (isStudyMode) "You're studying" else "You're on a break",
            modifier = Modifier.padding(PADDING_LARGE_DP.dp),
            style = MaterialTheme.typography.bodyMedium)
      }
}

@Composable
fun FriendInfoCard(name: String, mode: FriendMode, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(CORNER_RADIUS_CARD_DP.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_INFO_CARD_DP.dp)) {
        Row(Modifier.padding(PADDING_LARGE_DP.dp)) {
          Text(text = name, style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(SPACING_TINY_DP.dp))
          StatusChip(mode)
        }
      }
}

/* -------------------- helpers -------------------- */

private fun edumonFor(friendId: String): Int {
  val all = intArrayOf(R.drawable.edumon1, R.drawable.edumon2, R.drawable.edumon3)
  val idx = abs(friendId.hashCode()) % all.size
  return all[idx]
}

private fun dp2px(context: Context, dp: Float): Int =
    (dp * context.resources.displayMetrics.density).toInt()

private fun loadDrawableAsBitmap(context: Context, resId: Int, sizeDp: Float): Bitmap {
  val d = AppCompatResources.getDrawable(context, resId) ?: error("Drawable $resId not found")
  val px = dp2px(context, sizeDp)
  return d.toBitmap(width = px, height = px, config = Bitmap.Config.ARGB_8888)
}

private suspend fun CameraPositionState.safeAnimateTo(
    latLng: LatLng,
    zoom: Float,
    durationMs: Int
) {
  try {
    animate(CameraUpdateFactory.newLatLngZoom(latLng, zoom), durationMs)
  } catch (e: Exception) {
    Log.w(TAG, "Camera Problem", e)
  }
}

// Helper to persist last location for background polling worker
internal fun persistLastLocation(ctx: Context, lat: Double, lon: Double) {
  ctx.getSharedPreferences("last_location", Context.MODE_PRIVATE)
      .edit()
      .putFloat("lat", lat.toFloat())
      .putFloat("lon", lon.toFloat())
      .apply()
}
