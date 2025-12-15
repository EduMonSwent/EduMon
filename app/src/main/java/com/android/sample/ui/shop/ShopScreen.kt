package com.android.sample.ui.shop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.core.helpers.NetworkConnectivityObserver
import com.android.sample.ui.theme.*
import kotlin.random.Random

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

@Composable
fun ShopScreen(viewModel: ShopViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
  val context = LocalContext.current

  val userCoins by viewModel.userCoins.collectAsState()
  val items by viewModel.items.collectAsState()
  val isOnline by viewModel.isOnline.collectAsState()
  val lastPurchaseResult by viewModel.lastPurchaseResult.collectAsState()
  val isPurchasing by viewModel.isPurchasing.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  // Observe network connectivity
  LaunchedEffect(Unit) {
    NetworkConnectivityObserver.observe(context).collect { connected ->
      viewModel.setNetworkStatus(connected)
    }
  }

  // Handle purchase result feedback
  LaunchedEffect(lastPurchaseResult) {
    lastPurchaseResult?.let { result ->
      val message =
          when (result) {
            is PurchaseResult.Success -> "🎉 You purchased ${result.itemName}!"
            is PurchaseResult.InsufficientCoins -> "❌ Not enough coins for ${result.itemName}"
            is PurchaseResult.AlreadyOwned -> "✓ You already own ${result.itemName}"
            is PurchaseResult.NoConnection -> "📡 No internet connection"
            is PurchaseResult.NetworkError -> "⚠️ ${result.message}"
          }
      snackbarHostState.showSnackbar(message)
      viewModel.clearPurchaseResult()
    }
  }

  Scaffold(
      containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbarHostState) }) {
          innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
          ShopContent(
              userCoins = userCoins,
              items = items,
              isOnline = isOnline,
              isPurchasing = isPurchasing,
              modifier = Modifier.padding(innerPadding),
              onBuy = { item, triggerSuccess, triggerFail ->
                val initiated = viewModel.buyItem(item)
                if (initiated) {
                  triggerSuccess()
                } else {
                  triggerFail()
                }
              })

          // Offline banner at top
          AnimatedVisibility(
              visible = !isOnline,
              enter = slideInVertically { -it } + fadeIn(),
              exit = slideOutVertically { -it } + fadeOut(),
              modifier = Modifier.align(Alignment.TopCenter)) {
                OfflineBanner()
              }
        }
      }
}

/** Banner displayed when device is offline. */
@Composable
private fun OfflineBanner() {
  Surface(
      color = Color(0xFFE53935),
      shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
      shadowElevation = 8.dp,
      modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = Icons.Outlined.CloudOff,
                  contentDescription = "Offline",
                  tint = Color.White,
                  modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                  text = "You're offline — purchases are disabled",
                  color = Color.White,
                  fontWeight = FontWeight.Medium,
                  fontSize = 14.sp)
            }
      }
}

@Composable
fun ShopContent(
    userCoins: Int,
    items: List<CosmeticItem>,
    isOnline: Boolean,
    isPurchasing: Boolean,
    onBuy: (CosmeticItem, () -> Unit, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
  val localColorScheme = MaterialTheme.colorScheme
  val glowAlpha by
      rememberInfiniteTransition(label = "glow")
          .animateFloat(
              initialValue = 0.3f,
              targetValue = 0.8f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(2000, easing = LinearEasing),
                      repeatMode = RepeatMode.Reverse),
              label = "glowAlpha")

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(
                  Brush.verticalGradient(
                      listOf(localColorScheme.background, localColorScheme.surface)))
              .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        // Add top padding when offline banner is showing
        if (!isOnline) {
          Spacer(modifier = Modifier.height(48.dp))
        }

        Text(
            text = "EduMon Shop",
            fontWeight = FontWeight.Bold,
            color = localColorScheme.primary,
            fontSize = 26.sp)

        Spacer(modifier = Modifier.height(8.dp))

        // Connection status indicator
        ConnectionStatusChip(isOnline = isOnline)

        Spacer(modifier = Modifier.height(12.dp))

        // Coin Balance Card
        CoinBalanceCard(userCoins = userCoins, glowAlpha = glowAlpha)

        Spacer(modifier = Modifier.height(20.dp))

        // Item grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)) {
              items(items) { item ->
                ShopItemCard(
                    item = item,
                    isOnline = isOnline,
                    isPurchasing = isPurchasing,
                    onBuy = { success, fail -> onBuy(item, success, fail) })
              }
            }
      }
}

