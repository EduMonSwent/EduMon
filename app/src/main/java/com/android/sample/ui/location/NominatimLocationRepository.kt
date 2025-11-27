package com.android.sample.ui.location

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class NominatimLocationRepository(private val client: OkHttpClient) : LocationRepository {

  private fun parseBody(body: String): List<Location> {
    val jsonArray = JSONArray(body)
    return List(jsonArray.length()) { i ->
      val jsonObject = jsonArray.getJSONObject(i)
      val lat = jsonObject.getDouble("lat")
      val lon = jsonObject.getDouble("lon")
      val name = jsonObject.getString("display_name")
      Location(lat, lon, name)
    }
  }

  override suspend fun search(query: String): List<Location> =
      withContext(Dispatchers.IO) {
        // Build URL with proper params
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .addQueryParameter("limit", "5")
                // ⚠️ put a real email here (school email is fine)
                .addQueryParameter("email", "[email protected]")
                .build()

        val request =
            Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    // ⚠️ CHANGE THIS to something that *really* identifies your app + a contact
                    "EduMon/1.0 (contact: [email protected])")
                // Referer is optional but fine to keep / adjust
                .header("Referer", "https://edumon.example.com")
                .build()

        try {
          val response = client.newCall(request).execute()
          response.use {
            if (!response.isSuccessful) {
              Log.d("NominatimLocationRepo", "HTTP ${response.code} for '$query'")
              // While debugging, don’t hide this – return empty but log clearly
              return@withContext emptyList()
            }

            val body = response.body?.string()
            if (body != null) {
              Log.d("NominatimLocationRepo", "OK for '$query'")
              return@withContext parseBody(body)
            } else {
              Log.d("NominatimLocationRepo", "Empty body for '$query'")
              return@withContext emptyList()
            }
          }
        } catch (e: IOException) {
          Log.e("NominatimLocationRepo", "Failed request for '$query'", e)
          return@withContext emptyList()
        }
      }
}
