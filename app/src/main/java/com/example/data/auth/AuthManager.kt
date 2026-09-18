package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.local.preferences.UserPreferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String
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

            val authUser = AuthUser(
                uid = user.uid,
                email = trimmedEmail,
                displayName = trimmedName.ifEmpty { "User" }
            )
            // Immediately save as logged in user
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

            val authUser = AuthUser(
                uid = user.uid,
                email = user.email ?: trimmedEmail,
                displayName = user.displayName ?: trimmedEmail.substringBefore("@")
            )
            // Save logged in state
            preferencesDataStore.setLoggedInUser(authUser.uid, authUser.displayName, authUser.email)
            return Result.success(authUser)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("no user record", ignoreCase = true) == true || e.message?.contains("user-not-found", ignoreCase = true) == true ->
                    "No account found with this email. Please create an account first."
                e.message?.contains("password is invalid", ignoreCase = true) == true || e.message?.contains("wrong-password", ignoreCase = true) == true || e.message?.contains("invalid-credential", ignoreCase = true) == true ->
                    "Incorrect email or password. Passwords must match your registered account."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection."
                else -> e.localizedMessage ?: "Login failed. Please check your credentials."
            }
            return Result.failure(Exception(message))
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
