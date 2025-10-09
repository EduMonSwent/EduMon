package com.android.sample.ui.login

import android.os.Bundle
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Petit utilitaire isolant les appels à la librairie Google/Firebase.
 * Sert d’abstraction pour les tests ou futures évolutions.
 */
object GoogleAuthHelper {

    /** Convertit le bundle Credential Manager en objet GoogleIdTokenCredential. */
    fun fromBundle(bundle: Bundle): GoogleIdTokenCredential =
        GoogleIdTokenCredential.createFrom(bundle)

    /** Transforme le token d'identité en credential Firebase. */
    fun toFirebaseCredential(idToken: String): AuthCredential =
        GoogleAuthProvider.getCredential(idToken, null)
}
