// app/src/main/java/com/android/sample/pet/PetRepositoryProvider.kt
// Parts of this file were generated with the help of an AI language model.

package com.android.sample.pet

import com.android.sample.pet.data.PetRepository
import com.android.sample.pet.data.FirestorePetRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object PetRepositoryProvider {
    val repository: PetRepository by lazy { FirestorePetRepository(Firebase.firestore) }
}

