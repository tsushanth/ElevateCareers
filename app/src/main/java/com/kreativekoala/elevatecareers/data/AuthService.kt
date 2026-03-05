package com.kreativekoala.elevatecareers.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Auth Service using Firebase Auth - EXACTLY LIKE iOS
 *
 * iOS uses Firebase Auth for Google & Email
 * Android does the same!
 *
 * NO backend calls for authentication!
 */
class AuthService private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: AuthService? = null

        fun getInstance(context: Context): AuthService {
            return instance ?: synchronized(this) {
                instance ?: AuthService(context.applicationContext).also { instance = it }
            }
        }
    }

    private val firebaseAuth = FirebaseAuth.getInstance()

    // MARK: - Current User

    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    fun isAuthenticated(): Boolean = firebaseAuth.currentUser != null

    fun getUserEmail(): String? = firebaseAuth.currentUser?.email

    fun getUserDisplayName(): String? = firebaseAuth.currentUser?.displayName

    fun getUserId(): String? = firebaseAuth.currentUser?.uid

    fun getUserPhotoUrl(): String? = firebaseAuth.currentUser?.photoUrl?.toString()

    // MARK: - Google Sign In (Firebase Auth - Same as iOS)

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                // Sign in to Firebase with Google credential
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val user = authResult.user ?: throw Exception("No user returned from Firebase")
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // MARK: - Email/Password Sign In (Firebase Auth - Same as iOS)

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw Exception("No user returned from Firebase")
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // MARK: - Email/Password Sign Up (Firebase Auth - Same as iOS)

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw Exception("No user returned from Firebase")
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // MARK: - LinkedIn Sign In (Creates/Signs into Firebase)

    /**
     * Create or sign in a LinkedIn user to Firebase
     * This unifies LinkedIn auth with Firebase auth
     */
    suspend fun createOrSignInLinkedInUser(email: String, linkedInId: String, displayName: String?): Result<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                // Use LinkedIn ID as part of password for security
                val password = "linkedin_$linkedInId"

                // Try to sign in first
                val authResult = try {
                    firebaseAuth.signInWithEmailAndPassword(email, password).await()
                } catch (e: Exception) {
                    // User doesn't exist, create account
                    val newAuthResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()

                    // Update display name if provided
                    displayName?.let { name ->
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        newAuthResult.user?.updateProfile(profileUpdates)?.await()
                    }

                    newAuthResult
                }

                val user = authResult.user ?: throw Exception("No user returned from Firebase")
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // MARK: - Sign Out (Firebase Auth)

    fun signOut() {
        firebaseAuth.signOut()
    }

    // MARK: - Delete Account (Firebase Auth)

    suspend fun deleteAccount(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = firebaseAuth.currentUser
                    ?: return@withContext Result.failure(Exception("No user signed in"))

                user.delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // MARK: - Password Reset

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firebaseAuth.sendPasswordResetEmail(email).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // MARK: - Update Profile

    suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = firebaseAuth.currentUser
                    ?: return@withContext Result.failure(Exception("No user signed in"))

                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()

                user.updateProfile(profileUpdates).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}