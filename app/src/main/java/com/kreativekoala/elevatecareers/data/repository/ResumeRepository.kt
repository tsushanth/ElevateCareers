package com.kreativekoala.elevatecareers.data.repository

import com.kreativekoala.elevatecareers.data.api.ResumeApiService
import com.kreativekoala.elevatecareers.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for AI Resume Builder
 * ⭐ NO HILT - works standalone
 */
class ResumeRepository(
    private val apiService: ResumeApiService
) {

    suspend fun startConversation(userId: String): Result<StartConversationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.startConversation(
                    StartConversationRequest(userId = userId)
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        userId: String,
        message: String
    ): Result<ChatResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.sendMessage(
                    ChatRequest(
                        conversationId = conversationId,
                        userId = userId,
                        message = message
                    )
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}