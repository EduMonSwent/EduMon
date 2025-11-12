package com.android.sample.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
        loc?.let { viewModel.consumeLocation(it.latitude, it.longitude) }
      }
    }
  }

  // One-shot error display
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.consumeError()
    }
  }

  val cameraPosition = rememberCameraPositionState()
  // Center on the user (EPFL by default, then new GPS forwarded by UI)
  LaunchedEffect(uiState.effectiveUserLatLng) {
    cameraPosition.safeAnimateTo(uiState.effectiveUserLatLng, zoom = 16f, durationMs = 600)
  }

  // One MarkerState per friend id (prevents association crash)
  val markerStates = remember { mutableStateMapOf<String, MarkerState>() }
  LaunchedEffect(uiState.friends) {
    val ids = uiState.friends.map { it.id }.toSet()
    (markerStates.keys - ids).forEach { markerStates.remove(it) }
  }

  Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
    Box(Modifier.fillMaxSize().padding(innerPadding)) {
      if (showMap) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPosition,
            properties =
                MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = permissions.allPermissionsGranted)) {
              // --- User marker ---
              val userIcon = remember {
                BitmapDescriptorFactory.fromBitmap(
                    loadDrawableAsBitmap(context, R.drawable.edumon, sizeDp = 44f))
              }
              val userMarkerState = remember { MarkerState(position = uiState.effectiveUserLatLng) }
              LaunchedEffect(uiState.effectiveUserLatLng) {
                userMarkerState.position = uiState.effectiveUserLatLng
              }
              Marker(
                  state = userMarkerState,
                  title = "You",
                  icon = userIcon,
                  anchor = Offset(0.5f, 0.5f),
                  zIndex = 1f,
                  onClick = {
                    viewModel.selectUser()
                    true
                  })

              // --- Friend markers ---
              val friendsDistinct =
                  remember(uiState.friends) { uiState.friends.distinctBy { it.id } }
              friendsDistinct.forEach { friend ->
                key(friend.id) {
                  val target = LatLng(friend.latitude, friend.longitude)
                  val state = markerStates.getOrPut(friend.id) { MarkerState(position = target) }
                  LaunchedEffect(friend.id, friend.latitude, friend.longitude) {
                    state.position = target
                  }
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
                        viewModel.selectFriend(friend)
                        true
                      })
                }
              }
            }
      } else {
        // Simple stub so layout stays similar in tests
        Box(
            Modifier.fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Transparent)
                .testTag(TAG_MAP_STUB))
      }

      // ---------- Compact Edumon friends dropdown (top-left) ----------
      EdumonFriendsDropdown(
          friends = uiState.friends,
          onPick = { viewModel.selectFriend(it) },
          modifier = Modifier.align(Alignment.TopStart).padding(12.dp).testTag(TAG_BTN_FRIENDS))

      // ---------- Bottom info cards ----------
      AnimatedVisibility(
          visible = uiState.selectedFriend != null || uiState.isUserSelected,
          enter = slideInVertically { it } + fadeIn(),
          exit = slideOutVertically { it } + fadeOut(),
          modifier = Modifier.align(Alignment.BottomCenter)) {
            when {
              uiState.isUserSelected ->
                  UserStatusCard(
                      isStudyMode = true,
                      modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter))
              uiState.selectedFriend != null ->
                  FriendInfoCard(
                      name = uiState.selectedFriend!!.name,
                      mode = uiState.selectedFriend!!.mode,
                      modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter))
            }
          }

      // ---------- Add friend FAB ----------
      ExtendedFloatingActionButton(
          onClick = { showAddDialog = true },
          icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
          text = { Text("Add friend") },
          modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(TAG_FAB_ADD))

      if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add friend by UID") },
            text = {
              OutlinedTextField(
                  value = friendUidInput,
                  onValueChange = { friendUidInput = it },
                  label = { Text("Friend UID") },
                  singleLine = true,
                  modifier = Modifier.testTag(TAG_FIELD_UID))
            },
            confirmButton = {
              TextButton(
                  enabled = friendUidInput.isNotBlank(),
                  onClick = {
                    val uid = friendUidInput.trim()
                    if (uid.isNotEmpty()) viewModel.addFriendByUid(uid)
                    friendUidInput = ""
                    showAddDialog = false
                  }) {
                    Text("Add")
                  }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } })
      }
    }
  }
}

/* ---------- Friends dropdown (compact, “edumon” row) ---------- */

@Composable
private fun EdumonFriendsDropdown(
    friends: List<FriendStatus>,
    onPick: (FriendStatus) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Friends"
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    FilledTonalButton(
        onClick = { expanded = true },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.defaultMinSize(minHeight = 34.dp)) {
          Icon(
              painter = painterResource(id = R.drawable.edumon1),
              contentDescription = null,
              modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("$label (${friends.size})")
        }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      if (friends.isEmpty()) {
        DropdownMenuItem(text = { Text("No friends yet") }, onClick = { expanded = false })
      } else {
        friends.forEach { friend ->
          DropdownMenuItem(
              text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
              },
              onClick = {
                expanded = false
                onPick(friend)
              })
        }
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
              .padding(horizontal = 8.dp, vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(bg))
          Spacer(Modifier.width(6.dp))
          Text(label)
        }
      }
}

/**
 * Minimal cards to keep this file self-contained. Replace with your own UI if you already have it.
 */
@Composable
fun UserStatusCard(isStudyMode: Boolean, modifier: Modifier = Modifier) {
  Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
    Text(
        text = if (isStudyMode) "You’re studying" else "You’re on a break",
        modifier = Modifier.padding(16.dp))
  }
}

@Composable
fun FriendInfoCard(name: String, mode: FriendMode, modifier: Modifier = Modifier) {
  Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
    Column(Modifier.padding(16.dp)) {
      Text(text = name)
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

private fun dp2px(context: android.content.Context, dp: Float): Int =
    (dp * context.resources.displayMetrics.density).toInt()

private fun loadDrawableAsBitmap(
    context: android.content.Context,
    resId: Int,
    sizeDp: Float
): Bitmap {
  val d = AppCompatResources.getDrawable(context, resId) ?: error("Drawable $resId not found")
  val px = dp2px(context, sizeDp)
  return d.toBitmap(width = px, height = px, config = android.graphics.Bitmap.Config.ARGB_8888)
}

private suspend fun CameraPositionState.safeAnimateTo(
    latLng: LatLng,
    zoom: Float,
    durationMs: Int
) {
  try {
    animate(CameraUpdateFactory.newLatLngZoom(latLng, zoom), durationMs)
  } catch (_: Exception) {}
}
