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
import androidx.compose.ui.platform.LocalInspectionMode
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

// UI Constants
private const val SHOP_TITLE = "EduMon Shop"
private const val YOUR_COINS_LABEL = "Your Coins"
private const val OWNED_TEXT = "✓ Owned"
private const val OFFLINE_TEXT = "Offline"
private const val OFFLINE_BANNER_TEXT = "You're offline — purchases are disabled"
private const val ONLINE_TEXT = "Online"
private const val COIN_ICON_DESCRIPTION = "Coin Icon"

// Purchase Result Messages
private const val PURCHASE_SUCCESS_PREFIX = "🎉 You purchased "
private const val PURCHASE_SUCCESS_SUFFIX = "!"
private const val INSUFFICIENT_COINS_PREFIX = "❌ Not enough coins for "
private const val ALREADY_OWNED_PREFIX = "✓ You already own "
private const val NO_CONNECTION_MESSAGE = "📡 No internet connection"
private const val NETWORK_ERROR_PREFIX = "⚠️ "

// Dimensions
private val OFFLINE_BANNER_CORNER_SIZE = 16.dp
private val OFFLINE_BANNER_ELEVATION = 8.dp
private val OFFLINE_BANNER_PADDING_HORIZONTAL = 16.dp
private val OFFLINE_BANNER_PADDING_VERTICAL = 12.dp
private val OFFLINE_BANNER_ICON_SIZE = 20.dp
private val OFFLINE_BANNER_SPACER_WIDTH = 8.dp
private val OFFLINE_BANNER_TEXT_SIZE = 14.sp

private val SHOP_CONTENT_PADDING = 16.dp
private val OFFLINE_TOP_SPACER_HEIGHT = 48.dp
private val SHOP_TITLE_SIZE = 26.sp
private val TITLE_SPACER_HEIGHT = 8.dp
private val STATUS_CHIP_SPACER_HEIGHT = 12.dp
private val COIN_CARD_SPACER_HEIGHT = 20.dp
private val GRID_BOTTOM_PADDING = 80.dp
private val GRID_ITEM_SPACING = 16.dp

private val CONNECTION_CHIP_CORNER_SIZE = 20.dp
private val CONNECTION_CHIP_PADDING_HORIZONTAL = 12.dp
private val CONNECTION_CHIP_PADDING_VERTICAL = 6.dp
private val CONNECTION_CHIP_ICON_SIZE = 14.dp
private val CONNECTION_CHIP_SPACER_WIDTH = 6.dp
private val CONNECTION_CHIP_TEXT_SIZE = 12.sp

private val COIN_CARD_CORNER_SIZE = 20.dp
private val COIN_CARD_WIDTH_FRACTION = 0.9f
private val COIN_CARD_ELEVATION = 8.dp
private val COIN_CARD_PADDING = 16.dp
private val COIN_CARD_TEXT_SIZE = 16.sp
private val COIN_CARD_AMOUNT_SIZE = 18.sp
private val COIN_CARD_ICON_SIZE = 22.dp
private val COIN_CARD_ICON_SPACER = 6.dp

private val ITEM_CARD_CORNER_SIZE = 18.dp
private val ITEM_CARD_ASPECT_RATIO = 0.9f
private val ITEM_CARD_PADDING = 12.dp
private val ITEM_IMAGE_HEIGHT = 80.dp
private val ITEM_NAME_SIZE = 15.sp
private val ITEM_STATUS_TEXT_SIZE = 13.sp
private val ITEM_PARTICLE_RADIUS = 4f

private val BUY_BUTTON_CORNER_SIZE = 10.dp
private val BUY_BUTTON_PROGRESS_SIZE = 16.dp
private val BUY_BUTTON_PROGRESS_STROKE = 2.dp
private val BUY_BUTTON_ICON_SIZE = 16.dp
private val BUY_BUTTON_SPACER_WIDTH = 4.dp

// Animation Constants
private const val GLOW_ANIMATION_DURATION = 2000
private const val GLOW_ALPHA_MIN = 0.3f
private const val GLOW_ALPHA_MAX = 0.8f
private const val SCALE_NORMAL = 1f
private const val SCALE_SUCCESS = 1.1f
private const val SCALE_FAIL = 0.95f
private const val OPACITY_DISABLED = 0.5f
private const val OPACITY_FULL = 1f

