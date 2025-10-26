package com.android.sample.ui.shop

import androidx.compose.animation.core.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.ui.theme.*
import kotlin.random.Random
import kotlinx.coroutines.launch

// The assistance of an AI tool (ChatGPT) was solicited in writing this  file.
@Composable
fun ShopScreen(viewModel: ShopViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
  val userCoins by viewModel.userCoins.collectAsState()
  val items by viewModel.items.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
      containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbarHostState) }) {
          innerPadding ->
        ShopContent(
            userCoins = userCoins,
            items = items,
            modifier = Modifier.padding(innerPadding),
            onBuy = { item, triggerSuccess, triggerFail ->
              val success = viewModel.buyItem(item)
              if (success) {
                triggerSuccess() // ✨ animation
                coroutineScope.launch {
                  snackbarHostState.showSnackbar("🎉 You purchased ${item.name}!")
                }
              } else {
                triggerFail() // ❌ shake
                coroutineScope.launch {
                  snackbarHostState.showSnackbar("❌ Not enough coins to buy ${item.name}")
                }
              }
            })
      }
}

@Composable
fun ShopContent(
    userCoins: Int,
    items: List<CosmeticItem>,
    onBuy: (CosmeticItem, () -> Unit, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
  val glowAlpha by
      rememberInfiniteTransition()
          .animateFloat(
              initialValue = 0.3f,
              targetValue = 0.8f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(2000, easing = LinearEasing),
                      repeatMode = RepeatMode.Reverse))

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(Brush.verticalGradient(listOf(BackgroundDark, Color(0xFF181830))))
              .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "EduMon Shop",
            fontWeight = FontWeight.Bold,
            color = AccentViolet,
            fontSize = 26.sp)

        Spacer(modifier = Modifier.height(8.dp))

        // --- Coin Balance ---
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

        Spacer(modifier = Modifier.height(20.dp))

        // --- Item grid ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)) {
              items(items) { item ->
                ShopItemCard(item = item, onBuy = { success, fail -> onBuy(item, success, fail) })
              }
            }
      }
}

@Composable
fun ShopItemCard(item: CosmeticItem, onBuy: ((() -> Unit), (() -> Unit)) -> Unit) {
  var scale by remember { mutableStateOf(1f) }
  var particles by remember { mutableStateOf(emptyList<Offset>()) }

  val transition = rememberInfiniteTransition()
  val shakeOffset by
      transition.animateFloat(
          initialValue = 0f,
          targetValue = 10f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(150, easing = FastOutLinearInEasing),
                  repeatMode = RepeatMode.Reverse))

  Card(
      colors = CardDefaults.cardColors(containerColor = MidDarkCard),
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
              .clickable(enabled = !item.owned) {
                onBuy(
                    {
                      // 🎉 succès → scale + particules
                      scale = 1.1f
                      particles = generateParticles()
                    },
                    {
                      // ❌ échec → petit shake rouge
                      scale = 0.95f
                    })
              }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          // 🌟 Particles
          Canvas(modifier = Modifier.matchParentSize()) {
            particles.forEach { p ->
              drawCircle(
                  color = AccentViolet.copy(alpha = Random.nextFloat()), radius = 4f, center = p)
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
                    color = TextLight,
                    textAlign = TextAlign.Center)

                if (item.owned) {
                  Text(
                      text = "Owned",
                      color = AccentViolet.copy(alpha = 0.7f),
                      fontWeight = FontWeight.Medium,
                      fontSize = 13.sp)
                } else {
                  Button(
                      onClick = {
                        onBuy(
                            {
                              scale = 1.1f
                              particles = generateParticles()
                            },
                            { scale = 0.95f })
                      },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                      modifier = Modifier.fillMaxWidth()) {
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

/** Génère des particules lumineuses aléatoires autour de la carte */
internal fun generateParticles(): List<Offset> {
  return List(20) { Offset(x = Random.nextFloat() * 200f, y = Random.nextFloat() * 200f) }
}
