package com.android.sample.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.*
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

private val EPFL_LAT_LNG = LatLng(46.5191, 6.5668)

enum class FriendMode(val emoji: String, val color: Color) {
  STUDY("📚", StudyGreen),
  BREAK("☕", BreakYellow),
  IDLE("💤", IdleBlue)
}

data class FriendStatus(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val mode: FriendMode
)

@SuppressLint("MissingPermission")
@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class)
@Composable
fun StudyTogetherScreen(friends: List<FriendStatus> = defaultFriends) {
  val context = LocalContext.current
  val permissions =
      rememberMultiplePermissionsState(
          listOf(
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

  var userPosition by remember { mutableStateOf<Pair<Double, Double>?>(null) }
  var selectedFriend by remember { mutableStateOf<FriendStatus?>(null) }
  var isUserSelected by remember { mutableStateOf(false) }

  LaunchedEffect(permissions.allPermissionsGranted) {
    permissions.launchMultiplePermissionRequest()
    if (permissions.allPermissionsGranted) {
      val fused = LocationServices.getFusedLocationProviderClient(context)
      fused.lastLocation.addOnSuccessListener { loc ->
        loc?.let { userPosition = it.latitude to it.longitude }
      }
    }
  }

  val cameraPosition = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(EPFL_LAT_LNG, 16f)
  }

  Box(Modifier.fillMaxSize()) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPosition,
        properties = MapProperties(mapType = MapType.NORMAL)) {
          val userIcon =
              remember(userPosition) {
                val baseEduMon = BitmapFactory.decodeResource(context.resources, R.drawable.edumon)
                val scaledEduMon = Bitmap.createScaledBitmap(baseEduMon, 120, 120, false)

                val glowRadius = 25f
                val glowColor = GlowGold.copy(alpha = 0.8f).toArgb()
                val canvasSize = 180
                val glowBitmap =
                    Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(glowBitmap)
                val paint =
                    Paint().apply {
                      color = glowColor
                      maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
                      isAntiAlias = true
                    }

                val center = canvasSize / 2f
                canvas.drawCircle(center, center, 65f, paint)

                val offset = (canvasSize - scaledEduMon.width) / 2f
                canvas.drawBitmap(scaledEduMon, offset, offset, null)
                BitmapDescriptorFactory.fromBitmap(glowBitmap)
              }

          val userLatLng = userPosition?.let { LatLng(it.first, it.second) } ?: EPFL_LAT_LNG

          Marker(
              state = MarkerState(userLatLng),
              title = "You",
              icon = userIcon,
              onClick = {
                selectedFriend = null
                isUserSelected = true
                true
              })

          val friendIcons = listOf(R.drawable.edumon1, R.drawable.edumon2, R.drawable.edumon3)
          friends.forEachIndexed { index, friend ->
            val friendBitmap =
                BitmapFactory.decodeResource(
                    context.resources, friendIcons[index % friendIcons.size])
            val scaled = Bitmap.createScaledBitmap(friendBitmap, 100, 100, false)
            val friendIcon = BitmapDescriptorFactory.fromBitmap(scaled)

            Marker(
                state = MarkerState(LatLng(friend.latitude, friend.longitude)),
                title = friend.name,
                icon = friendIcon,
                onClick = {
                  selectedFriend = friend
                  isUserSelected = false
                  true
                })
          }
        }

    AnimatedVisibility(
        visible = selectedFriend != null || isUserSelected,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)) {
          when {
            isUserSelected -> {
              UserStatusCard(
                  isStudyMode = true,
                  modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter))
            }
            selectedFriend != null -> {
              FriendInfoCard(
                  name = selectedFriend!!.name,
                  mode = selectedFriend!!.mode,
                  modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter))
            }
          }
        }
  }
}

private val defaultFriends =
    listOf(
        FriendStatus("Alae", 46.5208, 6.5674, FriendMode.STUDY),
        FriendStatus("Florian", 46.5186, 6.5649, FriendMode.BREAK),
        FriendStatus("Khalil", 46.5197, 6.5702, FriendMode.IDLE))

@Composable
fun UserStatusCard(isStudyMode: Boolean, modifier: Modifier = Modifier) {
  val color =
      if (isStudyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

  Column(
      modifier = modifier.fillMaxWidth(0.9f).wrapContentHeight().testTag("user_status_card"),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.9f)),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(8.dp)) {
              Column(
                  modifier = Modifier.padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text =
                            stringResource(
                                R.string.user_mode_text,
                                if (isStudyMode) stringResource(R.string.study_mode)
                                else stringResource(R.string.break_mode)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary)
                  }
            }
        Spacer(Modifier.height(40.dp))
      }
}

@Composable
fun FriendInfoCard(name: String, mode: FriendMode, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth(0.9f).wrapContentHeight().testTag("friend_info_card"),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            colors = CardDefaults.cardColors(containerColor = mode.color.copy(alpha = 0.9f)),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(8.dp)) {
              Column(
                  modifier = Modifier.padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text =
                            stringResource(
                                R.string.friend_mode_text,
                                mode.emoji,
                                name,
                                mode.name.lowercase().replaceFirstChar { it.uppercase() }),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary)
                  }
            }
        Spacer(Modifier.height(40.dp))
      }
}
