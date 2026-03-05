package com.kreativekoala.elevatecareers.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/**
 * Supabase client singleton for authentication, storage, and database operations
 */
object SupabaseService {

    private const val SUPABASE_URL = "https://uuntgvccvepqhfaupjqa.supabase.co" // TODO: Replace with your Supabase URL
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV1bnRndmNjdmVwcWhmYXVwanFhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjEwNzE3NzQsImV4cCI6MjA3NjY0Nzc3NH0.NiYjN9ZdtRhcoZyxVQh8qzgyAOHnowVC4JSeOs-Y9XA" // TODO: Replace with your anon key

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Storage)
        install(Postgrest)
    }

    // Auth helpers
    val auth: Auth get() = client.auth
    val storage: Storage get() = client.storage

    // MARK: - Authentication

    suspend fun signUp(email: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    this.provider = Google
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signInWithLinkedIn(accessToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // LinkedIn OAuth implementation
                // Note: This requires custom provider setup in Supabase
                // For now, we'll use a placeholder
                // TODO: Implement LinkedIn provider
                Result.failure(Exception("LinkedIn auth not yet implemented"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signOut(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    // Delete user data from database first
                    client.from("user_profile")
                        .delete {
                            filter {
                                eq("user_id", user.id)
                            }
                        }

                    // Sign out (Supabase doesn't have direct account deletion API)
                    // You'll need to implement this on your backend
                    client.auth.signOut()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getCurrentUser() = client.auth.currentUserOrNull()

    suspend fun getAuthToken(): String? {
        return try {
            client.auth.currentSessionOrNull()?.accessToken
        } catch (e: Exception) {
            null
        }
    }

    // MARK: - Storage (Resumes)

    suspend fun uploadResume(
        data: ByteArray,
        fileName: String,
        userId: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis() / 1000
                val path = "$userId/$timestamp-$fileName"

                client.storage
                    .from("resumes")
                    .upload(path, data, upsert = false)

                Result.success(path)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getResumeUrl(path: String, expiresIn: Long = 3600): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = client.storage
                    .from("resumes")
                    .createSignedUrl(path, expiresIn.seconds)

                Result.success(url)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun downloadResume(path: String): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val data = client.storage
                    .from("resumes")
                    .downloadAuthenticated(path)

                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteResume(path: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.storage
                    .from("resumes")
                    .delete(listOf(path))

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // MARK: - Database Queries

    suspend fun getProfile(userId: String): Result<UserProfile?> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = client.from("user_profile")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()

                Result.success(profile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val updateData = updates.toMutableMap()
                updateData["user_id"] = userId
                updateData["updated_at"] = System.currentTimeMillis()

                client.from("user_profile")
                    .upsert(updateData)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getSavedJobs(userId: String): Result<List<SavedJob>> {
        return withContext(Dispatchers.IO) {
            try {
                val savedJobs = client.from("saved_job")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<SavedJob>()

                Result.success(savedJobs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun saveJob(userId: String, jobId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("saved_job")
                    .upsert(
                        mapOf(
                            "user_id" to userId,
                            "job_id" to jobId,
                            "status" to "saved"
                        )
                    )

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}