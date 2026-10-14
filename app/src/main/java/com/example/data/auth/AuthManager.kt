package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.local.preferences.UserPreferencesDataStore
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val profilePhotoBase64: String = ""
)

class AuthManager(
    private val context: Context,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    private val TAG = "AuthManager"

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth initialization fallback: ${e.message}")
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore initialization fallback: ${e.message}")
            null
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<AuthUser> {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()

        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        try {
            val auth = firebaseAuth ?: return Result.failure(IllegalStateException("Firebase Auth service unavailable. Please check your internet connection."))
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val user = authResult.user ?: return Result.failure(IllegalStateException("User creation failed"))

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(trimmedName.ifEmpty { "User" })
                .build()
            user.updateProfile(profileUpdate).await()

            // Save to Firestore user doc
            try {
                firestore?.collection("users")?.document(user.uid)?.set(
                    mapOf(
                        "name" to trimmedName.ifEmpty { "User" },
                        "email" to trimmedEmail,
                        "profilePhotoBase64" to ""
                    ),
                    SetOptions.merge()
                )?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore user profile doc save error: ${e.message}")
            }

            val authUser = AuthUser(
                uid = user.uid,
                email = trimmedEmail,
                displayName = trimmedName.ifEmpty { "User" }
            )
            preferencesDataStore.setLoggedInUser(authUser.uid, authUser.displayName, authUser.email)
            return Result.success(authUser)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("The email address is already in use", ignoreCase = true) == true ->
                    "An account already exists with this email. Please login."
                e.message?.contains("badly formatted", ignoreCase = true) == true ->
                    "Invalid email address format."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection."
                e.message?.contains("password is invalid", ignoreCase = true) == true || e.message?.contains("at least 6 characters", ignoreCase = true) == true ->
                    "Password must be at least 6 characters."
                else -> e.localizedMessage ?: "Registration failed. Please check your details."
            }
            return Result.failure(Exception(message))
        }
    }

    suspend fun login(email: String, password: String): Result<AuthUser> {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your password"))
        }

        try {
            val auth = firebaseAuth ?: return Result.failure(IllegalStateException("Firebase Auth service unavailable. Please check your internet connection."))
            val authResult = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val user = authResult.user ?: return Result.failure(IllegalStateException("Login failed"))

            var photoBase64 = ""
            var resolvedName = user.displayName ?: trimmedEmail.substringBefore("@")

            // Fetch profile data from Firestore
            try {
                val doc = firestore?.collection("users")?.document(user.uid)?.get()?.await()
                if (doc != null && doc.exists()) {
                    val firestoreName = doc.getString("name")
                    if (!firestoreName.isNullOrBlank()) {
                        resolvedName = firestoreName
                    }
                    photoBase64 = doc.getString("profilePhotoBase64") ?: ""
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load firestore profile: ${e.message}")
            }

            val authUser = AuthUser(
                uid = user.uid,
                email = user.email ?: trimmedEmail,
                displayName = resolvedName,
                profilePhotoBase64 = photoBase64
            )
            // Save logged in state with profile photo
            preferencesDataStore.setLoggedInUser(authUser.uid, authUser.displayName, authUser.email, photoBase64)
            return Result.success(authUser)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("no user record", ignoreCase = true) == true ||
                e.message?.contains("user-not-found", ignoreCase = true) == true ||
                e.message?.contains("password is invalid", ignoreCase = true) == true ||
                e.message?.contains("wrong-password", ignoreCase = true) == true ||
                e.message?.contains("invalid-credential", ignoreCase = true) == true ->
                    "Incorrect email & password"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection."
                else -> "Incorrect email & password"
            }
            return Result.failure(Exception(message))
        }
    }

    suspend fun signInWithGoogle(
        idToken: String,
        displayName: String,
        email: String,
        photoUrl: String
    ): Result<AuthUser> {
        try {
            val auth = firebaseAuth ?: return Result.failure(IllegalStateException("Firebase Auth service unavailable. Please check your internet connection."))
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: return Result.failure(IllegalStateException("Failed to sign in with Google"))

            val resolvedEmail = user.email ?: email
            val resolvedName = user.displayName ?: displayName.ifEmpty { resolvedEmail.substringBefore("@") }
            val resolvedPhoto = user.photoUrl?.toString() ?: photoUrl

            var finalName = resolvedName
            var finalPhoto = resolvedPhoto

            // Fetch existing Firestore profile first so custom photo and name are preserved across data clears
            try {
                val doc = firestore?.collection("users")?.document(user.uid)?.get()?.await()
                if (doc != null && doc.exists()) {
                    val existingName = doc.getString("name")
                    if (!existingName.isNullOrBlank()) {
                        finalName = existingName
                    }
                    val existingPhoto = doc.getString("profilePhotoBase64")
                    if (!existingPhoto.isNullOrBlank()) {
                        finalPhoto = existingPhoto
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load existing firestore profile: ${e.message}")
            }

            // Store user in Firestore under /users/{user.uid} with preserved custom photo and name
            try {
                val userDoc = hashMapOf<String, Any>(
                    "uid" to user.uid,
                    "name" to finalName,
                    "email" to resolvedEmail,
                    "googlePhotoUrl" to resolvedPhoto,
                    "profilePhotoBase64" to finalPhoto,
                    "provider" to "google",
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore?.collection("users")?.document(user.uid)?.set(userDoc, SetOptions.merge())?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore user document save error: ${e.message}")
            }

            val authUser = AuthUser(
                uid = user.uid,
                email = resolvedEmail,
                displayName = finalName,
                profilePhotoBase64 = finalPhoto
            )
            preferencesDataStore.setLoggedInUser(authUser.uid, authUser.displayName, authUser.email, finalPhoto)
            return Result.success(authUser)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in error", e)
            val msg = e.localizedMessage ?: "Google Sign-In failed"
            return Result.failure(Exception(msg))
        }
    }

    suspend fun updateProfilePhoto(base64Photo: String): Result<Unit> {
        val currentUid = firebaseAuth?.currentUser?.uid?.takeIf { it.isNotBlank() }
        val uid = currentUid ?: preferencesDataStore.preferencesFlow.first().userUid.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("User not logged in"))
        try {
            firestore?.collection("users")?.document(uid)?.set(
                mapOf(
                    "profilePhotoBase64" to base64Photo,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )?.await()
            Log.d(TAG, "Profile photo updated in Firestore for user $uid")
            preferencesDataStore.setProfilePhoto(base64Photo)
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Firestore updateProfilePhoto error: ${e.message}")
            // Even if offline/transient failure, save locally
            preferencesDataStore.setProfilePhoto(base64Photo)
            return Result.success(Unit)
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        val user = firebaseAuth?.currentUser
        val currentUid = user?.uid?.takeIf { it.isNotBlank() }
        val uid = currentUid ?: preferencesDataStore.preferencesFlow.first().userUid.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("User not logged in"))
        try {
            if (user != null) {
                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmed)
                    .build()
                user.updateProfile(profileUpdate).await()
            }
            firestore?.collection("users")?.document(uid)?.set(
                mapOf(
                    "name" to trimmed,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )?.await()
            preferencesDataStore.setUserName(trimmed)
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Firestore updateDisplayName error: ${e.message}")
            preferencesDataStore.setUserName(trimmed)
            return Result.success(Unit)
        }
    }

    suspend fun updatePassword(currentPass: String, newPass: String): Result<Unit> {
        if (newPass.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters"))
        }
        val user = firebaseAuth?.currentUser ?: return Result.failure(IllegalStateException("User not logged in"))
        val email = user.email ?: return Result.failure(IllegalStateException("No email associated with user"))

        try {
            if (currentPass.isNotEmpty()) {
                val credential = EmailAuthProvider.getCredential(email, currentPass)
                user.reauthenticate(credential).await()
            }
            user.updatePassword(newPass).await()
            return Result.success(Unit)
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("wrong-password", ignoreCase = true) == true ||
                e.message?.contains("invalid-credential", ignoreCase = true) == true ||
                e.message?.contains("password is invalid", ignoreCase = true) == true ->
                    "Current password is incorrect"
                e.message?.contains("weak-password", ignoreCase = true) == true ->
                    "Password must be at least 6 characters"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection."
                else -> e.localizedMessage ?: "Failed to update password"
            }
            return Result.failure(Exception(msg))
        }
    }

    suspend fun verifyPassword(password: String): Result<Unit> {
        val user = firebaseAuth?.currentUser ?: return Result.failure(IllegalStateException("User not logged in"))
        val email = user.email ?: return Result.failure(IllegalStateException("No email associated with user"))
        try {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(Exception("Enter correct Password"))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }

        try {
            val auth = firebaseAuth ?: return Result.failure(IllegalStateException("Firebase Auth service unavailable. Please check your internet connection."))
            auth.sendPasswordResetEmail(trimmedEmail).await()
            return Result.success(Unit)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("no user record", ignoreCase = true) == true || e.message?.contains("user-not-found", ignoreCase = true) == true ->
                    "No account found with this email."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection."
                else -> e.localizedMessage ?: "Failed to send reset email."
            }
            return Result.failure(Exception(message))
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = firebaseAuth?.currentUser
        val uid = user?.uid?.takeIf { it.isNotBlank() }
            ?: preferencesDataStore.preferencesFlow.first().userUid.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("User not logged in"))

        try {
            val fs = firestore
            if (uid.isNotBlank() && fs != null) {
                try {
                    val userRef = fs.collection("users").document(uid)
                    val subcollections = listOf("expenses", "incomes", "accounts", "budgets", "savings_goals", "recurring")
                    for (sub in subcollections) {
                        val docs = userRef.collection(sub).get().await()
                        for (doc in docs.documents) {
                            doc.reference.delete().await()
                        }
                    }
                    userRef.delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore deletion warning: ${e.message}")
                }
            }

            try {
                user?.delete()?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Auth user delete warning (e.g. recent login required): ${e.message}")
            }

            try {
                firebaseAuth?.signOut()
            } catch (_: Exception) {}

            preferencesDataStore.clearLoggedInUser()
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAccount error: ${e.message}")
            try {
                firebaseAuth?.signOut()
            } catch (_: Exception) {}
            preferencesDataStore.clearLoggedInUser()
            return Result.success(Unit)
        }
    }

    suspend fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Logout error: ${e.message}")
        }
        preferencesDataStore.clearLoggedInUser()
    }
}
