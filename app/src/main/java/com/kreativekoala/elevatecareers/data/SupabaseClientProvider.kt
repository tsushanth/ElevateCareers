package com.kreativekoala.elevatecareers.data

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Provides singleton Supabase client instance
 *
 * Usage: SupabaseClientProvider.getClient(context)
 */
object SupabaseClientProvider {

    @Volatile
    private var client: SupabaseClient? = null

    fun getClient(context: Context): SupabaseClient {
        return client ?: synchronized(this) {
            client ?: createClient(context).also { client = it }
        }
    }

    private fun createClient(context: Context): SupabaseClient {
        // TODO: Add these to your strings.xml or BuildConfig:
        // <string name="supabase_url">YOUR_SUPABASE_URL</string>
        // <string name="supabase_anon_key">YOUR_SUPABASE_ANON_KEY</string>

        val supabaseUrl = try {
            context.getString(com.kreativekoala.elevatecareers.R.string.supabase_url)
        } catch (e: Exception) {
            // Fallback - replace with your actual URL
            "https://your-project.supabase.co"
        }

        val supabaseKey = try {
            context.getString(com.kreativekoala.elevatecareers.R.string.supabase_anon_key)
        } catch (e: Exception) {
            // Fallback - replace with your actual key
            "your-anon-key"
        }

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Postgrest)
            install(Storage)
        }
    }
}