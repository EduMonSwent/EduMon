package com.android.sample.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.repos_providors.AppRepositories
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
import kotlinx.coroutines.launch
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

// Spacing values (in dp)
private const val SPACING_TINY_DP = 6
private const val SPACING_SMALL_DP = 8

// Elevation (in dp)
private const val ELEVATION_CARD_DP = 6
private const val ELEVATION_INFO_CARD_DP = 8

// Size constraints (in dp)
private const val MIN_BUTTON_HEIGHT_DP = 36

// Transparency
private const val ALPHA_MENU_BG = 0.62f // menu background (more transparent)
private const val ALPHA_PILL_BG = 0.48f // top pill (more transparent)
private const val ALPHA_ITEM_BG = 0.96f // list items (opaque)
private const val ALPHA_STATUS_CHIP_BG = 0.18f

// Z-index for map markers
private const val USER_MARKER_Z_INDEX = 1f

private val ON_CAMPUS_GREEN = Color(0xFF2E7D32) // Material Green 800
private val OFF_CAMPUS_RED = Color(0xFFC62828) // Material Red 800

@Stable
private data class AddFriendUiState(
    val showDialog: Boolean,
    val friendUidInput: String,
)

@Stable
private data class StudyTogetherActions(
    val onFriendUidChange: (String) -> Unit,
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
    @DrawableRes userEdumonResId: Int = R.drawable.edumon,
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

  RequestLocationPermissions(
      permissionsAlreadyGranted = permissionsAlreadyGranted,
      requestPermissions = { permissions.launchMultiplePermissionRequest() })

  TrackUserLocation(
      permissionsGranted = permissions.allPermissionsGranted,
      chooseLocation = chooseLocation,
      chosenLocation = chosenLocation,
      onLocationUpdate = viewModel::consumeLocation)

  HandleErrorMessages(
      errorMessage = uiState.errorMessage,
      snackbarHostState = snackbarHostState,
      onErrorConsumed = viewModel::consumeError)

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
      userEdumonResId = userEdumonResId,
  )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestLocationPermissions(
    permissionsAlreadyGranted: Boolean,
    requestPermissions: () -> Unit
) {
  LaunchedEffect(Unit) { if (!permissionsAlreadyGranted) requestPermissions() }
}

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

      fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        val actualLoc = loc?.let { it.latitude to it.longitude }
        resolveLocationCoordinates(chooseLocation, chosenLocation, actualLoc)?.let { (lat, lng) ->
          onLocationUpdate(lat, lng)
        }
      }

      val locationRequest =
          LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
              .apply {
                setMinUpdateIntervalMillis(5000L)
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

      fusedLocationClient.requestLocationUpdates(
          locationRequest, locationCallback, android.os.Looper.getMainLooper())

      try {
        kotlinx.coroutines.awaitCancellation()
      } finally {
        fusedLocationClient.removeLocationUpdates(locationCallback)
      }
    }
  }
}

@Composable
private fun HandleErrorMessages(
    errorMessage: String?,
    snackbarHostState: SnackbarHostState,
    onErrorConsumed: () -> Unit
) {
  val context = LocalContext.current

  LaunchedEffect(errorMessage) {
    errorMessage?.let { raw ->
      val msg =
          raw.toIntOrNull()?.let { resId ->
            try {
              context.getString(resId)
            } catch (_: Throwable) {
              raw
            }
          } ?: raw

      snackbarHostState.showSnackbar(msg)
      onErrorConsumed()
    }
  }
}

@Composable
private fun AnimateCameraToUser(userLatLng: LatLng, cameraPositionState: CameraPositionState) {
  LaunchedEffect(userLatLng) {
    cameraPositionState.safeAnimateTo(
        userLatLng, zoom = DEFAULT_MAP_ZOOM, durationMs = CAMERA_ANIMATION_DURATION_MS)
  }
}

