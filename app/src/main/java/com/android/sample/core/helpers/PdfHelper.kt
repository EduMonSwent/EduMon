package com.android.sample.core.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Helper object for opening PDF files.
 * Supports both local files and web URLs.
 */
object PdfHelper {

  /**
   * Opens a PDF file using an external app or web browser.
   *
   * @param context Android context
   * @param pdfUrl URL or file path to the PDF
   * @param fallbackMessage Message to show if no app can handle the PDF
   */
  fun openPdf(
      context: Context,
      pdfUrl: String,
      fallbackMessage: String = "No app available to open PDF"
  ) {
    if (pdfUrl.isBlank()) {
      Toast.makeText(context, "No PDF available", Toast.LENGTH_SHORT).show()
      return
    }

    try {
      val uri = Uri.parse(pdfUrl)
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        // Add FLAG_GRANT_READ_URI_PERMISSION for content:// URIs
        if (uri.scheme == "content") {
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      }

      // Check if there's an app that can handle this intent
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
      } else {
        // Fallback: try opening in browser (for http/https URLs)
        if (pdfUrl.startsWith("http://") || pdfUrl.startsWith("https://")) {
          val browserIntent = Intent(Intent.ACTION_VIEW, uri)
          context.startActivity(browserIntent)
        } else {
          Toast.makeText(context, fallbackMessage, Toast.LENGTH_LONG).show()
        }
      }
    } catch (e: Exception) {
      Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }

  /**
   * Checks if a PDF URL is valid (non-empty).

  fun isValidPdfUrl(pdfUrl: String?): Boolean {
    return !pdfUrl.isNullOrBlank()
  }
   */
}

