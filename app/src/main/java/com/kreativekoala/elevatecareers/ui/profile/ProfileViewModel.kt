package com.kreativekoala.elevatecareers.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreativekoala.elevatecareers.ApiService
import com.kreativekoala.elevatecareers.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val workExperience: List<WorkExperience> = emptyList(),
    val education: List<Education> = emptyList(),
    val resumes: List<Resume> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val uploadProgress: Float = 0f,
    val successMessage: String? = null
)

class ProfileViewModel : ViewModel() {

    private val apiService = ApiService()
    private val supabase = SupabaseService

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val hasResume: Boolean
        get() = _uiState.value.resumes.isNotEmpty()

    init {
        loadProfile()
    }

    // MARK: - Load Profile

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val user = supabase.getCurrentUser()
            if (user == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Not authenticated"
                    )
                }
                return@launch
            }

            val result = supabase.getProfile(user.id)

            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load profile"
                        )
                    }
                }
            )
        }
    }

    // MARK: - Upload Resume

    fun uploadResume(fileData: ByteArray, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, uploadProgress = 0f, error = null) }

            try {
                val user = supabase.getCurrentUser()
                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Not authenticated"
                        )
                    }
                    return@launch
                }

                val userId = user.id

                // Update progress
                _uiState.update { it.copy(uploadProgress = 0.3f) }

                // 1. Upload to Supabase Storage
                val uploadResult = supabase.uploadResume(fileData, fileName, userId)

                if (uploadResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to upload: ${uploadResult.exceptionOrNull()?.message}"
                        )
                    }
                    return@launch
                }

                val storagePath = uploadResult.getOrNull()!!

                // Update progress
                _uiState.update { it.copy(uploadProgress = 0.6f) }

                // 2. Tell backend to parse
                val parseResult = apiService.parseResume(
                    storagePath = storagePath,
                    fileName = fileName,
                    fileSize = fileData.size
                )

                if (parseResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to parse: ${parseResult.exceptionOrNull()?.message}"
                        )
                    }
                    return@launch
                }

                val parseResponse = parseResult.getOrNull()!!

                // Update progress
                _uiState.update { it.copy(uploadProgress = 0.9f) }

                // 3. Reload profile
                loadProfile()

                // Update progress
                _uiState.update {
                    it.copy(
                        uploadProgress = 1.0f,
                        successMessage = "Resume uploaded! Found ${parseResponse.skillsCount} skills and ${parseResponse.experienceCount} experiences."
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Upload failed: ${e.message}"
                    )
                }
            }
        }
    }

    // MARK: - View Resume

    suspend fun getResumeUrl(): String? {
        val resume = _uiState.value.resumes.firstOrNull() ?: return null

        val result = supabase.getResumeUrl(resume.filePath)
        return result.getOrNull()
    }

    // MARK: - Delete Resume

    fun deleteResume() {
        viewModelScope.launch {
            val resume = _uiState.value.resumes.firstOrNull() ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Delete from storage
                supabase.deleteResume(resume.filePath)

                // Delete from database
                apiService.deleteResume(resume.id)

                // Reload profile
                loadProfile()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Resume deleted successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to delete: ${e.message}"
                    )
                }
            }
        }
    }

    // MARK: - Update Profile

    fun updateProfile(updates: Map<String, Any>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val user = supabase.getCurrentUser()
            if (user == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Not authenticated"
                    )
                }
                return@launch
            }

            val result = supabase.updateProfile(user.id, updates)

            result.fold(
                onSuccess = {
                    loadProfile()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Profile updated successfully"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to update: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    // MARK: - LinkedIn Import

    fun importFromLinkedIn(linkedInData: Map<String, String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = apiService.importLinkedIn(linkedInData)

            result.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            profile = response.profile,
                            isLoading = false,
                            successMessage = "LinkedIn profile imported successfully!"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to import: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    // MARK: - Clear Messages

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        apiService.close()
    }
}