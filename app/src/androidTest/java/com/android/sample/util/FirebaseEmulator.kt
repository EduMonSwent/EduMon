package com.android.sample.util

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Manages connection to Firebase Emulators for Android tests. Robust to different UI ports, avoids
 * main-thread network, and exposes helpers for setup/cleanup.
 */
object FirebaseEmulator {
  val auth
    get() = Firebase.auth

  val firestore
    get() = Firebase.firestore

  const val HOST = "10.0.2.2"
  // We’ll probe both, 4000 is the canonical UI port, 4400 was used historically in some setups.
  private val UI_PORTS = listOf(4000, 4400)
  const val FIRESTORE_PORT = 8080
  const val AUTH_PORT = 9099

  // Keep this lazy (and null-safe) — if default app isn’t ready yet, initIfNeeded() takes care of
  // it.
  val projectID: String by lazy {
    FirebaseApp.getInstance().options.projectId
        ?: error(
            "Firebase projectId is null. Is google-services.json present and FirebaseApp initialized?")
  }

  private val httpClient = OkHttpClient()

  private val firestoreEndpoint by lazy {
    "http://$HOST:$FIRESTORE_PORT/emulator/v1/projects/$projectID/databases/(default)/documents"
  }

  private val authEndpoint by lazy {
    "http://$HOST:$AUTH_PORT/emulator/v1/projects/$projectID/accounts"
  }

  // Probe UI (4000 or 4400). If UI is up, emulators are very likely up.
  private fun areEmulatorsRunning(): Boolean {
    return UI_PORTS.any { port ->
      runCatching {
            val req = Request.Builder().url("http://$HOST:$port/emulators").build()
            httpClient.newCall(req).execute().use { it.isSuccessful }
          }
          .getOrNull() == true
    }
  }

  private fun probe(url: String): Boolean =
      runCatching {
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
              // Any response means something is listening (200..599)
              resp.code in 200..599
            }
          }
          .getOrDefault(false)

  val isRunning: Boolean
    get() = probe("http://$HOST:$FIRESTORE_PORT") && probe("http://$HOST:$AUTH_PORT")

  // Idempotent connector flag (avoid re-calling useEmulator a bunch of times)
  @Volatile private var connected = false

  /**
   * Ensure FirebaseApp exists (use when running instrumentation in projects without automatic
   * init).
   */
  fun initIfNeeded(context: Context) {
    runCatching { FirebaseApp.getInstance() }
        .getOrElse {
          FirebaseApp.initializeApp(context)
              ?: error("Failed to initialize FirebaseApp. Is google-services.json included?")
        }
  }

  /** Connects Auth/Firestore to emulators if they’re running. Safe to call multiple times. */
  fun connectIfRunning() {
    if (!isRunning) return
    if (connected) return

    synchronized(this) {
      if (connected) return
      auth.useEmulator(HOST, AUTH_PORT)
      firestore.useEmulator(HOST, FIRESTORE_PORT)

      // Sanity check: ensure we’re actually pointed at the emulator host.
      val host = firestore.firestoreSettings.host
      require(host.contains(HOST)) { "Failed to connect to Firestore Emulator; host=$host" }

      connected = true
    }
  }

  // ---- Clearing helpers (run on IO to avoid NetworkOnMainThreadException) ----

  private suspend fun clearEmulator(endpoint: String) {
    withContext(Dispatchers.IO) {
      val req = Request.Builder().url(endpoint).delete().build()
      httpClient.newCall(req).execute().use { resp ->
        require(resp.isSuccessful) {
          "Failed to clear emulator at $endpoint: ${resp.code} ${resp.message}"
        }
      }
    }
  }

  suspend fun clearAuthEmulator() = clearEmulator(authEndpoint)

  suspend fun clearFirestoreEmulator() = clearEmulator(firestoreEndpoint)

  suspend fun clearAll() {
    // Order doesn’t matter, they’re independent; do them sequentially for clarity
    clearFirestoreEmulator()
    clearAuthEmulator()
  }

  /** Seeds a Google user in the Auth Emulator using a fake JWT id_token. */
  suspend fun createGoogleUser(fakeIdToken: String) =
      withContext(Dispatchers.IO) {
        val url =
            "http://$HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=fake-api-key"
        val postBody = "id_token=$fakeIdToken&providerId=google.com"

        val requestJson =
            JSONObject().apply {
              put("postBody", postBody)
              put("requestUri", "http://localhost")
              put("returnIdpCredential", true)
              put("returnSecureToken", true)
            }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val req =
            Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

        httpClient.newCall(req).execute().use { resp ->
          require(resp.isSuccessful) {
            "Failed to create user in Auth Emulator: ${resp.code} ${resp.message}"
          }
        }
      }

  suspend fun changeEmail(fakeIdToken: String, newEmail: String) =
      withContext(Dispatchers.IO) {
        val url =
            "http://$HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:update?key=fake-api-key"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val payload =
            """
      {
        "idToken": "$fakeIdToken",
        "email": "$newEmail",
        "returnSecureToken": true
      }
    """
                .trimIndent()
                .toRequestBody(mediaType)

        val req =
            Request.Builder()
                .url(url)
                .post(payload)
                .addHeader("Content-Type", "application/json")
                .build()

        httpClient.newCall(req).execute().use { resp ->
          require(resp.isSuccessful) {
            "Failed to change email in Auth Emulator: ${resp.code} ${resp.message}"
          }
        }
      }

  val users: String
    get() =
        runCatching {
              val req =
                  Request.Builder()
                      .url(
                          "http://$HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:query?key=fake-api-key")
                      .build()
              httpClient.newCall(req).execute().use { resp -> resp.body?.string().orEmpty() }
            }
            .getOrElse { e ->
              Log.w("FirebaseEmulator", "Failed to fetch emulator users", e)
              ""
            }
}
