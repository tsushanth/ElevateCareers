package com.kreativekoala.elevatecareers.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kreativekoala.elevatecareers.ui.auth.LinkedInProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LinkedIn Manager - Matches iOS LinkedInManager class
 *
 * Manages LinkedIn authentication state across the app
 * Similar to iOS @Published properties
 *
 * iOS Equivalent:
 * @Published var isLinkedInConnected = false
 * @Published var linkedInProfile: LinkedInProfile?
 */
object LinkedInManager {

    private const val PREFS_NAME = "linkedin_auth"
    private const val KEY_IS_CONNECTED = "is_connected"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_LAST_NAME = "last_name"
    private const val KEY_PROFILE_PICTURE = "profile_picture"

    // StateFlow = iOS @Published
    private val _isLinkedInConnected = MutableStateFlow(false)
    val isLinkedInConnected: StateFlow<Boolean> = _isLinkedInConnected.asStateFlow()

    private val _linkedInProfile = MutableStateFlow<LinkedInProfile?>(null)
    val linkedInProfile: StateFlow<LinkedInProfile?> = _linkedInProfile.asStateFlow()

    /**
     * Initialize - loads saved profile from SharedPreferences
     * Similar to iOS loadSavedProfile()
     */
    fun initialize(context: Context) {
        val prefs = getPrefs(context)
        val isConnected = prefs.getBoolean(KEY_IS_CONNECTED, false)

        if (isConnected) {
            val profile = LinkedInProfile(
                id = prefs.getString(KEY_USER_ID, "") ?: "",
                email = prefs.getString(KEY_EMAIL, null),
                firstName = prefs.getString(KEY_FIRST_NAME, null),
                lastName = prefs.getString(KEY_LAST_NAME, null),
                profilePicture = prefs.getString(KEY_PROFILE_PICTURE, null)
            )

            _isLinkedInConnected.value = true
            _linkedInProfile.value = profile

            Log.d("LinkedInManager", "✅ Loaded saved LinkedIn profile: ${profile.fullName}")
        } else {
            Log.d("LinkedInManager", "ℹ️ No saved LinkedIn profile")
        }
    }

    /**
     * Save profile after successful OAuth
     * Similar to iOS saving to UserDefaults
     */
    fun saveProfile(context: Context, profile: LinkedInProfile) {
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putBoolean(KEY_IS_CONNECTED, true)
            putString(KEY_USER_ID, profile.id)
            putString(KEY_EMAIL, profile.email)
            putString(KEY_FIRST_NAME, profile.firstName)
            putString(KEY_LAST_NAME, profile.lastName)
            putString(KEY_PROFILE_PICTURE, profile.profilePicture)
            apply()
        }

        _isLinkedInConnected.value = true
        _linkedInProfile.value = profile

        Log.d("LinkedInManager", "✅ Saved LinkedIn profile: ${profile.fullName}")
    }

    /**
     * Disconnect LinkedIn
     * Similar to iOS disconnectLinkedIn()
     */
    fun disconnect(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().clear().apply()

        _isLinkedInConnected.value = false
        _linkedInProfile.value = null

        Log.d("LinkedInManager", "✅ LinkedIn disconnected")
    }

    /**
     * Check if LinkedIn is connected (synchronous)
     */
    fun isConnected(): Boolean = _isLinkedInConnected.value

    /**
     * Get current profile (synchronous)
     */
    fun getCurrentProfile(): LinkedInProfile? = _linkedInProfile.value

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}