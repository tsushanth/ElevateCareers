package com.kreativekoala.elevatecareers

import com.kreativekoala.elevatecareers.data.LinkedInImportRequest
import com.kreativekoala.elevatecareers.data.LinkedInImportResponse
import com.kreativekoala.elevatecareers.data.ParseResumeRequest
import com.kreativekoala.elevatecareers.data.ParseResumeResponse
import com.kreativekoala.elevatecareers.data.SupabaseService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiService {

    private val baseUrl = "https://elevate-careers-917362189743.europe-west1.run.app"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    suspend fun searchJobs(
        keyword: String? = null,
        remote: Boolean? = null,
        location: String? = null,
        employmentType: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Job>> {
        return try {
            val response = client.get("$baseUrl/jobs") {
                parameter("limit", limit)
                parameter("offset", offset)
                keyword?.let { parameter("keyword", it) }
                remote?.let { parameter("remote", it) }
                location?.let { parameter("location", it) }
                employmentType?.let { parameter("employment_type", it) }
            }

            val jobsResponse: JobsResponse = response.body()
            Result.success(jobsResponse.jobs)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getJob(id: String): Result<Job?> {
        return try {
            val job: Job = client.get("$baseUrl/jobs/$id").body()
            Result.success(job)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }

    suspend fun parseResume(
        storagePath: String,
        fileName: String,
        fileSize: Int
    ): Result<ParseResumeResponse> {
        return try {
            val response = client.post("$baseUrl/api/profile/resume/parse") {
                setBody(ParseResumeRequest(storagePath, fileName, fileSize))
                header("Authorization", "Bearer ${SupabaseService.getAuthToken()}")
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteResume(resumeId: String): Result<Unit> {
        return try {
            client.delete("$baseUrl/api/profile/resume/$resumeId") {
                header("Authorization", "Bearer ${SupabaseService.getAuthToken()}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importLinkedIn(
        linkedInData: Map<String, String>
    ): Result<LinkedInImportResponse> {
        return try {
            val response = client.post("$baseUrl/api/profile/linkedin") {
                setBody(LinkedInImportRequest(linkedInData))
                header("Authorization", "Bearer ${SupabaseService.getAuthToken()}")
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}