@Composable
private fun StudyTogetherContent(
    uiState: StudyTogetherUiState,
    showMap: Boolean,
    permissionsGranted: Boolean,
    cameraPositionState: CameraPositionState,
    snackbarHostState: SnackbarHostState,
    addFriendUiState: AddFriendUiState,
    actions: StudyTogetherActions,
    @DrawableRes userEdumonResId: Int,
) {
  val showAddDialog = addFriendUiState.showDialog
  val friendUidInput = addFriendUiState.friendUidInput
  val scope = rememberCoroutineScope()

  // List click: center camera on friend (no bottom bar)
  val onFriendPickedFromList: (FriendStatus) -> Unit = { friend ->
    actions.onFriendSelected(friend)
    scope.launch {
      cameraPositionState.safeAnimateTo(
          LatLng(friend.latitude, friend.longitude),
          zoom = DEFAULT_MAP_ZOOM,
          durationMs = CAMERA_ANIMATION_DURATION_MS)
    }
  }

  Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (showMap) {
            StudyMap(
                cameraPositionState = cameraPositionState,
                permissionsGranted = permissionsGranted,
                uiState = uiState,
                // clicking your own edumon should do nothing UI-wise
                onUserSelected = { /* no-op */},
                onFriendSelected = { friend -> actions.onFriendSelected(friend) },
                userMarkerResId = userEdumonResId,
                modifier = Modifier.matchParentSize())
          } else {
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag(TAG_MAP_STUB))
          }

          EdumonFriendsDropdown(
              friends = uiState.friends,
              onPick = onFriendPickedFromList,
              modifier =
                  Modifier.align(Alignment.TopStart)
                      .zIndex(1f)
                      .padding(start = 12.dp, top = 12.dp)
                      .testTag(TAG_BTN_FRIENDS))

          if (uiState.isLocationInitialized) {
            Box(
                modifier =
                    Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp).zIndex(1f)) {
                  CampusStatusChip(onCampus = uiState.isOnCampus)
                }
          }

          // Add friend FAB bottom-left
          Box(
              modifier = Modifier.fillMaxSize().padding(PADDING_LARGE_DP.dp),
              contentAlignment = Alignment.BottomStart) {
                AddFriendFab(onClick = actions.onAddFriendFabClick)
              }

          Box(
              modifier = Modifier.fillMaxSize().padding(bottom = 96.dp),
              contentAlignment = Alignment.BottomCenter) {
                GoBackToMeChip(
                    onClick = {
                      scope.launch {
                        cameraPositionState.safeAnimateTo(
                            uiState.effectiveUserLatLng,
                            zoom = DEFAULT_MAP_ZOOM,
                            durationMs = CAMERA_ANIMATION_DURATION_MS)
                      }
                    })
              }

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

@Composable
private fun OnCampusIndicatorTopRight(onCampus: Boolean) {
  val color = if (onCampus) ON_CAMPUS_GREEN else OFF_CAMPUS_RED
  val text = if (onCampus) "On campus" else "Off campus"

  Card(
      shape = RoundedCornerShape(CORNER_RADIUS_PILL_DP),
      colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.95f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
      }
}

