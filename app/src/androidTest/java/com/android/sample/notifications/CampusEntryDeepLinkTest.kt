package com.android.sample.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampusEntryDeepLinkTest {

  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun deepLink_hasCorrectSchemeAndHost() {
    // Given
    val eventId = "campus"
    val deepLink = context.getString(R.string.deep_link_format, eventId)

    // When
    val uri = Uri.parse(deepLink)

    // Then
    assertEquals("edumon", uri.scheme)
    assertEquals("study_session", uri.host)
    assertEquals("/campus", uri.path)
  }

  @Test
  fun deepLink_canBeResolvedByMainActivity() {
    // Given
    val deepLink = context.getString(R.string.deep_link_format, "campus")
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply { `package` = context.packageName }

    // When
    val resolveInfo = context.packageManager.resolveActivity(intent, 0)

    // Then
    assertNotNull("Deep link should resolve to an activity", resolveInfo)
  }

  @Test
  fun pendingIntent_createdForCampusNotification() {
    // Given
    val deepLink = context.getString(R.string.deep_link_format, "campus")
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply { `package` = context.packageName }

    // When
    val pendingIntent =
        PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    // Then
    assertNotNull(pendingIntent)
  }

  @Test
  fun deepLink_formatsCorrectlyForDifferentEventIds() {
    // Test with various event IDs
    val testIds = listOf("campus", "task123", "demo", "event-456")

    testIds.forEach { eventId ->
      val deepLink = context.getString(R.string.deep_link_format, eventId)
      val uri = Uri.parse(deepLink)

      assertEquals("edumon", uri.scheme)
      assertEquals("study_session", uri.host)
      assertTrue(uri.path?.contains(eventId) == true)
    }
  }

  @Test
  fun campusEntryStrings_areNotEmpty() {
    // Verify all campus entry strings exist and are not empty
    val title = context.getString(R.string.campus_entry_title)
    val text = context.getString(R.string.campus_entry_text)
    val toggleTitle = context.getString(R.string.campus_entry_toggle_title)
    val toggleSubtitle = context.getString(R.string.campus_entry_toggle_subtitle)

    assertTrue("Title should not be empty", title.isNotEmpty())
    assertTrue("Text should not be empty", text.isNotEmpty())
    assertTrue("Toggle title should not be empty", toggleTitle.isNotEmpty())
    assertTrue("Toggle subtitle should not be empty", toggleSubtitle.isNotEmpty())
  }
}
