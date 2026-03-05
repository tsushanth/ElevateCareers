package com.kreativekoala.elevatecareers.data.model

import com.google.gson.annotations.SerializedName

/**
 * Resume data structure matching backend
 */
data class ResumeData(
    val personalInfo: PersonalInfo = PersonalInfo(),
    val education: List<Education> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val skills: List<String> = emptyList()
)

data class PersonalInfo(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = ""
)

data class Education(
    val school: String = "",
    val degree: String = "",
    val field: String = "",
    val graduationYear: String = ""
)

data class Experience(
    val company: String = "",
    val position: String = "",
    val duration: String = "",
    val responsibilities: List<String> = emptyList()
)

/**
 * Chat message in the conversation
 */
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: String = ""
)

/**
 * API Request/Response models
 */
data class StartConversationRequest(
    val userId: String
)

data class StartConversationResponse(
    val conversationId: String,
    val message: String,
    val resumeData: ResumeData,
    val progress: Float,
    val isComplete: Boolean
)

data class ChatRequest(
    val conversationId: String,
    val userId: String,
    val message: String
)

data class ChatResponse(
    val message: String,
    val resumeData: ResumeData,
    val progress: Float,
    val isComplete: Boolean,
    val resumeId: Int? = null,        // ✅ CHANGED: String -> Int to match database
    val pdfUrl: String? = null,
    val fileName: String? = null,
    val stage: String
)

/**
 * UI State
 */
data class ResumeBuilderState(
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val resumeData: ResumeData = ResumeData(),
    val progress: Float = 0f,
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pdfUrl: String? = null,
    val fileName: String? = null,
    val resumeId: Int? = null,        // ✅ ADDED: Track database resume ID
    val isListening: Boolean = false
)

/**
 * Resume entry method
 */
enum class ResumeEntryMethod {
    AI_VOICE,
    UPLOAD,
    MANUAL
}