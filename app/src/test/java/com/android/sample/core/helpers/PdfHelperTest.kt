// filepath:
// c:\Users\Khalil\EduMon\app\src\test\java\com\android\sample\core\helpers\PdfHelperTest.kt
package com.android.sample.core.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowPackageManager
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
  private lateinit var packageManager: ShadowPackageManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    packageManager = shadowOf(context.packageManager)
    ShadowToast.reset()

    // Make Robolectric throw when starting an activity that has no handler.
    // This is what drives the "Error opening PDF: ..." tests.
    ShadowApplication.getInstance().checkActivities(true)
  }

  // ======================== Blank/Empty URL Tests ========================

  @Test
  fun openPdf_withBlankUrl_showsNoPdfAvailableToast() {
    PdfHelper.openPdf(context, "")

    val toast = ShadowToast.getTextOfLatestToast()
    assertEquals("No PDF available", toast)
  }

  @Test
  fun openPdf_withWhitespaceUrl_showsNoPdfAvailableToast() {
    PdfHelper.openPdf(context, "   ")

    val toast = ShadowToast.getTextOfLatestToast()
    assertEquals("No PDF available", toast)
  }

  @Test
  fun openPdf_withOnlySpaces_showsNoPdfAvailableToast() {
    PdfHelper.openPdf(context, "     ")

    val toast = ShadowToast.getTextOfLatestToast()
    assertEquals("No PDF available", toast)
  }

  @Test
  fun openPdf_withTabsAndNewlines_showsNoPdfAvailableToast() {
    PdfHelper.openPdf(context, "\t\n  \n\t")

    val toast = ShadowToast.getTextOfLatestToast()
    assertEquals("No PDF available", toast)
  }

  // ======================== With PDF Viewer Available ========================

  @Test
  fun openPdf_withHttpsUrl_whenPdfViewerAvailable_doesNotShowToast() {
    val pdfUrl = "https://example.com/document.pdf"
    addPdfViewerForUrl(pdfUrl)

    PdfHelper.openPdf(context, pdfUrl)

    val toast = ShadowToast.getTextOfLatestToast()
    assertNull("Should not show toast when PDF viewer is available", toast)
  }

  @Test
  fun openPdf_withHttpUrl_whenPdfViewerAvailable_doesNotShowToast() {
    val pdfUrl = "http://example.com/document.pdf"
    addPdfViewerForUrl(pdfUrl)

    PdfHelper.openPdf(context, pdfUrl)

    val toast = ShadowToast.getTextOfLatestToast()
    assertNull("Should not show toast when PDF viewer is available", toast)
  }

  @Test
  fun openPdf_withContentUri_whenPdfViewerAvailable_doesNotShowToast() {
    val contentUri = "content://com.example.provider/pdfs/document.pdf"
    addPdfViewerForUrl(contentUri)

    PdfHelper.openPdf(context, contentUri)

    val toast = ShadowToast.getTextOfLatestToast()
    assertNull("Should not show toast when PDF viewer is available", toast)
  }

  @Test
  fun openPdf_withFileUri_whenPdfViewerAvailable_doesNotShowToast() {
    val fileUri = "file:///storage/emulated/0/document.pdf"
    addPdfViewerForUrl(fileUri)

    PdfHelper.openPdf(context, fileUri)

    val toast = ShadowToast.getTextOfLatestToast()
    assertNull("Should not show toast when PDF viewer is available", toast)
  }

  // ======================== Without PDF Viewer - Fallback Scenarios ========================

  @Test
  fun openPdf_withFileUrl_whenNoPdfViewer_showsErrorToast() {
    val pdfUrl = "file:///storage/document.pdf"

    PdfHelper.openPdf(context, pdfUrl)

    // File URLs without a viewer show the fallback message
    val toast = ShadowToast.getTextOfLatestToast()
    assertEquals("No app available to open PDF", toast)
  }

  @Test
  fun openPdf_withContentUri_whenNoPdfViewer_showsErrorToast() {
    val contentUri = "content://com.example.provider/pdfs/document.pdf"

    PdfHelper.openPdf(context, contentUri)

    // Content URIs without a viewer show the fallback message
    val toast = ShadowToast.getTextOfLatestToast()
    assertEquals("No app available to open PDF", toast)
  }

  @Test
  fun openPdf_withCustomFallbackMessage_showsCustomMessage() {
    val pdfUrl = "file:///storage/document.pdf"
    val customMessage = "Please install a PDF reader"

    PdfHelper.openPdf(context, pdfUrl, customMessage)

    val toast = ShadowToast.getTextOfLatestToast()
    assertEquals(customMessage, toast)
  }

  // Note: HTTP/HTTPS URLs without a viewer attempt browser fallback.
  // With ShadowApplication.checkActivities(true), startActivity() throws
  // when no browser exists, and we end up in the catch block.

  @Test
  fun openPdf_withHttpsUrl_whenNoPdfViewerOrBrowser_showsErrorToast() {
    val pdfUrl = "https://example.com/document.pdf"

    PdfHelper.openPdf(context, pdfUrl)

    val toast = ShadowToast.getTextOfLatestToast()
    assertNotNull(toast)
    assertTrue(toast!!.startsWith("Error opening PDF:"))
  }

  @Test
  fun openPdf_withHttpUrl_whenNoPdfViewerOrBrowser_showsErrorToast() {
    val pdfUrl = "http://example.com/document.pdf"

    PdfHelper.openPdf(context, pdfUrl)

    val toast = ShadowToast.getTextOfLatestToast()
    assertNotNull(toast)
    assertTrue(toast!!.startsWith("Error opening PDF:"))
  }

  // ======================== Real-World URL Tests (with viewer) ========================

  @Test
  fun openPdf_withFirebaseStorageUrl_whenPdfViewerAvailable_doesNotShowToast() {
    val firebaseUrl =
        "https://firebasestorage.googleapis.com/v0/b/app.appspot.com/o/docs%2Fpdf.pdf?alt=media&token=abc123"
    addPdfViewerForUrl(firebaseUrl)

    PdfHelper.openPdf(context, firebaseUrl)

    val toast = ShadowToast.getTextOfLatestToast()
    assertNull("Should not show toast for Firebase URL with PDF viewer", toast)
  }

  @Test
  fun openPdf_withGoogleDriveUrl_whenPdfViewerAvailable_doesNotShowToast() {
    val driveUrl = "https://drive.google.com/uc?export=download&id=FILE_ID"
    addPdfViewerForUrl(driveUrl)

    PdfHelper.openPdf(context, driveUrl)

    val toast = ShadowToast.getTextOfLatestToast()
    assertNull("Should not show toast for Drive URL with PDF viewer", toast)
  }

  @Test
  fun openPdf_withDropboxUrl_whenPdfViewerAvailable_doesNotShowToast() {
    val dropboxUrl = "https://www.dropbox.com/s/abc123/document.pdf?dl=1"
    addPdfViewerForUrl(dropboxUrl)

    PdfHelper.openPdf(context, dropboxUrl)

    val toast = ShadowToast.getTextOfLatestToast()
    assertNull("Should not show toast for Dropbox URL with PDF viewer", toast)
  }

  // ======================== URI Parsing Tests ========================

  @Test
  fun uriParsing_withHttpsUrl_parsesCorrectly() {
    val pdfUrl = "https://example.com/document.pdf"

    val uri = Uri.parse(pdfUrl)
    assertEquals("https", uri.scheme)
    assertEquals("example.com", uri.host)
  }

  @Test
  fun uriParsing_withContentUri_parsesCorrectly() {
    val contentUri = "content://com.example.provider/pdfs/document.pdf"

    val uri = Uri.parse(contentUri)
    assertEquals("content", uri.scheme)
  }

  @Test
  fun uriParsing_withFileUri_parsesCorrectly() {
    val fileUri = "file:///storage/emulated/0/document.pdf"

    val uri = Uri.parse(fileUri)
    assertEquals("file", uri.scheme)
  }

  // ======================== Helper Methods ========================

  /**
   * Adds a mock PDF viewer app to the package manager for this specific URL, so intents created by
   * PdfHelper.resolveActivity(...) will succeed.
   */
  private fun addPdfViewerForUrl(pdfUrl: String) {
    val uri = Uri.parse(pdfUrl)

    val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf") }

    val resolveInfo =
        ResolveInfo().apply {
          activityInfo =
              ActivityInfo().apply {
                packageName = "com.example.pdfviewer"
                name = "PdfViewerActivity"
              }
        }

    @Suppress("DEPRECATION") packageManager.addResolveInfoForIntent(intent, resolveInfo)
  }
}
