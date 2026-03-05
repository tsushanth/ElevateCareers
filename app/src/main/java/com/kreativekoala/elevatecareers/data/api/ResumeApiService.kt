package com.kreativekoala.elevatecareers.data.api

import com.kreativekoala.elevatecareers.data.model.*
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API Service for AI Resume Builder
 */
interface ResumeApiService {

    @POST("api/ai-resume/start")
    suspend fun startConversation(
        @Body request: StartConversationRequest
    ): StartConversationResponse

    @POST("api/ai-resume/chat")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): ChatResponse

    companion object {
        const val BASE_URL = "https://elevate-careers-917362189743.europe-west1.run.app/"
    }
}