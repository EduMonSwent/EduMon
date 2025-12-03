package com.android.sample.core.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Helper object for opening PDF files. Supports both local files and web URLs. */
object PdfHelper {

  // Constants for strings
  private const val MIME_TYPE_PDF = "application/pdf"
  private const val URI_SCHEME_CONTENT = "content"
  private const val URI_SCHEME_HTTP = "http://"
  private const val URI_SCHEME_HTTPS = "https://"
  private const val DEFAULT_FALLBACK_MESSAGE = "No app available to open PDF"
  private const val NO_PDF_AVAILABLE_MESSAGE = "No PDF available"
  private const val ERROR_OPENING_PDF_PREFIX = "Error opening PDF: "

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
      fallbackMessage: String = DEFAULT_FALLBACK_MESSAGE
  ) {
    if (pdfUrl.isBlank()) {
      Toast.makeText(context, NO_PDF_AVAILABLE_MESSAGE, Toast.LENGTH_SHORT).show()
      return
    }

    try {
      val uri = Uri.parse(pdfUrl)
      val intent =
          Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MIME_TYPE_PDF)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Add FLAG_GRANT_READ_URI_PERMISSION for content:// URIs
            if (uri.scheme == URI_SCHEME_CONTENT) {
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
          }

      // Check if there's an app that can handle this intent
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
      } else {
        // Fallback: try opening in browser (for http/https URLs)
        if (pdfUrl.startsWith(URI_SCHEME_HTTP) || pdfUrl.startsWith(URI_SCHEME_HTTPS)) {
          val browserIntent = Intent(Intent.ACTION_VIEW, uri)
          context.startActivity(browserIntent)
        } else {
          Toast.makeText(context, fallbackMessage, Toast.LENGTH_LONG).show()
        }
      }
    } catch (e: Exception) {
      Toast.makeText(context, ERROR_OPENING_PDF_PREFIX + e.message, Toast.LENGTH_LONG).show()
    }
  }

  /**
   * Checks if a PDF URL is valid (non-empty).
   *
   * fun isValidPdfUrl(pdfUrl: String?): Boolean { return !pdfUrl.isNullOrBlank() }
   */
}
