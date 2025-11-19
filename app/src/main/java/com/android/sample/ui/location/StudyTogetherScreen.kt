package com.android.sample.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Log.e
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
import com.android.sample.ui.theme.BreakYellow
import com.android.sample.ui.theme.IdleBlue
import com.android.sample.ui.theme.IndicatorRed
import com.android.sample.ui.theme.StudyGreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
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

private const val TAG_FAB_ADD = "fab_add_friend"
private const val TAG_FIELD_UID = "field_friend_uid"
private const val TAG_BTN_FRIENDS = "btn_friends"
private const val TAG_MAP_STUB = "map_stub"

private const val ON_CAMPUS = "on_campus_indicator"

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

@SuppressLint("MissingPermission")
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
  val context = LocalContext.current
  val permissions =
      rememberMultiplePermissionsState(
          listOf(
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
  val uiState by viewModel.uiState.collectAsState()

  var showAddDialog by remember { mutableStateOf(false) }
  var friendUidInput by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }

  // Ask permission, then feed VM one-shot last known location
  LaunchedEffect(Unit) { permissions.launchMultiplePermissionRequest() }

  LaunchedEffect(permissions.allPermissionsGranted) {
    if (permissions.allPermissionsGranted) {
      LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener {
          loc ->
        if (chooseLocation) {
          viewModel.consumeLocation(chosenLocation.latitude, chosenLocation.longitude)
        } else loc?.let { viewModel.consumeLocation(it.latitude, it.longitude) }
      }
    }
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { raw ->
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
      viewModel.consumeError()
    }
  }

  val cameraPosition = rememberCameraPositionState()
  // Center on the user (EPFL by default, then new GPS forwarded by UI)
  LaunchedEffect(uiState.effectiveUserLatLng) {
    cameraPosition.safeAnimateTo(uiState.effectiveUserLatLng, zoom = 16f, durationMs = 600)
  }

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
          StudyMap(
              showMap = showMap,
              cameraPositionState = cameraPositionState,
              permissionsGranted = permissionsGranted,
              uiState = uiState,
              onUserSelected = actions.onUserSelected,
              onFriendSelected = actions.onFriendSelected,
              modifier = Modifier.matchParentSize())

          // On-campus indicator
          OnCampusIndicator(
              modifier =
                  Modifier.align(Alignment.TopCenter).padding(top = 12.dp).testTag(ON_CAMPUS),
              uiState.isOnCampus)

          // Compact friends dropdown
          EdumonFriendsDropdown(
              friends = uiState.friends,
              onPick = actions.onFriendSelected,
              modifier =
                  Modifier.align(Alignment.TopStart)
                      .padding(12.dp, top = 72.dp)
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
    showMap: Boolean,
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

  if (!showMap) {
    // Simple stub so layout stays similar in tests
    Box(
        modifier =
            modifier.background(MaterialTheme.colorScheme.surfaceVariant).testTag(TAG_MAP_STUB))
    return
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
              loadDrawableAsBitmap(context, R.drawable.edumon, sizeDp = 44f))
        }
        val userMarkerState = remember { MarkerState(position = userLatLng) }
        LaunchedEffect(userLatLng) { userMarkerState.position = userLatLng }

        Marker(
            state = userMarkerState,
            title = "You",
            icon = userIcon,
            anchor = Offset(0.5f, 0.5f),
            zIndex = 1f,
            onClick = {
              onUserSelected()
              true
            })

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
                  val bmp = loadDrawableAsBitmap(context, iconRes, sizeDp = 40f)
                  BitmapDescriptorFactory.fromBitmap(bmp)
                }
            Marker(
                state = state,
                title = friend.name,
                icon = friendIcon,
                anchor = Offset(0.5f, 0.5f),
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
      modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
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
      modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(TAG_FAB_ADD),
      shape = RoundedCornerShape(24.dp),
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
      shape = RoundedCornerShape(24.dp),
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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.defaultMinSize(minHeight = 36.dp),
        shape = RoundedCornerShape(50), // full pill
        colors =
            ButtonDefaults.filledTonalButtonColors(
                // Use the same color as your campus indicator card
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            )) {
          Icon(
              painter = painterResource(id = R.drawable.edumon1),
              contentDescription = null,
              modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("$label (${friends.size})")
        }

    // Our own "dropdown", drawn as a Card → only one background, fully rounded
    if (expanded) {
      Card(
          modifier =
              Modifier.align(Alignment.TopStart)
                  .padding(top = 44.dp) // show under the pill
                  .widthIn(min = 220.dp, max = 320.dp),
          shape = RoundedCornerShape(24.dp),
          colors =
              CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
            if (friends.isEmpty()) {
              Text(
                  text = "No friends yet",
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                  style = MaterialTheme.typography.bodyMedium)
            } else {
              Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))

                friends.forEach { friend ->
                  FriendDropdownRow(
                      friend = friend,
                      onClick = {
                        expanded = false
                        onPick(friend)
                      },
                      modifier = Modifier.fillMaxWidth())
                  Spacer(Modifier.height(9.dp))
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
              .clip(RoundedCornerShape(18.dp))
              .clickable(onClick = onClick)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
              .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = edumonFor(friend.id)),
            contentDescription = null,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            friend.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        StatusChip(mode = friend.mode)
      }
}

@Composable
private fun OnCampusIndicator(modifier: Modifier = Modifier, onCampus: Boolean) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(50),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        if (onCampus) {
          Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically) {
                // Green dot
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(StudyGreen))
                Spacer(Modifier.width(8.dp))
                Text(text = "On EPFL campus", style = MaterialTheme.typography.labelLarge)
              }
        } else {
          Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically) {
                // Green dot
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(IndicatorRed))
                Spacer(Modifier.width(8.dp))
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
          Modifier.clip(RoundedCornerShape(50))
              .background(bg.copy(alpha = 0.18f))
              .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(bg))
          Spacer(Modifier.width(6.dp))
          Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
      }
}

/* ---------- Info cards ---------- */

@Composable
fun UserStatusCard(isStudyMode: Boolean, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Text(
            text = if (isStudyMode) "You’re studying" else "You’re on a break",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium)
      }
}

@Composable
fun FriendInfoCard(name: String, mode: FriendMode, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Row(Modifier.padding(16.dp)) {
          Text(text = name, style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(6.dp))
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
