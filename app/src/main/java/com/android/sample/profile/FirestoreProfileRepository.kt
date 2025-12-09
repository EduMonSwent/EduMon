package com.android.sample.profile

import android.util.Log
import com.android.sample.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class FirestoreProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ProfileRepository {

    private val _profile = MutableStateFlow(UserProfile())
    override val profile: StateFlow<UserProfile> = _profile

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private var listenerRegistration: ListenerRegistration? = null
    private var currentListeningUid: String? = null

    init {
        Log.d("FirestoreProfileRepo", "=== Repository CREATED ===")

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d("FirestoreProfileRepo", "AuthStateListener: uid=${user?.uid}, currentListening=$currentListeningUid")

            when {
                user != null && user.uid != currentListeningUid -> {
                    Log.d("FirestoreProfileRepo", "Starting listener for new user")
                    startListening(user.uid)
                }
                user == null && currentListeningUid != null -> {
                    Log.d("FirestoreProfileRepo", "User signed out")
                    stopListening()
                    _profile.value = UserProfile()
                    _isLoaded.value = true
                }
                user == null && currentListeningUid == null -> {
                    Log.d("FirestoreProfileRepo", "No user at all")
                    _isLoaded.value = true
                }
            }
        }

        val currentUser = auth.currentUser
        Log.d("FirestoreProfileRepo", "Init immediate check: currentUser=${currentUser?.uid}")
        if (currentUser != null && currentListeningUid == null) {
            startListening(currentUser.uid)
        } else if (currentUser == null) {
            _isLoaded.value = true
        }
    }

    private fun startListening(uid: String) {
        if (uid == currentListeningUid) {
            Log.d("FirestoreProfileRepo", "Already listening to $uid")
            return
        }

        stopListening()

        currentListeningUid = uid
        _isLoaded.value = false

        Log.d("FirestoreProfileRepo", "=== Starting Firestore listener for: $uid ===")

        listenerRegistration = db.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreProfileRepo", "Firestore listener error", error)
                    _isLoaded.value = true
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.toObject(UserProfile::class.java)
                    Log.d("FirestoreProfileRepo", "=== LOADED: starterId='${data?.starterId}' ===")
                    if (data != null) {
                        _profile.value = data
                    }
                } else {
                    Log.d("FirestoreProfileRepo", "=== NO DOCUMENT for $uid ===")
                    _profile.value = UserProfile()
                }
                _isLoaded.value = true
            }
    }

    private fun stopListening() {
        Log.d("FirestoreProfileRepo", "Stopping listener")
        listenerRegistration?.remove()
        listenerRegistration = null
        currentListeningUid = null
    }

    override suspend fun updateProfile(newProfile: UserProfile) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("FirestoreProfileRepo", "=== CANNOT SAVE: no user! ===")
            return
        }

        Log.d("FirestoreProfileRepo", "=== SAVING: uid=$uid, starterId='${newProfile.starterId}' ===")

        _profile.value = newProfile

        try {

            db.collection("users")
                .document(uid)
                .set(newProfile, SetOptions.merge())
                .await()
            Log.d("FirestoreProfileRepo", "=== SAVE SUCCESS ===")
        } catch (e: Exception) {
            Log.e("FirestoreProfileRepo", "=== SAVE FAILED ===", e)
            throw e
        }
    }
}