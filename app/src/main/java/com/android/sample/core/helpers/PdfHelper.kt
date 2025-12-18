package com.android.sample.core.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/** Helper object for opening PDF files. Supports both local files and web URLs. */
object PdfHelper {
  // User-facing string constants
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
      val uri: Uri =
          if (pdfUrl.startsWith("file:///android_asset/")) {
            val assetPath = pdfUrl.removePrefix("file:///android_asset/")
            val fileName = assetPath.substringAfterLast("/")
            val cacheFile = File(context.cacheDir, fileName)

            if (!cacheFile.exists()) {
              context.assets.open(assetPath).use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
              }
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
          } else {
            Uri.parse(pdfUrl)
          }

      val intent =
          Intent(Intent.ACTION_VIEW).apply {
            setData(uri) // ✅ DO NOT set MIME type
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }

      context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
      Toast.makeText(context, fallbackMessage, Toast.LENGTH_LONG).show()
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
