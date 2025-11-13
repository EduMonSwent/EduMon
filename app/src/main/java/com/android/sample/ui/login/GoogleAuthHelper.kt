package com.android.sample.ui.login

import android.os.Bundle
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider


object GoogleAuthHelper {

  fun fromBundle(bundle: Bundle): GoogleIdTokenCredential =
      GoogleIdTokenCredential.createFrom(bundle)

  fun toFirebaseCredential(idToken: String): AuthCredential =
      GoogleAuthProvider.getCredential(idToken, null)
}