@Composable
private fun StudyMap(
    cameraPositionState: CameraPositionState,
    permissionsGranted: Boolean,
    uiState: StudyTogetherUiState,
    onUserSelected: () -> Unit,
    onFriendSelected: (FriendStatus) -> Unit,
    @DrawableRes userMarkerResId: Int,
    modifier: Modifier = Modifier,
) {
  val userLatLng = uiState.effectiveUserLatLng
  val friends = uiState.friends
  val context = LocalContext.current

  val todoRepo = remember { AppRepositories.toDoRepository }
  val okHttpClient = remember { OkHttpClient() }
  val locationRepo = remember { NominatimLocationRepository(okHttpClient) }

  var todoMarkers by remember { mutableStateOf<List<TodoMarker>>(emptyList()) }

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

  val markerStates = remember { mutableStateMapOf<String, MarkerState>() }
  LaunchedEffect(friends) {
    val ids = friends.map { it.id }.toSet()
    (markerStates.keys - ids).forEach { markerStates.remove(it) }
  }

  // ✅ NEW BEHAVIOR:
  // - status appears above head ONLY for the selected friend marker
  // - disappears when you click on the map / elsewhere
  var selectedFriendId by remember { mutableStateOf<String?>(null) }

  fun clearSelection() {
    val id = selectedFriendId ?: return
    markerStates[id]?.hideInfoWindow()
    selectedFriendId = null
  }

  GoogleMap(
      modifier = modifier,
      cameraPositionState = cameraPositionState,
      properties =
          MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = permissionsGranted),
      onMapClick = { clearSelection() },
      onPOIClick = { clearSelection() },
  ) {
    val userIcon =
        remember(userMarkerResId) {
          BitmapDescriptorFactory.fromBitmap(
              loadDrawableAsBitmap(context, userMarkerResId, sizeDp = USER_MARKER_SIZE_DP))
        }
    val userMarkerState = remember { MarkerState(position = userLatLng) }
    // --- USER EDUMON MARKER (PLAIN, ALWAYS VISIBLE) ---
    Marker(
        state = userMarkerState,
        icon = userIcon,
        anchor = Offset(MARKER_ANCHOR_CENTER, MARKER_ANCHOR_CENTER),
        zIndex = USER_MARKER_Z_INDEX,
        onClick = {
          // No UI action — campus status is shown top-right
          true
        })

    LaunchedEffect(Unit) { userMarkerState.showInfoWindow() }

    LaunchedEffect(userLatLng) { userMarkerState.position = userLatLng }

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
          onClick = {
            clearSelection()
            false
          })
    }

    val friendsDistinct = remember(friends) { friends.distinctBy { it.id } }

    friendsDistinct.forEach { friend ->
      key(friend.id) {
        val target = LatLng(friend.latitude, friend.longitude)
        val state = markerStates.getOrPut(friend.id) { MarkerState(position = target) }

        LaunchedEffect(friend.latitude, friend.longitude) { state.position = target }

        val friendIcon =
            remember(friend.id) {
              BitmapDescriptorFactory.fromBitmap(
                  loadDrawableAsBitmap(
                      context, edumonFor(friend.id), sizeDp = FRIEND_MARKER_SIZE_DP))
            }

        Marker(
            state = state,
            icon = friendIcon,
            anchor = Offset(MARKER_ANCHOR_CENTER, MARKER_ANCHOR_CENTER),
            onClick = {
              // ❌ friend edumon does nothing
              true
            })
      }
    }
  }
}

@Composable
fun GoBackToMeChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme

  Card(
      modifier = modifier.clickable(onClick = onClick),
      shape = RoundedCornerShape(CORNER_RADIUS_PILL_DP),
      colors = CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = 0.7f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(cs.primary))
              Spacer(Modifier.width(10.dp))
              Text(text = "Go back to my location", style = MaterialTheme.typography.labelLarge)
            }
      }
}

/**
 * Visually appealing status pill in-app theme:
 * - surface background
 * - soft border using the mode color
 * - readable text + small colored dot
 * - subtle scale/alpha animation
 */
@Composable
private fun FriendStatusPill(mode: FriendMode, alpha: Float, scale: Float) {
  val cs = MaterialTheme.colorScheme
  val (label, accent) =
      when (mode) {
        FriendMode.STUDY -> "Studying" to cs.tertiary
        FriendMode.BREAK -> "Break" to cs.secondary
        FriendMode.IDLE -> "Idle" to cs.primary
      }

  val shape = RoundedCornerShape(14.dp)

  Row(
      modifier =
          Modifier.graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
              }
              .shadow(6.dp, shape, clip = false)
              .clip(shape)
              .background(cs.surface.copy(alpha = 0.94f))
              .border(1.dp, accent.copy(alpha = 0.55f), shape)
              .padding(horizontal = 10.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
      }
}

