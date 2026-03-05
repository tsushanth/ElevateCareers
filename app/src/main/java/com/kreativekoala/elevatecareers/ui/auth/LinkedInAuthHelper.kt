package com.kreativekoala.elevatecareers.ui.auth

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * LinkedIn OAuth Helper - Matches iOS implementation exactly
 *
 * IMPORTANT: Requires INTERNET permission in AndroidManifest.xml:
 * <uses-permission android:name="android.permission.INTERNET" />
 */
object LinkedInAuthHelper {

    // Same as iOS
    private const val CLIENT_ID = "86fdw92kq8r7hg"
    private const val CLIENT_SECRET = "WPL_AP1.EraA7s5nnvopV7i3.YOlznQ=="

    // CRITICAL: Must match iOS - uses custom URL scheme for callback
    private const val REDIRECT_URI = "https://elevatecareers.us/linkedin-callback.html"

    // Updated scope to match iOS (openid profile email)
    private const val SCOPE = "openid profile email"

    private const val AUTH_URL = "https://www.linkedin.com/oauth/v2/authorization"
    private const val TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken"
    private const val PROFILE_URL = "https://api.linkedin.com/v2/userinfo"

    /**
     * Build LinkedIn OAuth URL - same as iOS buildAuthorizationURL()
     */
    fun getAuthorizationUrl(): String {
        return Uri.Builder()
            .scheme("https")
            .authority("www.linkedin.com")
            .path("oauth/v2/authorization")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("state", java.util.UUID.randomUUID().toString())
            .build()
            .toString()
    }

    /**
     * Create intent to open LinkedIn OAuth in browser
     */
    fun createAuthIntent(context: android.content.Context): android.content.Intent {
        val authUrl = getAuthorizationUrl()
        return android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(authUrl))
    }

    /**
     * Extract authorization code from callback URL
     * Matches iOS extractAuthorizationCode()
     */
    fun extractAuthorizationCode(uri: Uri): String? {
        // Check for error first
        val error = uri.getQueryParameter("error")
        if (error != null) {
            val errorDescription = uri.getQueryParameter("error_description")
            Log.e("LinkedInAuth", "LinkedIn returned error: $error")
            if (errorDescription != null) {
                Log.e("LinkedInAuth", "Error description: $errorDescription")
            }
            return null
        }

        // Get code from query parameters
        val code = uri.getQueryParameter("code")
        if (code != null) {
            Log.d("LinkedInAuth", "Code found: ${code.take(10)}...")
            return code
        }

        Log.e("LinkedInAuth", "No code found in callback URL")
        return null
    }

    /**
     * Exchange authorization code for access token
     * Matches iOS exchangeCodeForAccessToken()
     */
    suspend fun exchangeCodeForAccessToken(code: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("LinkedInAuth", "Exchanging code for access token...")

            val url = URL(TOKEN_URL)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                // Build request body with proper URL encoding
                val params = mapOf(
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "client_id" to CLIENT_ID,
                    "client_secret" to CLIENT_SECRET,
                    "redirect_uri" to REDIRECT_URI
                )

                val postData = params.entries.joinToString("&") { (key, value) ->
                    "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
                }

                Log.d("LinkedInAuth", "Request body (sanitized): ${postData.replace(CLIENT_SECRET, "***SECRET***")}")

                // Write request body
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                Log.d("LinkedInAuth", "Token exchange status: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }

                    Log.d("LinkedInAuth", "Token response received (length: ${response.length})")

                    val json = JSONObject(response)

                    if (json.has("error")) {
                        val error = json.getString("error")
                        val errorDescription = json.optString("error_description", "")
                        Log.e("LinkedInAuth", "Token exchange failed: $error - $errorDescription")
                        return@withContext Result.failure(Exception("$error: $errorDescription"))
                    }

                    if (json.has("access_token")) {
                        val accessToken = json.getString("access_token")
                        Log.d("LinkedInAuth", "✅ Got LinkedIn access token")
                        return@withContext Result.success(accessToken)
                    }

                    Log.e("LinkedInAuth", "No access token in response")
                    Result.failure(Exception("No access token in response"))
                } else {
                    // Read error response
                    val errorResponse = try {
                        BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                            reader.readText()
                        }
                    } catch (e: Exception) {
                        "Unable to read error response"
                    }

                    Log.e("LinkedInAuth", "Token exchange HTTP error: $responseCode")
                    Log.e("LinkedInAuth", "Error response: $errorResponse")
                    Result.failure(Exception("HTTP $responseCode: $errorResponse"))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e("LinkedInAuth", "Token exchange exception: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch user profile from LinkedIn
     * Matches iOS fetchUserProfile()
     */
    suspend fun fetchUserProfile(accessToken: String): Result<LinkedInProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d("LinkedInAuth", "Fetching LinkedIn profile...")

            val url = URL(PROFILE_URL)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val responseCode = connection.responseCode
                Log.d("LinkedInAuth", "Profile fetch status: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }

                    Log.d("LinkedInAuth", "Profile response received (length: ${response.length})")

                    val json = JSONObject(response)

                    val profile = LinkedInProfile(
                        id = json.optString("sub", ""),
                        email = json.optString("email", null),
                        firstName = json.optString("given_name", null),
                        lastName = json.optString("family_name", null),
                        profilePicture = json.optString("picture", null)
                    )

                    Log.d("LinkedInAuth", "✅ LinkedIn profile fetched: ${profile.fullName}")
                    Result.success(profile)
                } else {
                    val errorResponse = try {
                        BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                            reader.readText()
                        }
                    } catch (e: Exception) {
                        "Unable to read error response"
                    }

                    Log.e("LinkedInAuth", "Profile fetch HTTP error: $responseCode")
                    Log.e("LinkedInAuth", "Error response: $errorResponse")
                    Result.failure(Exception("HTTP $responseCode: $errorResponse"))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e("LinkedInAuth", "Profile fetch exception: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * LinkedIn Profile data class - matches iOS LinkedInProfile struct
 */
data class LinkedInProfile(
    val id: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val profilePicture: String?
) {
    val fullName: String
        get() {
            val first = firstName ?: ""
            val last = lastName ?: ""
            return "$first $last".trim()
        }
}