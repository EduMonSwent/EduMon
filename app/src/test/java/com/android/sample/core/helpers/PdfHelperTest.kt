// filepath:
// c:\Users\Khalil\EduMon\app\src\test\java\com\android\sample\core\helpers\PdfHelperTest.kt
package com.android.sample.core.helpers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowToast

/**
 * Unit tests for PdfHelper.
 *
 * Tests the PdfHelper's ability to:
 * - Handle blank/empty URLs
 * - Open PDFs when a viewer is available
 * - Show appropriate error messages when no viewer exists
 * - Handle different URI schemes (http, https, file, content)
 */
@RunWith(RobolectricTestRunner::class)
class PdfHelperTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    ShadowToast.reset()
  }

  // ======================== Blank URL ========================

  @Test
  fun openPdf_blankUrl_showsNoPdfToast() {
    PdfHelper.openPdf(context, "")

    assertEquals("No PDF available", ShadowToast.getTextOfLatestToast())
  }

  // ======================== Valid URL – no app ========================

  @Test
  fun openPdf_fileUrl_noActivity_showsFallbackToast() {
    ShadowApplication.getInstance().checkActivities(true)

    PdfHelper.openPdf(context, "file:///storage/document.pdf")

    assertEquals("No app available to open PDF", ShadowToast.getTextOfLatestToast())
  }

  @Test
  fun openPdf_contentUrl_noActivity_showsFallbackToast() {
    ShadowApplication.getInstance().checkActivities(true)

    PdfHelper.openPdf(context, "content://com.example/doc.pdf")

    assertEquals("No app available to open PDF", ShadowToast.getTextOfLatestToast())
  }

  // ======================== Activity exists ========================

  @Test
  fun openPdf_httpsUrl_activityExists_noToast() {
    ShadowApplication.getInstance().checkActivities(false)

    PdfHelper.openPdf(context, "https://example.com/test.pdf")

    assertNull(ShadowToast.getTextOfLatestToast())
  }

  // ======================== Asset URL ========================

  @Test
  fun openPdf_assetUrl_doesNotCrash() {
    ShadowApplication.getInstance().checkActivities(true)

    PdfHelper.openPdf(context, "file:///android_asset/sample.pdf")

    // Either fallback or error toast is acceptable — test is that it doesn't crash
    assertNotNull(ShadowToast.getTextOfLatestToast())
  }
}