@Composable
private fun AddFriendFab(onClick: () -> Unit) {
  ExtendedFloatingActionButton(
      onClick = onClick,
      icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
      text = { Text("Add friend") },
      modifier = Modifier.testTag(TAG_FAB_ADD),
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

/**
 * Friends pill + menu:
 * - menu background transparent
 * - list background transparent
 * - list items opaque
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EdumonFriendsDropdown(
    friends: List<FriendStatus>,
    onPick: (FriendStatus) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Friends",
) {
  var expanded by remember { mutableStateOf(false) }
  val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrowRotation")

  val cs = MaterialTheme.colorScheme
  val pillShape = RoundedCornerShape(CORNER_RADIUS_PILL_DP)

  val borderBrush =
      remember(cs) {
        Brush.linearGradient(
            listOf(
                cs.primary.copy(alpha = 0.16f),
                cs.tertiary.copy(alpha = 0.12f),
                cs.secondary.copy(alpha = 0.12f),
            ))
      }

  Box(modifier = modifier) {
    Row(
        modifier =
            Modifier.defaultMinSize(minHeight = MIN_BUTTON_HEIGHT_DP.dp)
                .shadow(5.dp, pillShape, clip = false)
                .clip(pillShape)
                .background(cs.surface.copy(alpha = ALPHA_PILL_BG))
                .border(1.dp, borderBrush, pillShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                      expanded = !expanded
                    }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(26.dp)
                      .clip(CircleShape)
                      .background(cs.surfaceVariant.copy(alpha = 0.26f))
                      .border(1.dp, cs.outline.copy(alpha = 0.10f), CircleShape),
              contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.edumon1),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = cs.onSurface)
              }

          Spacer(Modifier.width(8.dp))

          Text(
              text = "$label ${friends.size}",
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)

          Spacer(Modifier.width(6.dp))

          Icon(
              imageVector = Icons.Filled.ExpandMore,
              contentDescription = null,
              modifier = Modifier.graphicsLayer { rotationZ = rotation },
              tint = cs.onSurfaceVariant)
        }

    AnimatedVisibility(
        visible = expanded,
        enter = slideInVertically { -it / 6 } + fadeIn(),
        exit = slideOutVertically { -it / 6 } + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 52.dp).fillMaxWidth()) {
          Card(
              modifier = Modifier.padding(horizontal = 12.dp),
              shape = RoundedCornerShape(CORNER_RADIUS_CARD_DP.dp),
              colors =
                  CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = ALPHA_MENU_BG)),
              elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
          ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Friends",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = false }) { Text("Close") }
              }

              Spacer(Modifier.height(8.dp))

              if (friends.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = cs.surfaceVariant.copy(alpha = 0.20f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text("No friends yet", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap “Add friend” to share your study status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant)
                  }
                }
              } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(0.dp)) {
                      itemsIndexed(friends) { _, friend ->
                        FriendDropdownRow(
                            friend = friend,
                            onClick = {
                              expanded = false
                              onPick(friend)
                            },
                            onFindClick = {
                              expanded = false
                              onPick(friend)
                            },
                            modifier = Modifier.fillMaxWidth())
                      }
                    }
              }

              Spacer(Modifier.height(8.dp))
            }
          }
        }
  }
}

@Composable
fun CampusStatusChip(onCampus: Boolean, modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme
  val dotColor = if (onCampus) ON_CAMPUS_GREEN else OFF_CAMPUS_RED
  val label = if (onCampus) "On campus" else "Off campus"

  Row(
      modifier =
          modifier
              .clip(RoundedCornerShape(CORNER_RADIUS_PILL_DP))
              .background(cs.surfaceVariant.copy(alpha = 0.75f))
              .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // ● Status dot
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))

        Spacer(Modifier.width(8.dp))

        // Text
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = cs.onSurface)

        Spacer(Modifier.width(8.dp))

        // EPFL logo
        Icon(
            painter = painterResource(id = R.drawable.epfl),
            contentDescription = "EPFL",
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified)
      }
}

@Composable
fun FriendDropdownRow(
    friend: FriendStatus,
    onClick: () -> Unit,
    onFindClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  val rowShape = RoundedCornerShape(CORNER_RADIUS_ROW_DP.dp)

  Row(
      modifier =
          modifier
              .shadow(4.dp, rowShape, clip = false)
              .clip(rowShape)
              .background(cs.surfaceVariant.copy(alpha = ALPHA_ITEM_BG))
              .clickable(onClick = onClick)
              .padding(horizontal = 10.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // Avatar
        Box(
            modifier =
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .background(cs.surface.copy(alpha = 0.92f))
                    .border(1.dp, cs.outline.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center) {
              Icon(
                  painter = painterResource(id = edumonFor(friend.id)),
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                  tint = cs.onSurface)
            }

        Spacer(Modifier.width(10.dp))

        // Name
        Text(
            text = friend.name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)

        // ✅ Status back on the right
        StatusChip(mode = friend.mode)

        Spacer(Modifier.width(8.dp))

        // Find arrow button
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = "Find on map",
            modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onFindClick),
            tint = cs.primary)
      }
}

@Composable
private fun OnCampusIndicator(modifier: Modifier = Modifier, onCampus: Boolean) {
  val cs = MaterialTheme.colorScheme
  val statusColor = if (onCampus) cs.tertiary else cs.error
  val label = if (onCampus) "On EPFL campus" else "Outside of EPFL campus"

  Card(
      modifier = modifier,
      shape = RoundedCornerShape(CORNER_RADIUS_PILL_DP),
      colors = CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = 0.60f)),
      elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_CARD_DP.dp)) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = PADDING_STANDARD_DP.dp, vertical = PADDING_MEDIUM_DP.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Box(
                  modifier =
                      Modifier.size(ICON_SIZE_MEDIUM_DP.dp)
                          .clip(CircleShape)
                          .background(statusColor))
              Spacer(Modifier.width(SPACING_SMALL_DP.dp))
              Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
      }
}

@Composable
private fun OnCampusIndicatorCompact(modifier: Modifier = Modifier, onCampus: Boolean) {
  val cs = MaterialTheme.colorScheme
  val statusColor = if (onCampus) ON_CAMPUS_GREEN else OFF_CAMPUS_RED
  val label = if (onCampus) "On campus" else "Off campus"

  Card(
      modifier = modifier,
      shape = RoundedCornerShape(CORNER_RADIUS_PILL_DP),
      colors = CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = 0.60f)),
      elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_CARD_DP.dp)) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = PADDING_STANDARD_DP.dp, vertical = PADDING_MEDIUM_DP.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Box(
                  modifier =
                      Modifier.size(ICON_SIZE_MEDIUM_DP.dp)
                          .clip(CircleShape)
                          .background(statusColor))
              Spacer(Modifier.width(SPACING_SMALL_DP.dp))
              Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
      }
}

@Composable
fun StatusChip(mode: FriendMode) {
  val cs = MaterialTheme.colorScheme
  val (label, baseColor) =
      when (mode) {
        FriendMode.STUDY -> "Studying" to cs.tertiary
        FriendMode.BREAK -> "Break" to cs.secondary
        FriendMode.IDLE -> "Idle" to cs.primary
      }

  Box(
      modifier =
          Modifier.clip(RoundedCornerShape(CORNER_RADIUS_PILL_DP))
              .background(baseColor.copy(alpha = ALPHA_STATUS_CHIP_BG))
              .padding(horizontal = ICON_SIZE_MEDIUM_DP.dp, vertical = PADDING_SMALL_DP.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(ICON_SIZE_SMALL_DP.dp).clip(CircleShape).background(baseColor))
          Spacer(Modifier.width(SPACING_TINY_DP.dp))
          Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
      }
}

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

internal fun persistLastLocation(ctx: Context, lat: Double, lon: Double) {
  ctx.getSharedPreferences("last_location", Context.MODE_PRIVATE).edit {
    putFloat("lat", lat.toFloat())
    putFloat("lon", lon.toFloat())
  }
}
