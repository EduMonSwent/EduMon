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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

data class FriendStatus(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val status: String // "study", "break", "idle"
)

@SuppressLint("MissingPermission")
@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class
)
@Composable
fun StudyTogetherScreen() {
    val context = LocalContext.current
    val permission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var userPosition by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var selectedFriend by remember { mutableStateOf<FriendStatus?>(null) }
    var isUserSelected by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) { permission.launchPermissionRequest() }


    if (permission.status.isGranted) {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { loc ->
            loc?.let { userPosition = it.latitude to it.longitude }
        }
    }


    val epfl = LatLng(46.5191, 6.5668)

    // 👥 Amis fictifs (chacun avec un avatar différent)
    val friends = listOf(
        FriendStatus("Alae", 46.5208, 6.5674, "study"),
        FriendStatus("Florian", 46.5186, 6.5649, "break"),
        FriendStatus("Khalil", 46.5197, 6.5702, "idle")
    )

    val cameraPosition = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(epfl, 16f)
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPosition,
            properties = MapProperties(mapType = MapType.NORMAL)
        ) {
            // 🧠 Ton EduMon (avec halo doré bien centré)
            val baseEduMon = BitmapFactory.decodeResource(context.resources, R.drawable.edumon)
            val scaledEduMon = Bitmap.createScaledBitmap(baseEduMon, 120, 120, false)

            val glowRadius = 25f
            val glowColor = android.graphics.Color.argb(200, 255, 215, 0) // doré clair
            val canvasSize = 180
            val glowBitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(glowBitmap)
            val paint = Paint().apply {
                color = glowColor
                maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
                isAntiAlias = true
            }

            // Dessine un halo bien centré
            val center = canvasSize / 2f
            canvas.drawCircle(center, center, 65f, paint)

            // Dessine ton EduMon au centre du halo
            val offset = (canvasSize - scaledEduMon.width) / 2f
            canvas.drawBitmap(scaledEduMon, offset, offset, null)

            val userIcon = BitmapDescriptorFactory.fromBitmap(glowBitmap)
            val userLatLng = userPosition?.let { LatLng(it.first, it.second) } ?: epfl

            Marker(
                state = MarkerState(userLatLng),
                title = "You",
                icon = userIcon,
                onClick = {
                    selectedFriend = null
                    isUserSelected = true
                    true
                }
            )

            // 🧩 Amis avec leurs EduMon
            val friendIcons = listOf(
                R.drawable.edumon1,
                R.drawable.edumon2,
                R.drawable.edumon3
            )

            friends.forEachIndexed { index, friend ->
                val friendBitmap = BitmapFactory.decodeResource(context.resources, friendIcons[index % friendIcons.size])
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
                    }
                )
            }
        }



        AnimatedVisibility(
            visible = selectedFriend != null || isUserSelected,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            when {
                isUserSelected -> {
                    UserStatusCard(
                        isStudyMode = true,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    )
                }

                selectedFriend != null -> {
                    FriendInfoCard(
                        name = selectedFriend!!.name,
                        status = selectedFriend!!.status,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun UserStatusCard(isStudyMode: Boolean, modifier: Modifier = Modifier) {
    val color = if (isStudyMode) Color(0xFF7C4DFF) else Color(0xFFBDBDBD)

    Column(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.9f)),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🧠 You’re in ${if (isStudyMode) "Study Mode" else "Break Mode"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun FriendInfoCard(name: String, status: String, modifier: Modifier = Modifier) {
    val (emoji, color) = when (status) {
        "study" -> "📚" to Color(0xFF4CAF50)
        "break" -> "☕" to Color(0xFFFFB300)
        else -> "💤" to Color(0xFF2196F3)
    }

    Column(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.9f)),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$emoji $name is currently in ${status.replaceFirstChar { it.uppercase() }} Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
