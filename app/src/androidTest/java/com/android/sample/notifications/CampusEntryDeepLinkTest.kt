package com.android.sample.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Parts of this code were written with ChatGPT assistance

@RunWith(AndroidJUnit4::class)
class CampusEntryDeepLinkTest {
  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun campusEntryStrings_areNotEmpty() {
    val title = context.getString(R.string.campus_entry_title)
    val toggleTitle = context.getString(R.string.campus_entry_toggle_title)
    val toggleSubtitle = context.getString(R.string.campus_entry_toggle_subtitle)
    listOf(title, toggleTitle, toggleSubtitle).forEach { s ->
      assertTrue("String should not be empty", s.isNotEmpty())
    }
  }
}
