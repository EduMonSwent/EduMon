package com.android.sample.profile

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object ProfileRepositoryProvider {
  val repository: ProfileRepository by lazy {
    FirestoreProfileRepository(Firebase.firestore, Firebase.auth)
  }
}
