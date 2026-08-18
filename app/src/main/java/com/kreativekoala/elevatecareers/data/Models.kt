package com.kreativekoala.elevatecareers.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("desired_salary_currency")
    val desiredSalaryCurrency: String? = null,
    val skills: List<String>? = null,
    @SerialName("work_authorization")
    val workAuthorization: List<String>? = null,
    @SerialName("requires_sponsorship")
    val requiresSponsorship: Boolean? = null,
    @SerialName("profile_completeness")
    val profileCompleteness: Int? = null,
    @SerialName("profile_source")
    val profileSource: String? = null,
    @SerialName("autofill_data")
    val autofillData: Map<String, String>? = null
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
    val description: String? = null,
    val achievements: List<String>? = null,
    @SerialName("skills_used")
    val skillsUsed: List<String>? = null
) {
    fun getDateRange(): String {
        val start = startDate.take(7) // YYYY-MM format
        return if (isCurrent) {
            "$start - Present"
        } else {
            val end = endDate?.take(7) ?: "Present"
            "$start - $end"
        }
    }
}

@Serializable
data class Education(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    val institution: String,
    val degree: String? = null,
    @SerialName("field_of_study")
    val fieldOfStudy: String? = null,
    @SerialName("start_date")
    val startDate: String? = null,
    @SerialName("end_date")
    val endDate: String? = null,
    val gpa: Double? = null
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
    @SerialName("file_size")
    val fileSize: Int? = null,
    @SerialName("file_type")
    val fileType: String? = null,
    @SerialName("uploaded_at")
    val uploadedAt: String,
    @SerialName("is_primary")
    val isPrimary: Boolean = false,
    @SerialName("raw_text")
    val rawText: String? = null
)

@Serializable
data class SavedJob(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("job_id")
    val jobId: String,
    val notes: String? = null,
    val status: String,
    @SerialName("saved_at")
    val savedAt: String,
    @SerialName("applied_at")
    val appliedAt: String? = null
)

@Serializable
data class ParseResumeRequest(
    @SerialName("storage_path")
    val storagePath: String,
    @SerialName("file_name")
    val fileName: String,
    @SerialName("file_size")
    val fileSize: Int
)

@Serializable
data class ParseResumeResponse(
    val success: Boolean,
    @SerialName("skills_count")
    val skillsCount: Int,
    @SerialName("experience_count")
    val experienceCount: Int,
    @SerialName("education_count")
    val educationCount: Int
)

@Serializable
data class LinkedInImportRequest(
    @SerialName("linkedin_data")
    val linkedInData: Map<String, String>
)

@Serializable
data class LinkedInImportResponse(
    val success: Boolean,
    val profile: UserProfile
)