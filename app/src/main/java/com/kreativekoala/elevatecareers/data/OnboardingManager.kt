package com.kreativekoala.elevatecareers.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages onboarding completion state
 *
 * Tracks whether the user has completed onboarding, separate from auth state.
 * This prevents showing onboarding every time the app opens.
 */
object OnboardingManager {

    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

    /**
     * Check if user has completed onboarding
     */
    fun isOnboardingComplete(context: Context): Boolean {
        val prefs = getPrefs(context)
        val isComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        Log.d("OnboardingManager", "Onboarding complete: $isComplete")
        return isComplete
    }

    /**
     * Mark onboarding as complete
     * Call this when user finishes or skips onboarding
     */
    fun markOnboardingComplete(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
        Log.d("OnboardingManager", "✅ Onboarding marked as complete")
    }

    /**
     * Reset onboarding state
     * Call this when user signs out
     */
    fun resetOnboarding(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, false).apply()
        Log.d("OnboardingManager", "🔄 Onboarding reset")
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}