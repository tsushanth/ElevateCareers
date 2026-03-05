package com.kreativekoala.elevatecareers.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kreativekoala.elevatecareers.data.AuthService
import com.kreativekoala.elevatecareers.data.LinkedInManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val userEmail: String? = null,
    val userName: String? = null,
    val userId: String? = null,
    val isAnonymous: Boolean = false
)

/**
 * AuthViewModel - Uses Firebase Auth (Same as iOS)
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authService = AuthService.getInstance(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    // MARK: - Check Auth Status

    fun checkAuthStatus() {
        val user = authService.getCurrentUser()
        _uiState.update {
            it.copy(
                isAuthenticated = user != null,
                userEmail = user?.email,
                userName = user?.displayName,
                userId = user?.uid
            )
        }
    }

    // MARK: - Google Sign In (Firebase Auth - Same as iOS)

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authService.signInWithGoogle(idToken)

            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userEmail = user.email,
                            userName = user.displayName,
                            userId = user.uid,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Google sign in failed"
                        )
                    }
                }
            )
        }
    }

    // MARK: - LinkedIn Sign In (Creates/Signs into Firebase)

    /**
     * ⭐ NEW: Sign in with LinkedIn by creating/signing into Firebase
     * This unifies LinkedIn auth with Firebase auth
     */
    fun signInWithLinkedIn(profile: LinkedInProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (profile.email == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "LinkedIn email is required"
                    )
                }
                return@launch
            }

            // Sign into Firebase using LinkedIn credentials
            val result = authService.createOrSignInLinkedInUser(
                email = profile.email,
                linkedInId = profile.id,
                displayName = profile.fullName
            )

            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userEmail = user.email,
                            userName = profile.fullName,
                            userId = user.uid,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "LinkedIn sign in failed: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    // MARK: - Email/Password Sign In (Firebase Auth - Same as iOS)

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authService.signInWithEmail(email, password)

            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userEmail = user.email,
                            userName = user.displayName,
                            userId = user.uid,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Sign in failed"
                        )
                    }
                }
            )
        }
    }

    /**
     * Check if user is authenticated
     * Now only checks Firebase since LinkedIn users are also in Firebase
     */
    fun isUserAuthenticated(context: android.content.Context): Boolean {
        return _uiState.value.isAuthenticated
    }

    // MARK: - Email/Password Sign Up (Firebase Auth - Same as iOS)

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authService.signUpWithEmail(email, password)

            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userEmail = user.email,
                            userName = user.displayName,
                            userId = user.uid,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Sign up failed"
                        )
                    }
                }
            )
        }
    }

    // MARK: - Proceed Without Sign In

    fun proceedWithoutSignIn() {
        _uiState.update {
            it.copy(
                isAuthenticated = true,
                isAnonymous = true
            )
        }
    }

    // MARK: - Sign Out

    fun signOut() {
        authService.signOut()
        _uiState.update { AuthUiState() }
    }

    // MARK: - Delete Account

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authService.deleteAccount()

            result.fold(
                onSuccess = {
                    _uiState.update { AuthUiState() }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Delete account failed"
                        )
                    }
                }
            )
        }
    }

    // MARK: - Clear Error

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}