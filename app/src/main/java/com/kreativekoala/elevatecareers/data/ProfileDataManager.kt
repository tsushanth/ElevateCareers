package com.kreativekoala.elevatecareers.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Manages user profile data for autofill and job matching
 */
class ProfileDataManager(
    private val supabaseClient: SupabaseClient
) {

    companion object {
        private const val TAG = "ProfileDataManager"

        @Volatile
        private var instance: ProfileDataManager? = null

        fun getInstance(supabaseClient: SupabaseClient): ProfileDataManager {
            return instance ?: synchronized(this) {
                instance ?: ProfileDataManager(supabaseClient).also { instance = it }
            }
        }
    }

    /**
     * Get autofill data formatted for application forms
     */
    suspend fun getAutofillData(): Result<AutofillData> = withContext(Dispatchers.IO) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            // Fetch user profile
            val profile = supabaseClient
                .from("user_profile")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingle<UserProfile>()

            // Fetch primary resume
            val resume = try {
                supabaseClient
                    .from("resume")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("is_primary", true)
                        }
                    }
                    .decodeSingle<Resume>()
            } catch (e: Exception) {
                Log.w(TAG, "No resume found: ${e.message}")
                null
            }

            // Fetch most recent work experience
            val currentWork = try {
                supabaseClient
                    .from("work_experience")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("is_current", true)
                        }
                    }
                    .decodeSingle<WorkExperience>()
            } catch (e: Exception) {
                // Fallback to most recent
                try {
                    supabaseClient
                        .from("work_experience")
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }
                        .decodeList<WorkExperience>()
                        .maxByOrNull { it.startDate }
                } catch (e: Exception) {
                    null
                }
            }

            val autofill = AutofillData(
                // Basic Info
                fullName = profile.fullName ?: "",
                firstName = profile.fullName?.split(" ")?.firstOrNull() ?: "",
                lastName = profile.fullName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                email = profile.email ?: "",
                phone = profile.phone ?: "",

                // Location
                city = profile.locationCity ?: "",
                country = profile.locationCountry ?: "",

                // Links
                linkedinUrl = profile.linkedinUrl ?: "",
                portfolioUrl = profile.portfolioUrl ?: "",
                githubUrl = profile.githubUrl ?: "",

                // Professional
                headline = profile.headline ?: "",
                summary = profile.summary ?: "",
                yearsOfExperience = profile.yearsOfExperience ?: 0,
                skills = profile.skills ?: emptyList(),

                // Current Position
                currentCompany = currentWork?.companyName ?: "",
                currentTitle = currentWork?.jobTitle ?: "",

                // Resume
                resumeText = resume?.rawText,
                resumeFilePath = resume?.filePath,
                resumeFileName = resume?.fileName,

                // Work Authorization
                workAuthorization = profile.workAuthorization ?: emptyList(),
                requiresSponsorship = profile.requiresSponsorship ?: false,

                // Custom fields from JSONB
                customFields = profile.autofillData
            )

            Log.d(TAG, "✅ Autofill data loaded for: ${profile.fullName}")
            Result.success(autofill)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get autofill data: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get user preferences for job filtering
     */
    suspend fun getUserPreferences(): Result<JobPreferences> = withContext(Dispatchers.IO) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val profile = supabaseClient
                .from("user_profile")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingle<UserProfile>()

            val prefs = JobPreferences(
                desiredRoles = profile.desiredRoles ?: emptyList(),
                desiredLocations = profile.desiredLocations ?: emptyList(),
                remotePreference = profile.remotePreference,
                desiredEmploymentTypes = profile.desiredEmploymentTypes ?: emptyList(),
                desiredSalaryMin = profile.desiredSalaryMin,
                desiredSalaryMax = profile.desiredSalaryMax,
                skills = profile.skills ?: emptyList()
            )

            Result.success(prefs)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get preferences: ${e.message}", e)
            Result.failure(e)
        }
    }
}

// ============================================
// DATA MODELS
// ============================================

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("location_city")
    val locationCity: String? = null,
    @SerialName("location_country")
    val locationCountry: String? = null,
    @SerialName("linkedin_url")
    val linkedinUrl: String? = null,
    @SerialName("portfolio_url")
    val portfolioUrl: String? = null,
    @SerialName("github_url")
    val githubUrl: String? = null,
    val headline: String? = null,
    val summary: String? = null,
    @SerialName("years_of_experience")
    val yearsOfExperience: Int? = null,
    @SerialName("desired_roles")
    val desiredRoles: List<String>? = null,
    @SerialName("desired_locations")
    val desiredLocations: List<String>? = null,
    @SerialName("remote_preference")
    val remotePreference: String? = null,
    @SerialName("desired_employment_types")
    val desiredEmploymentTypes: List<String>? = null,
    @SerialName("desired_salary_min")
    val desiredSalaryMin: Int? = null,
    @SerialName("desired_salary_max")
    val desiredSalaryMax: Int? = null,
    val skills: List<String>? = null,
    @SerialName("work_authorization")
    val workAuthorization: List<String>? = null,
    @SerialName("requires_sponsorship")
    val requiresSponsorship: Boolean? = null,
    @SerialName("autofill_data")
    val autofillData: Map<String, String>? = null
)

@Serializable
data class Resume(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("file_name")
    val fileName: String,
    @SerialName("file_path")
    val filePath: String,
    @SerialName("raw_text")
    val rawText: String? = null,
    @SerialName("is_primary")
    val isPrimary: Boolean = true
)

@Serializable
data class WorkExperience(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("job_title")
    val jobTitle: String,
    val location: String? = null,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String? = null,
    @SerialName("is_current")
    val isCurrent: Boolean = false,
    val description: String? = null
)

/**
 * Autofill data ready for form injection
 */
data class AutofillData(
    // Basic
    val fullName: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,

    // Location
    val city: String,
    val country: String,

    // Links
    val linkedinUrl: String,
    val portfolioUrl: String,
    val githubUrl: String,

    // Professional
    val headline: String,
    val summary: String,
    val yearsOfExperience: Int,
    val skills: List<String>,

    // Current Position
    val currentCompany: String,
    val currentTitle: String,

    // Resume
    val resumeText: String?,
    val resumeFilePath: String?,
    val resumeFileName: String?,

    // Work Authorization
    val workAuthorization: List<String>,
    val requiresSponsorship: Boolean,

    // Custom
    val customFields: Map<String, String>?
)

/**
 * User preferences for job filtering
 */
data class JobPreferences(
    val desiredRoles: List<String>,
    val desiredLocations: List<String>,
    val remotePreference: String?,
    val desiredEmploymentTypes: List<String>,
    val desiredSalaryMin: Int?,
    val desiredSalaryMax: Int?,
    val skills: List<String>
)