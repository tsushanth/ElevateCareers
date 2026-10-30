package com.kreativekoala.elevatecareers

import kotlinx.serialization.json.Json

/**
 * Central place for backend endpoints used by the job feed.
 *
 * The job feed (list + detail) is public and needs no authentication.
 * Profile, resume and LinkedIn calls still use ApiService.legacyBaseUrl and are
 * intentionally NOT repointed here (they need an auth decision first).
 */
object ApiConfig {
    const val JOBS_API_BASE_URL = "https://elevate-careers-api.fly.dev"

    /** Lenient on purpose: one odd field from the server must never break the feed. */
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
}