// Particle Generation
private const val PARTICLE_COUNT = 20
private const val PARTICLE_MAX_OFFSET = 200f

// Alpha values
private const val CONNECTION_CHIP_ALPHA = 0.2f
private const val OWNED_TEXT_ALPHA = 0.8f
private const val COIN_TEXT_ALPHA = 0.8f
private const val BUTTON_DISABLED_ALPHA = 0.3f

// Grid Configuration
private const val GRID_COLUMN_COUNT = 2

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
            is PurchaseResult.Success ->
                "$PURCHASE_SUCCESS_PREFIX${result.itemName}$PURCHASE_SUCCESS_SUFFIX"
            is PurchaseResult.InsufficientCoins ->
                "$INSUFFICIENT_COINS_PREFIX${result.itemName}$PURCHASE_SUCCESS_SUFFIX"
            is PurchaseResult.AlreadyOwned -> "$ALREADY_OWNED_PREFIX${result.itemName}"
            is PurchaseResult.NoConnection -> NO_CONNECTION_MESSAGE
            is PurchaseResult.NetworkError -> "$NETWORK_ERROR_PREFIX${result.message}"
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
      color = ShopOfflineBanner,
      shape =
          RoundedCornerShape(
              bottomStart = OFFLINE_BANNER_CORNER_SIZE, bottomEnd = OFFLINE_BANNER_CORNER_SIZE),
      shadowElevation = OFFLINE_BANNER_ELEVATION,
      modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = OFFLINE_BANNER_PADDING_HORIZONTAL,
                    vertical = OFFLINE_BANNER_PADDING_VERTICAL),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = Icons.Outlined.CloudOff,
                  contentDescription = OFFLINE_TEXT,
                  tint = Color.White,
                  modifier = Modifier.size(OFFLINE_BANNER_ICON_SIZE))
              Spacer(modifier = Modifier.width(OFFLINE_BANNER_SPACER_WIDTH))
              Text(
                  text = OFFLINE_BANNER_TEXT,
                  color = Color.White,
                  fontWeight = FontWeight.Medium,
                  fontSize = OFFLINE_BANNER_TEXT_SIZE)
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
    modifier: Modifier = Modifier,
    enableAnimations: Boolean = true
) {
  val localColorScheme = MaterialTheme.colorScheme
  val isInspectionMode = LocalInspectionMode.current

  // Only run infinite animation when animations are enabled and not in inspection mode
  val glowAlpha by
      if (enableAnimations && !isInspectionMode) {
        rememberInfiniteTransition(label = "glow")
            .animateFloat(
                initialValue = GLOW_ALPHA_MIN,
                targetValue = GLOW_ALPHA_MAX,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(GLOW_ANIMATION_DURATION, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse),
                label = "glowAlpha")
      } else {
        // In test/preview mode, use a static value
        remember { mutableStateOf(GLOW_ALPHA_MIN) }
      }

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(
                  Brush.verticalGradient(
                      listOf(localColorScheme.background, localColorScheme.surface)))
              .padding(SHOP_CONTENT_PADDING),
      horizontalAlignment = Alignment.CenterHorizontally) {
        // Add top padding when offline banner is showing
        if (!isOnline) {
          Spacer(modifier = Modifier.height(OFFLINE_TOP_SPACER_HEIGHT))
        }

        Text(
            text = SHOP_TITLE,
            fontWeight = FontWeight.Bold,
            color = localColorScheme.primary,
            fontSize = SHOP_TITLE_SIZE)

        Spacer(modifier = Modifier.height(TITLE_SPACER_HEIGHT))

        // Connection status indicator
        ConnectionStatusChip(isOnline = isOnline)

        Spacer(modifier = Modifier.height(STATUS_CHIP_SPACER_HEIGHT))

        // Coin Balance Card
        CoinBalanceCard(userCoins = userCoins, glowAlpha = glowAlpha)

        Spacer(modifier = Modifier.height(COIN_CARD_SPACER_HEIGHT))

        // Item grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMN_COUNT),
            horizontalArrangement = Arrangement.spacedBy(GRID_ITEM_SPACING),
            verticalArrangement = Arrangement.spacedBy(GRID_ITEM_SPACING),
            contentPadding = PaddingValues(bottom = GRID_BOTTOM_PADDING)) {
              items(items) { item ->
                ShopItemCard(
                    item = item,
                    isOnline = isOnline,
                    isPurchasing = isPurchasing,
                    enableAnimations = enableAnimations,
                    onBuy = { success, fail -> onBuy(item, success, fail) })
              }
            }
      }
}

