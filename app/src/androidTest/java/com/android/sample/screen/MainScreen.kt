package com.android.sample.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.android.sample.resources.C
import com.android.sample.ui.widgets.WeekProgDailyObjTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class MainScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(C.Tag.main_screen_container) }) {

  val simpleText: KNode = child { hasTestTag(C.Tag.greeting) }

  // Root card of WeekProgDailyObj
  val weekRootCard: KNode = child { hasTestTag(WeekProgDailyObjTags.ROOT_CARD) }

  // Week progression
  val weekProgressToggle: KNode = child { hasTestTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE) }
  val weekProgressBar: KNode = child { hasTestTag(WeekProgDailyObjTags.WEEK_PROGRESS_BAR) }
  val weeksList: KNode = child { hasTestTag(WeekProgDailyObjTags.WEEKS_LIST) }

  // Objectives
  val objectivesSection: KNode = child { hasTestTag(WeekProgDailyObjTags.OBJECTIVES_SECTION) }
  val objectivesShowAll: KNode = child {
    hasTestTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON)
  }

  // Footer dots row
  val weekDotsRow: KNode = child { hasTestTag(WeekProgDailyObjTags.WEEK_DOTS_ROW) }
}
