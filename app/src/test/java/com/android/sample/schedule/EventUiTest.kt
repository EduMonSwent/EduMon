package com.android.sample.schedule

import com.android.sample.R
import com.android.sample.model.schedule.EventKind
import com.android.sample.ui.schedule.EventUi
import org.junit.Assert.assertEquals
import org.junit.Test

class EventUiTest {

  @Test
  fun styleFor_knownKinds_haveExpectedIcons() {
    // Spot-check a few mappings
    assertEquals(R.drawable.ic_event, EventUi.styleFor(EventKind.CLASS_LECTURE).icon)
    assertEquals(R.drawable.ic_exercise, EventUi.styleFor(EventKind.STUDY).icon)
    assertEquals(R.drawable.ic_yoga, EventUi.styleFor(EventKind.PROJECT).icon)
    assertEquals(R.drawable.ic_star, EventUi.styleFor(EventKind.ACTIVITY_ASSOCIATION).icon)
  }
}