/** Small chip showing online/offline status. */
@Composable
private fun ConnectionStatusChip(isOnline: Boolean) {
  val backgroundColor = if (isOnline) ShopConnectionOnline else ShopConnectionOffline
  val icon = if (isOnline) Icons.Outlined.Wifi else Icons.Outlined.CloudOff
  val text = if (isOnline) ONLINE_TEXT else OFFLINE_TEXT
  val testTagValue = if (isOnline) "connection_status_online" else "connection_status_offline"

  Surface(
      color = backgroundColor.copy(alpha = CONNECTION_CHIP_ALPHA),
      shape = RoundedCornerShape(CONNECTION_CHIP_CORNER_SIZE),
      modifier = Modifier.testTag(testTagValue)) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = CONNECTION_CHIP_PADDING_HORIZONTAL,
                    vertical = CONNECTION_CHIP_PADDING_VERTICAL),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = icon,
                  contentDescription = text,
                  tint = backgroundColor,
                  modifier = Modifier.size(CONNECTION_CHIP_ICON_SIZE))
              Spacer(modifier = Modifier.width(CONNECTION_CHIP_SPACER_WIDTH))
              Text(
                  text = text,
                  color = backgroundColor,
                  fontSize = CONNECTION_CHIP_TEXT_SIZE,
                  fontWeight = FontWeight.Medium)
            }
      }
}

/** Card displaying user's coin balance. */
@Composable
private fun CoinBalanceCard(userCoins: Int, glowAlpha: Float) {
  Card(
      colors = CardDefaults.cardColors(containerColor = MidDarkCard),
      shape = RoundedCornerShape(COIN_CARD_CORNER_SIZE),
      modifier =
          Modifier.fillMaxWidth(COIN_CARD_WIDTH_FRACTION)
              .shadow(
                  elevation = COIN_CARD_ELEVATION,
                  spotColor = AccentViolet.copy(alpha = glowAlpha),
                  ambientColor = AccentViolet.copy(alpha = glowAlpha),
                  shape = RoundedCornerShape(COIN_CARD_CORNER_SIZE))) {
        Row(
            modifier = Modifier.padding(COIN_CARD_PADDING),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = YOUR_COINS_LABEL,
                  color = TextLight.copy(alpha = COIN_TEXT_ALPHA),
                  fontWeight = FontWeight.Medium,
                  fontSize = COIN_CARD_TEXT_SIZE)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AttachMoney,
                    contentDescription = COIN_ICON_DESCRIPTION,
                    tint = AccentViolet,
                    modifier = Modifier.size(COIN_CARD_ICON_SIZE))
                Spacer(modifier = Modifier.width(COIN_CARD_ICON_SPACER))
                Text(
                    text = "$userCoins",
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = COIN_CARD_AMOUNT_SIZE)
              }
            }
      }
}

