package com.kreativekoala.elevatecareers

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreativekoala.elevatecareers.data.JobPreferences
import com.kreativekoala.elevatecareers.data.ProfileDataManager
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobListUiState(
    val jobs: List<Job> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isRemoteOnly: Boolean = false,
    val selectedEmploymentType: String? = null,
    val userPreferences: JobPreferences? = null,
    val isPersonalized: Boolean = false
)

class JobListViewModel(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val apiService = ApiService()
    private val profileDataManager = ProfileDataManager.getInstance(supabaseClient)

    private val _uiState = MutableStateFlow(JobListUiState())
    val uiState: StateFlow<JobListUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "JobListViewModel"
    }

    init {
        loadUserPreferences()
        loadJobs()
    }

    /**
     * Load user preferences for personalized filtering
     */
    private fun loadUserPreferences() {
        viewModelScope.launch {
            val result = profileDataManager.getUserPreferences()

            result.fold(
                onSuccess = { prefs ->
                    _uiState.update {
                        it.copy(
                            userPreferences = prefs,
                            isPersonalized = true
                        )
                    }
                    Log.d(TAG, "✅ User preferences loaded")

                    // Reload jobs with preferences
                    loadJobs()
                },
                onFailure = { error ->
                    Log.w(TAG, "Could not load preferences: ${error.message}")
                    // Continue without personalization
                }
            )
        }
    }

    /**
     * Load jobs with optional personalization
     */
    fun loadJobs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = apiService.searchJobs(
                keyword = _uiState.value.searchQuery.ifBlank { null },
                remote = if (_uiState.value.isRemoteOnly) true else null,
                employmentType = _uiState.value.selectedEmploymentType
            )

            result.fold(
                onSuccess = { jobs ->
                    // Apply personalization if available
                    val personalizedJobs = if (_uiState.value.isPersonalized) {
                        applyPersonalization(jobs)
                    } else {
                        jobs
                    }

                    _uiState.update {
                        it.copy(
                            jobs = personalizedJobs,
                            isLoading = false,
                            error = null
                        )
                    }
                    Log.d(TAG, "✅ Loaded ${personalizedJobs.size} jobs")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load jobs"
                        )
                    }
                    Log.e(TAG, "❌ Failed to load jobs: ${error.message}")
                }
            )
        }
    }

    /**
     * Apply basic personalization filtering to jobs
     */
    private fun applyPersonalization(jobs: List<Job>): List<Job> {
        val prefs = _uiState.value.userPreferences ?: return jobs

        Log.d(TAG, "🎯 Applying personalization: ${prefs.skills.size} skills, ${prefs.desiredRoles.size} roles")

        return jobs
            .map { job -> job to calculateJobScore(job, prefs) }
            .sortedByDescending { it.second } // Sort by score
            .map { it.first }
    }

    /**
     * Calculate basic job match score (0-100)
     */
    private fun calculateJobScore(job: Job, prefs: JobPreferences): Int {
        var score = 50 // Base score

        // Skills match (max +30 points)
        if (prefs.skills.isNotEmpty() && job.skills.isNotEmpty()) {
            val matchingSkills = job.skills.count { jobSkill ->
                prefs.skills.any { prefSkill ->
                    prefSkill.equals(jobSkill, ignoreCase = true)
                }
            }
            val skillScore = (matchingSkills.toFloat() / prefs.skills.size * 30).toInt()
            score += skillScore

            if (matchingSkills > 0) {
                Log.d(TAG, "  ${job.title}: +$skillScore pts (${matchingSkills}/${prefs.skills.size} skills match)")
            }
        }

        // Role match (max +20 points)
        if (prefs.desiredRoles.isNotEmpty()) {
            val roleMatches = prefs.desiredRoles.any { role ->
                job.title.contains(role, ignoreCase = true)
            }
            if (roleMatches) {
                score += 20
                Log.d(TAG, "  ${job.title}: +20 pts (role match)")
            }
        }

        // Remote preference (+10 points)
        if (prefs.remotePreference == "remote_only" && job.remote) {
            score += 10
        } else if (prefs.remotePreference == "onsite" && !job.remote) {
            score += 10
        }

        // Location match (+10 points)
        if (prefs.desiredLocations.isNotEmpty()) {
            val locationMatches = prefs.desiredLocations.any { loc ->
                job.locations.any { jobLoc ->
                    jobLoc.city?.contains(loc, ignoreCase = true) == true ||
                            jobLoc.region?.contains(loc, ignoreCase = true) == true ||
                            jobLoc.country?.contains(loc, ignoreCase = true) == true
                }
            }
            if (locationMatches) {
                score += 10
            }
        }

        // Employment type match (+10 points)
        if (prefs.desiredEmploymentTypes.isNotEmpty() && job.employmentType != null) {
            if (prefs.desiredEmploymentTypes.contains(job.employmentType)) {
                score += 10
            }
        }

        return score.coerceIn(0, 100)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun search() {
        loadJobs()
    }

    fun toggleRemoteFilter() {
        _uiState.update { it.copy(isRemoteOnly = !it.isRemoteOnly) }
        loadJobs()
    }

    fun selectEmploymentType(type: String?) {
        _uiState.update { it.copy(selectedEmploymentType = type) }
        loadJobs()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        apiService.close()
    }
}