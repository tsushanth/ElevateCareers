package com.kreativekoala.elevatecareers.ui.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kreativekoala.elevatecareers.JobListViewModel
import com.kreativekoala.elevatecareers.ui.application.JobApplicationViewModel
import io.github.jan.supabase.SupabaseClient

/**
 * Factory for creating ViewModels that require SupabaseClient
 */
class ViewModelFactory(
    private val supabaseClient: SupabaseClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(JobListViewModel::class.java) -> {
                JobListViewModel(supabaseClient) as T
            }
            modelClass.isAssignableFrom(JobApplicationViewModel::class.java) -> {
                JobApplicationViewModel(supabaseClient) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}