@Composable
fun ShopItemCard(
    item: CosmeticItem,
    isOnline: Boolean,
    isPurchasing: Boolean,
    onBuy: ((() -> Unit), (() -> Unit)) -> Unit,
    enableAnimations: Boolean = true
) {
  var scale by remember { mutableFloatStateOf(SCALE_NORMAL) }
  var particles by remember { mutableStateOf(emptyList<Offset>()) }

  // Determine if purchase is allowed
  val canPurchase = isOnline && !item.owned && !isPurchasing

  // Capture colors for use in non-Composable contexts
  val localColorScheme = MaterialTheme.colorScheme
  val particleColor = localColorScheme.primary
  val textColor = localColorScheme.onSurface
  val surfaceColor = localColorScheme.surface

  // Animation callbacks - only change scale/particles if animations enabled
  val onSuccess = {
    if (enableAnimations) {
      scale = SCALE_SUCCESS
      particles = generateParticles()
    }
  }
  val onFail = {
    if (enableAnimations) {
      scale = SCALE_FAIL
    }
  }

  Card(
      colors = CardDefaults.cardColors(containerColor = surfaceColor),
      shape = RoundedCornerShape(ITEM_CARD_CORNER_SIZE),
      modifier =
          Modifier.fillMaxWidth()
              .aspectRatio(ITEM_CARD_ASPECT_RATIO)
              .graphicsLayer {
                if (scale != SCALE_NORMAL) {
                  scaleX = scale
                  scaleY = scale
                }
              }
              .alpha(if (!isOnline && !item.owned) OPACITY_DISABLED else OPACITY_FULL)
              .clickable(enabled = canPurchase) { onBuy(onSuccess, onFail) }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          // Particles canvas
          Canvas(modifier = Modifier.matchParentSize()) {
            particles.forEach { p ->
              drawCircle(
                  color = particleColor.copy(alpha = Random.nextFloat()),
                  radius = ITEM_PARTICLE_RADIUS,
                  center = p)
            }
          }

          Column(
              modifier = Modifier.fillMaxSize().padding(ITEM_CARD_PADDING),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween) {
                Image(
                    painter = painterResource(id = item.imageRes),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(ITEM_IMAGE_HEIGHT).fillMaxWidth())

                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = ITEM_NAME_SIZE,
                    color = textColor,
                    textAlign = TextAlign.Center)

                ItemStatusDisplay(
                    item = item,
                    isOnline = isOnline,
                    isPurchasing = isPurchasing,
                    canPurchase = canPurchase,
                    onBuy = { onBuy(onSuccess, onFail) })
              }
        }
      }
}

/** Displays the status/action area of a shop item (owned, offline, or buy button). */
@Composable
private fun ItemStatusDisplay(
    item: CosmeticItem,
    isOnline: Boolean,
    isPurchasing: Boolean,
    canPurchase: Boolean,
    onBuy: () -> Unit
) {
  when {
    item.owned -> {
      // Already owned indicator
      Text(
          text = OWNED_TEXT,
          color = AccentViolet.copy(alpha = OWNED_TEXT_ALPHA),
          fontWeight = FontWeight.Medium,
          fontSize = ITEM_STATUS_TEXT_SIZE)
    }
    !isOnline -> {
      // Offline indicator
      Text(
          text = OFFLINE_TEXT,
          color = Color.Gray,
          fontWeight = FontWeight.Medium,
          fontSize = ITEM_STATUS_TEXT_SIZE,
          modifier = Modifier.testTag("item_offline_indicator"))
    }
    else -> {
      // Buy button
      BuyButton(
          price = item.price,
          isPurchasing = isPurchasing,
          canPurchase = canPurchase,
          onClick = onBuy)
    }
  }
}

/** Buy button with price display or loading indicator. */
@Composable
private fun BuyButton(
    price: Int,
    isPurchasing: Boolean,
    canPurchase: Boolean,
    onClick: () -> Unit
) {
  Button(
      onClick = onClick,
      enabled = canPurchase,
      shape = RoundedCornerShape(BUY_BUTTON_CORNER_SIZE),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = AccentViolet,
              disabledContainerColor = AccentViolet.copy(alpha = BUTTON_DISABLED_ALPHA)),
      modifier = Modifier.fillMaxWidth()) {
        if (isPurchasing) {
          CircularProgressIndicator(
              modifier = Modifier.size(BUY_BUTTON_PROGRESS_SIZE),
              color = Color.White,
              strokeWidth = BUY_BUTTON_PROGRESS_STROKE)
        } else {
          Icon(
              imageVector = Icons.Outlined.AttachMoney,
              contentDescription = COIN_ICON_DESCRIPTION,
              tint = Color.White,
              modifier = Modifier.size(BUY_BUTTON_ICON_SIZE))
          Spacer(modifier = Modifier.width(BUY_BUTTON_SPACER_WIDTH))
          Text("$price", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
}

/** Generates random sparkle particles for purchase animation. */
internal fun generateParticles(): List<Offset> {
  return List(PARTICLE_COUNT) {
    Offset(
        x = Random.nextFloat() * PARTICLE_MAX_OFFSET, y = Random.nextFloat() * PARTICLE_MAX_OFFSET)
  }
}
