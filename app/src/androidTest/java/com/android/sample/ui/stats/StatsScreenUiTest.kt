package com.android.sample.ui.stats

import androidx.compose.runtime.*
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsScreenUITest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun statsScreen_smoke_renders() {
    val vm = StatsViewModel(FakeStatsRepository())
    compose.setContent { EduMonTheme { StatsScreen(vm) } }
    compose.onRoot().assertExists().assertIsDisplayed()
  }

  @Test
  fun statsScreen_recreated_twice_noCrash() {
    val vm = StatsViewModel(FakeStatsRepository())
    compose.setContent {
      var recreateTick by remember { mutableStateOf(0) }
      key(recreateTick) { EduMonTheme { StatsScreen(vm) } }
      Recreator { recreateTick++ }
    }
    compose.runOnIdle { Recreator.trigger(compose) }
    compose.runOnIdle { Recreator.trigger(compose) }
    compose.onRoot().assertExists().assertIsDisplayed()
  }

  @Test
  fun statsScreen_multipleRecompositions_doNotCrash() {
    val vm = StatsViewModel(FakeStatsRepository())
    compose.setContent {
      var counter by remember { mutableStateOf(0) }
      EduMonTheme { StatsScreen(vm) }
      SideEffect {}
      CounterHost { counter++ }
      if (counter < 0) error("noop")
    }
    repeat(3) { compose.runOnIdle { CounterHost.bump(compose) } }
    compose.onRoot().assertExists().assertIsDisplayed()
  }

  @Composable
  private fun Recreator(onRecreate: () -> Unit) {
    ComposeCallbacks.recreate = onRecreate
  }

  private object Recreator {
    fun trigger(rule: androidx.compose.ui.test.junit4.ComposeContentTestRule) {
      ComposeCallbacks.recreate?.invoke()
    }
  }

  @Composable
  private fun CounterHost(onBump: () -> Unit) {
    ComposeCallbacks.bump = onBump
  }

  private object CounterHost {
    fun bump(rule: androidx.compose.ui.test.junit4.ComposeContentTestRule) {
      ComposeCallbacks.bump?.invoke()
    }
  }

  private object ComposeCallbacks {
    var recreate: (() -> Unit)? = null
    var bump: (() -> Unit)? = null
  }
}
