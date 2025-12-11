package com.android.sample.ui.onBoarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import kotlinx.coroutines.launch

// This code has been written partially using A.I (LLM).
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StarterSelectionScreen(onStarterSelected: (String) -> Unit) {
  val starters =
      listOf(
          StarterItem(id = "pyromon", image = R.drawable.edumon, background = R.drawable.bg_pyrmon),
          StarterItem(
              id = "aquamon", image = R.drawable.edumon2, background = R.drawable.bg_aquamon),
          StarterItem(
              id = "floramon", image = R.drawable.bg_aquamon, background = R.drawable.bg_floramon))

  val pagerState = rememberPagerState(pageCount = { starters.size })
  val coroutine = rememberCoroutineScope()

  Box(modifier = Modifier.fillMaxSize()) {

    // 🟢 FULLSCREEN BACKGROUND FIX
    Image(
        painter = painterResource(starters[pagerState.currentPage].background),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize())

    // Floating animation
    val floatTransition = rememberInfiniteTransition(label = "")
    val floatAnim by
        floatTransition.animateFloat(
            initialValue = -12f,
            targetValue = 12f,
            animationSpec =
                infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Reverse),
            label = "")

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically) { page ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(starters[page].image),
                contentDescription = null,
                modifier = Modifier.graphicsLayer { translationY = floatAnim }.size(260.dp))
          }
        }

    // Left arrow
    AnimatedArrow(
        visible = pagerState.currentPage > 0, alignment = Alignment.CenterStart, isLeft = true) {
          coroutine.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        }

    // Right arrow
    AnimatedArrow(
        visible = pagerState.currentPage < starters.lastIndex,
        alignment = Alignment.CenterEnd,
        isLeft = false) {
          coroutine.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        }

    Button(
        onClick = { onStarterSelected(starters[pagerState.currentPage].id) },
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.onboarding_button_background)),
        shape = RoundedCornerShape(50.dp)) {
          Text(
              text = stringResource(R.string.onboarding_confirm_starter),
              fontSize = 18.sp,
              color = colorResource(R.color.onboarding_button_text))
        }
  }
}

@Composable
private fun AnimatedArrow(
    visible: Boolean,
    alignment: Alignment,
    isLeft: Boolean,
    onClick: () -> Unit
) {
  AnimatedVisibility(visible = visible, modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
      val alphaAnim by
          rememberInfiniteTransition(label = "")
              .animateFloat(
                  initialValue = 0.3f,
                  targetValue = 1f,
                  animationSpec =
                      infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
                  label = "")

      Image(
          imageVector = if (isLeft) ArrowLeftCute else ArrowRightCute,
          contentDescription = null,
          modifier = Modifier.size(48.dp).alpha(alphaAnim).clickable { onClick() })
    }
  }
}

data class StarterItem(val id: String, val image: Int, val background: Int)

// Cute arrows
val ArrowLeftCute: ImageVector =
    Builder(
            name = "arrow_left_cute",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f)
        .apply {
          path(
              fill = SolidColor(Color(0x4DB3E5FC)),
              stroke = null,
              pathFillType = PathFillType.NonZero) {
                moveTo(30f, 6f)
                lineTo(18f, 18f)
                lineTo(30f, 30f)
                close()
              }

          path(
              fill = SolidColor(Color.Transparent),
              stroke = SolidColor(Color(0xFF79B8FF)),
              strokeLineWidth = 4f,
              strokeLineCap = StrokeCap.Round,
              strokeLineJoin = StrokeJoin.Round) {
                moveTo(30f, 6f)
                lineTo(18f, 18f)
                lineTo(30f, 30f)
              }
        }
        .build()

val ArrowRightCute: ImageVector =
    Builder(
            name = "arrow_right_cute",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f)
        .apply {
          path(
              fill = SolidColor(Color(0x4DB3E5FC)),
              stroke = null,
              pathFillType = PathFillType.NonZero) {
                moveTo(18f, 6f)
                lineTo(30f, 18f)
                lineTo(18f, 30f)
                close()
              }

          path(
              fill = SolidColor(Color.Transparent),
              stroke = SolidColor(Color(0xFF79B8FF)),
              strokeLineWidth = 4f,
              strokeLineCap = StrokeCap.Round,
              strokeLineJoin = StrokeJoin.Round) {
                moveTo(18f, 6f)
                lineTo(30f, 18f)
                lineTo(18f, 30f)
              }
        }
        .build()
