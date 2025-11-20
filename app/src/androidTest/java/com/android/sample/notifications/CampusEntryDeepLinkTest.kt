package com.android.sample.notifications

import android.content.Context
import android.net.Uri
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

  // Campus notifications no longer attach a deep link intent; this test was repurposed
  // to validate the generic deep link format still used for other study session notifications.
  @Test
  fun genericDeepLink_hasCorrectSchemeHostAndPath() {
    val eventId = "demo"
    val deepLink = context.getString(R.string.deep_link_format, eventId)
    val uri = Uri.parse(deepLink)
    assertEquals("edumon", uri.scheme)
    assertEquals("study_session", uri.host)
    assertEquals("/$eventId", uri.path)
  }

  @Test
  fun deepLink_formatsCorrectlyForVariousEventIds() {
    val testIds = listOf("demo", "task123", "event-456")
    testIds.forEach { id ->
      val deepLink = context.getString(R.string.deep_link_format, id)
      val uri = Uri.parse(deepLink)
      assertEquals("edumon", uri.scheme)
      assertEquals("study_session", uri.host)
      assertTrue(uri.path?.endsWith(id) == true)
    }
  }

  @Test
  fun campusEntryStrings_areNotEmpty() {
    val title = context.getString(R.string.campus_entry_title)
    val text = context.getString(R.string.campus_entry_text)
    val toggleTitle = context.getString(R.string.campus_entry_toggle_title)
    val toggleSubtitle = context.getString(R.string.campus_entry_toggle_subtitle)
    listOf(title, text, toggleTitle, toggleSubtitle).forEach { s ->
      assertTrue("String should not be empty", s.isNotEmpty())
    }
  }
}
