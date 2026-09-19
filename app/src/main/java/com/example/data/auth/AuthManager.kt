package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.local.preferences.UserPreferencesDataStore
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    suspend fun updateProfilePhoto(base64Photo: String): Result<Unit> {
        val user = firebaseAuth?.currentUser ?: return Result.failure(IllegalStateException("User not logged in"))
        try {
            firestore?.collection("users")?.document(user.uid)?.set(
                mapOf("profilePhotoBase64" to base64Photo),
                SetOptions.merge()
            )?.await()
            preferencesDataStore.setProfilePhoto(base64Photo)
            return Result.success(Unit)
        } catch (e: Exception) {
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
        val user = firebaseAuth?.currentUser ?: return Result.failure(IllegalStateException("User not logged in"))
        try {
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(trimmed)
                .build()
            user.updateProfile(profileUpdate).await()
            firestore?.collection("users")?.document(user.uid)?.set(
                mapOf("name" to trimmed),
                SetOptions.merge()
            )?.await()
            preferencesDataStore.setUserName(trimmed)
            return Result.success(Unit)
        } catch (e: Exception) {
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
            val msg = when {
                e.message?.contains("wrong-password", ignoreCase = true) == true ||
                e.message?.contains("invalid-credential", ignoreCase = true) == true ||
                e.message?.contains("password is invalid", ignoreCase = true) == true ->
                    "Incorrect account password"
                else -> e.localizedMessage ?: "Password verification failed"
            }
            return Result.failure(Exception(msg))
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

    suspend fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Logout error: ${e.message}")
        }
        preferencesDataStore.clearLoggedInUser()
    }
}