/** Small chip showing online/offline status. */
@Composable
private fun ConnectionStatusChip(isOnline: Boolean) {
  val backgroundColor = if (isOnline) Color(0xFF43A047) else Color(0xFF757575)
  val icon = if (isOnline) Icons.Outlined.Wifi else Icons.Outlined.CloudOff
  val text = if (isOnline) "Online" else "Offline"
  val testTagValue = if (isOnline) "connection_status_online" else "connection_status_offline"

  Surface(
      color = backgroundColor.copy(alpha = 0.2f),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier.testTag(testTagValue)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = icon,
                  contentDescription = text,
                  tint = backgroundColor,
                  modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                  text = text,
                  color = backgroundColor,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium)
            }
      }
}

/** Card displaying user's coin balance. */
@Composable
private fun CoinBalanceCard(userCoins: Int, glowAlpha: Float) {
  Card(
      colors = CardDefaults.cardColors(containerColor = MidDarkCard),
      shape = RoundedCornerShape(20.dp),
      modifier =
          Modifier.fillMaxWidth(0.9f)
              .shadow(
                  elevation = 8.dp,
                  spotColor = AccentViolet.copy(alpha = glowAlpha),
                  ambientColor = AccentViolet.copy(alpha = glowAlpha),
                  shape = RoundedCornerShape(20.dp))) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = "Your Coins",
                  color = TextLight.copy(alpha = 0.8f),
                  fontWeight = FontWeight.Medium,
                  fontSize = 16.sp)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AttachMoney,
                    contentDescription = "Coin Icon",
                    tint = AccentViolet,
                    modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$userCoins",
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp)
              }
            }
      }
}

@Composable
fun ShopItemCard(
    item: CosmeticItem,
    isOnline: Boolean,
    isPurchasing: Boolean,
    onBuy: ((() -> Unit), (() -> Unit)) -> Unit
) {
  var scale by remember { mutableStateOf(1f) }
  var particles by remember { mutableStateOf(emptyList<Offset>()) }

  // Determine if purchase is allowed
  val canPurchase = isOnline && !item.owned && !isPurchasing

  // Capture colors for use in non-Composable contexts
  val localColorScheme = MaterialTheme.colorScheme
  val particleColor = localColorScheme.primary
  val textColor = localColorScheme.onSurface
  val surfaceColor = localColorScheme.surface

  Card(
      colors = CardDefaults.cardColors(containerColor = surfaceColor),
      shape = RoundedCornerShape(18.dp),
      modifier =
          Modifier.fillMaxWidth()
              .aspectRatio(0.9f)
              .graphicsLayer {
                if (scale != 1f) {
                  scaleX = scale
                  scaleY = scale
                }
              }
              .alpha(if (!isOnline && !item.owned) 0.5f else 1f)
              .clickable(enabled = canPurchase) {
                onBuy(
                    {
                      // Success animation
                      scale = 1.1f
                      particles = generateParticles()
                    },
                    {
                      // Fail animation
                      scale = 0.95f
                    })
              }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          // Particles canvas
          Canvas(modifier = Modifier.matchParentSize()) {
            particles.forEach { p ->
              drawCircle(
                  color = particleColor.copy(alpha = Random.nextFloat()), radius = 4f, center = p)
            }
          }

          Column(
              modifier = Modifier.fillMaxSize().padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween) {
                Image(
                    painter = painterResource(id = item.imageRes),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(80.dp).fillMaxWidth())

                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textColor,
                    textAlign = TextAlign.Center)

                when {
                  item.owned -> {
                    // Already owned indicator
                    Text(
                        text = "✓ Owned",
                        color = AccentViolet.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp)
                  }
                  !isOnline -> {
                    // Offline indicator
                    Text(
                        text = "Offline",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.testTag("item_offline_indicator"))
                  }
                  else -> {
                    // Buy button
                    Button(
                        onClick = {
                          onBuy(
                              {
                                scale = 1.1f
                                particles = generateParticles()
                              },
                              { scale = 0.95f })
                        },
                        enabled = canPurchase,
                        shape = RoundedCornerShape(10.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = AccentViolet,
                                disabledContainerColor = AccentViolet.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()) {
                          if (isPurchasing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp)
                          } else {
                            Icon(
                                imageVector = Icons.Outlined.AttachMoney,
                                contentDescription = "Coin Icon",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${item.price}", color = Color.White, fontWeight = FontWeight.Bold)
                          }
                        }
                  }
                }
              }
        }
      }
}

/** Generates random sparkle particles for purchase animation. */
internal fun generateParticles(): List<Offset> {
  return List(20) { Offset(x = Random.nextFloat() * 200f, y = Random.nextFloat() * 200f) }
}
