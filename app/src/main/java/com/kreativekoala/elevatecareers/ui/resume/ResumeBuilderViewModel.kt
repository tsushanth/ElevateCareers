package com.kreativekoala.elevatecareers.ui.resume

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kreativekoala.elevatecareers.data.model.*
import com.kreativekoala.elevatecareers.data.repository.ResumeRepository
import com.kreativekoala.elevatecareers.data.api.ResumeApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.UUID

/**
 * ViewModel for AI Resume Builder
 * No Hilt/Dagger dependencies - creates repository manually
 */
class ResumeBuilderViewModel(application: Application) : AndroidViewModel(application) {

    // Get user ID from Firebase Auth, or generate UUID as fallback
    private val userId: String by lazy {
        FirebaseAuth.getInstance().currentUser?.uid
            ?: UUID.randomUUID().toString()
    }

    // SharedPreferences for persisting conversation state
    private val prefs = application.getSharedPreferences(
        "resume_builder_prefs",
        Context.MODE_PRIVATE
    )

    // Create repository manually without dependency injection
    private val repository: ResumeRepository by lazy {
        val gson = GsonBuilder().setLenient().create()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(ResumeApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val apiService = retrofit.create(ResumeApiService::class.java)
        ResumeRepository(apiService)
    }

    private val _state = MutableStateFlow(ResumeBuilderState())
    val state: StateFlow<ResumeBuilderState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        setupSpeechRecognizer()
        loadSavedConversation()
    }

    /**
     * Load saved conversation from SharedPreferences
     */
    private fun loadSavedConversation() {
        val savedConversationId = prefs.getString("conversation_id", null)
        if (savedConversationId != null) {
            _state.update { it.copy(conversationId = savedConversationId) }
        }
    }

    /**
     * Save conversation ID to SharedPreferences
     */
    private fun saveConversationId(conversationId: String) {
        prefs.edit().putString("conversation_id", conversationId).apply()
    }

    /**
     * Clear saved conversation
     */
    private fun clearSavedConversation() {
        prefs.edit().remove("conversation_id").apply()
    }

    /**
     * Start a new AI resume conversation
     */
    fun startConversation() {
        // Check if we already have an active conversation
        if (_state.value.conversationId != null && _state.value.messages.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.startConversation(userId).fold(
                onSuccess = { response ->
                    saveConversationId(response.conversationId)

                    _state.update {
                        it.copy(
                            conversationId = response.conversationId,
                            messages = listOf(
                                ChatMessage(
                                    role = "assistant",
                                    content = response.message
                                )
                            ),
                            resumeData = response.resumeData,
                            progress = response.progress,
                            isComplete = response.isComplete,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    clearSavedConversation()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to start conversation"
                        )
                    }
                }
            )
        }
    }

    /**
     * Send a text message
     */
    fun sendMessage(message: String) {
        val conversationId = _state.value.conversationId

        if (conversationId == null) {
            _state.update {
                it.copy(error = "No active conversation. Please restart.")
            }
            return
        }

        // Add user message immediately
        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(
                    role = "user",
                    content = message
                ),
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            repository.sendMessage(conversationId, userId, message).fold(
                onSuccess = { response ->
                    _state.update {
                        it.copy(
                            messages = it.messages + ChatMessage(
                                role = "assistant",
                                content = response.message
                            ),
                            resumeData = response.resumeData,
                            progress = response.progress,
                            isComplete = response.isComplete,
                            pdfUrl = response.pdfUrl,
                            fileName = response.fileName,
                            resumeId = response.resumeId,
                            isLoading = false
                        )
                    }

                    // Clear saved conversation if complete AND PDF ready
                    if (response.isComplete && response.pdfUrl != null) {
                        clearSavedConversation()
                    }
                },
                onFailure = { error ->
                    // If conversation not found, clear and show message
                    if (error.message?.contains("404") == true ||
                        error.message?.contains("not found") == true) {
                        clearSavedConversation()
                        _state.update {
                            it.copy(
                                isLoading = false,
                                conversationId = null,
                                error = "Conversation expired. Please start a new conversation."
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to send message"
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * Reset conversation (start fresh)
     */
    fun resetConversation() {
        clearSavedConversation()
        _state.update { ResumeBuilderState() }
        startConversation()
    }

    /**
     * Setup speech recognizer
     */
    private fun setupSpeechRecognizer() {
        val context = getApplication<Application>().applicationContext

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.update { it.copy(isListening = true) }
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _state.update { it.copy(isListening = false) }
                    }

                    override fun onError(error: Int) {
                        _state.update {
                            it.copy(
                                isListening = false,
                                error = "Speech recognition error: $error"
                            )
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        matches?.firstOrNull()?.let { text ->
                            sendMessage(text)
                        }
                        _state.update { it.copy(isListening = false) }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    /**
     * Start voice input
     */
    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell me about yourself...")
        }

        speechRecognizer?.startListening(intent)
    }

    /**
     * Stop voice input
     */
    fun stopVoiceInput() {
        speechRecognizer?.stopListening()
        _state.update { it.copy(isListening = false) }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}