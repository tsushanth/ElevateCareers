package com.kreativekoala.elevatecareers.ui.application

import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreativekoala.elevatecareers.data.AutofillData
import com.kreativekoala.elevatecareers.data.ProfileDataManager
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobApplicationUiState(
    val isLoading: Boolean = true,
    val isAutoFilling: Boolean = false,
    val autofillData: AutofillData? = null,
    val unfilledFields: List<String> = emptyList(),
    val error: String? = null
)

class JobApplicationViewModel(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val profileDataManager = ProfileDataManager.getInstance(supabaseClient)

    private val _uiState = MutableStateFlow(JobApplicationUiState())
    val uiState: StateFlow<JobApplicationUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "JobApplicationVM"
    }

    fun loadAutofillData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = profileDataManager.getAutofillData()

            result.fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            autofillData = data,
                            error = null
                        )
                    }
                    Log.d(TAG, "✅ Autofill data loaded: ${data.fullName}")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load data"
                        )
                    }
                    Log.e(TAG, "❌ Failed to load autofill data", error)
                }
            )
        }
    }

    /**
     * Inject autofill data into the web page
     */
    fun injectAutofill(webView: WebView, url: String) {
        val data = _uiState.value.autofillData ?: return

        _uiState.update { it.copy(isAutoFilling = true) }

        // Detect job board type from URL
        val boardType = detectJobBoardType(url)
        Log.d(TAG, "🎯 Detected job board: $boardType for URL: $url")

        // Get appropriate JavaScript for this board
        val script = when (boardType) {
            JobBoardType.GREENHOUSE -> getGreenhouseAutofillScript(data)
            JobBoardType.LEVER -> getLeverAutofillScript(data)
            JobBoardType.JSONLD -> getJSONLDAutofillScript(data)
            JobBoardType.GENERIC -> getGenericAutofillScript(data)
        }

        // Inject JavaScript
        webView.evaluateJavascript(script) { result ->
            Log.d(TAG, "Autofill result: $result")
            _uiState.update { it.copy(isAutoFilling = false) }

            // Detect unfilled fields
            detectUnfilledFields(webView)
        }
    }

    /**
     * Detect unfilled form fields
     */
    private fun detectUnfilledFields(webView: WebView) {
        val script = """
            (function() {
                const unfilled = [];
                const inputs = document.querySelectorAll('input[type="text"], input[type="email"], input[type="tel"], textarea');
                
                inputs.forEach(input => {
                    if (!input.value || input.value.trim() === '') {
                        const label = input.labels?.[0]?.textContent || input.placeholder || input.name || 'Unknown field';
                        unfilled.push(label.trim());
                    }
                });
                
                return JSON.stringify(unfilled);
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            try {
                val unfilledList = result
                    ?.removeSurrounding("\"")
                    ?.replace("\\\"", "\"")
                    ?.let { kotlinx.serialization.json.Json.decodeFromString<List<String>>(it) }
                    ?: emptyList()

                _uiState.update { it.copy(unfilledFields = unfilledList) }
                Log.d(TAG, "📝 Unfilled fields: $unfilledList")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse unfilled fields", e)
            }
        }
    }

    /**
     * Fill a specific field with voice input value
     */
    fun fillField(webView: WebView, fieldName: String, value: String) {
        val script = """
            (function() {
                const inputs = document.querySelectorAll('input, textarea, select');
                
                for (const input of inputs) {
                    const label = input.labels?.[0]?.textContent || input.placeholder || input.name || '';
                    
                    if (label.toLowerCase().includes('${fieldName.lowercase()}')) {
                        input.value = '$value';
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                        return true;
                    }
                }
                
                return false;
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            Log.d(TAG, "Filled field '$fieldName': $result")
            // Re-detect unfilled fields
            detectUnfilledFields(webView)
        }
    }

    fun setUnfilledFields(fields: List<String>) {
        _uiState.update { it.copy(unfilledFields = fields) }
    }

    // ============================================
    // JOB BOARD DETECTION
    // ============================================

    private fun detectJobBoardType(url: String): JobBoardType {
        return when {
            url.contains("greenhouse.io") || url.contains("boards.greenhouse.io") -> JobBoardType.GREENHOUSE
            url.contains("lever.co") || url.contains("jobs.lever.co") -> JobBoardType.LEVER
            url.contains("schema.org/JobPosting") -> JobBoardType.JSONLD
            else -> JobBoardType.GENERIC
        }
    }

    // ============================================
    // GREENHOUSE AUTOFILL
    // ============================================

    private fun getGreenhouseAutofillScript(data: AutofillData): String {
        return """
            (function() {
                console.log('🟢 Greenhouse autofill starting...');
                
                // Helper function to fill input
                function fillInput(selector, value) {
                    const input = document.querySelector(selector);
                    if (input && value) {
                        input.value = value;
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                        input.dispatchEvent(new Event('blur', { bubbles: true }));
                        console.log('Filled:', selector, '=', value);
                        return true;
                    }
                    return false;
                }
                
                // Greenhouse common field IDs
                fillInput('#first_name', '${data.firstName.escapeJS()}');
                fillInput('#last_name', '${data.lastName.escapeJS()}');
                fillInput('#email', '${data.email.escapeJS()}');
                fillInput('#phone', '${data.phone.escapeJS()}');
                
                // Alternative selectors
                fillInput('input[name="job_application[first_name]"]', '${data.firstName.escapeJS()}');
                fillInput('input[name="job_application[last_name]"]', '${data.lastName.escapeJS()}');
                fillInput('input[name="job_application[email]"]', '${data.email.escapeJS()}');
                fillInput('input[name="job_application[phone]"]', '${data.phone.escapeJS()}');
                
                // LinkedIn URL
                fillInput('input[name="job_application[linkedin_profile]"]', '${data.linkedinUrl.escapeJS()}');
                fillInput('input[placeholder*="LinkedIn"]', '${data.linkedinUrl.escapeJS()}');
                
                // Website/Portfolio
                fillInput('input[name="job_application[website]"]', '${data.portfolioUrl.escapeJS()}');
                fillInput('input[placeholder*="Portfolio"]', '${data.portfolioUrl.escapeJS()}');
                fillInput('input[placeholder*="Website"]', '${data.portfolioUrl.escapeJS()}');
                
                // GitHub
                fillInput('input[placeholder*="GitHub"]', '${data.githubUrl.escapeJS()}');
                
                // Location
                fillInput('input[name="job_application[location]"]', '${data.city.escapeJS()}');
                
                // Cover letter / Summary
                const coverLetter = document.querySelector('textarea[name="job_application[cover_letter]"]');
                if (coverLetter && !coverLetter.value) {
                    coverLetter.value = '${data.summary.escapeJS()}';
                    coverLetter.dispatchEvent(new Event('input', { bubbles: true }));
                }
                
                console.log('✅ Greenhouse autofill complete');
                return 'success';
            })();
        """.trimIndent()
    }

    // ============================================
    // LEVER AUTOFILL
    // ============================================

    private fun getLeverAutofillScript(data: AutofillData): String {
        return """
            (function() {
                console.log('🔵 Lever autofill starting...');
                
                function fillInput(selector, value) {
                    const input = document.querySelector(selector);
                    if (input && value) {
                        input.value = value;
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                        return true;
                    }
                    return false;
                }
                
                // Lever field names
                fillInput('input[name="name"]', '${data.fullName.escapeJS()}');
                fillInput('input[name="email"]', '${data.email.escapeJS()}');
                fillInput('input[name="phone"]', '${data.phone.escapeJS()}');
                fillInput('input[name="org"]', '${data.currentCompany.escapeJS()}');
                fillInput('input[name="urls[LinkedIn]"]', '${data.linkedinUrl.escapeJS()}');
                fillInput('input[name="urls[Portfolio]"]', '${data.portfolioUrl.escapeJS()}');
                fillInput('input[name="urls[GitHub]"]', '${data.githubUrl.escapeJS()}');
                
                // Additional info
                const additionalInfo = document.querySelector('textarea[name="comments"]');
                if (additionalInfo && !additionalInfo.value) {
                    additionalInfo.value = '${data.summary.escapeJS()}';
                    additionalInfo.dispatchEvent(new Event('input', { bubbles: true }));
                }
                
                console.log('✅ Lever autofill complete');
                return 'success';
            })();
        """.trimIndent()
    }

    // ============================================
    // JSON-LD / GENERIC AUTOFILL
    // ============================================

    private fun getJSONLDAutofillScript(data: AutofillData): String {
        return """
            (function() {
                console.log('🟡 JSON-LD/Generic autofill starting...');
                
                function fillInput(input, value) {
                    if (input && value) {
                        input.value = value;
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                        return true;
                    }
                    return false;
                }
                
                function matchesField(input, keywords) {
                    const text = (
                        input.name + ' ' +
                        input.id + ' ' +
                        input.placeholder + ' ' +
                        (input.labels?.[0]?.textContent || '')
                    ).toLowerCase();
                    
                    return keywords.some(keyword => text.includes(keyword));
                }
                
                // Get all text inputs
                const inputs = document.querySelectorAll('input[type="text"], input[type="email"], input[type="tel"], input[type="url"], textarea');
                
                inputs.forEach(input => {
                    // Name fields
                    if (matchesField(input, ['first name', 'firstname', 'fname'])) {
                        fillInput(input, '${data.firstName.escapeJS()}');
                    } else if (matchesField(input, ['last name', 'lastname', 'lname', 'surname'])) {
                        fillInput(input, '${data.lastName.escapeJS()}');
                    } else if (matchesField(input, ['full name', 'fullname', 'name']) && !input.value) {
                        fillInput(input, '${data.fullName.escapeJS()}');
                    }
                    
                    // Email
                    else if (matchesField(input, ['email', 'e-mail'])) {
                        fillInput(input, '${data.email.escapeJS()}');
                    }
                    
                    // Phone
                    else if (matchesField(input, ['phone', 'telephone', 'mobile'])) {
                        fillInput(input, '${data.phone.escapeJS()}');
                    }
                    
                    // LinkedIn
                    else if (matchesField(input, ['linkedin', 'linked-in'])) {
                        fillInput(input, '${data.linkedinUrl.escapeJS()}');
                    }
                    
                    // Portfolio
                    else if (matchesField(input, ['portfolio', 'website', 'personal site'])) {
                        fillInput(input, '${data.portfolioUrl.escapeJS()}');
                    }
                    
                    // GitHub
                    else if (matchesField(input, ['github', 'git hub'])) {
                        fillInput(input, '${data.githubUrl.escapeJS()}');
                    }
                    
                    // Location/City
                    else if (matchesField(input, ['city', 'location', 'where'])) {
                        fillInput(input, '${data.city.escapeJS()}');
                    }
                    
                    // Current company
                    else if (matchesField(input, ['current company', 'employer', 'organization'])) {
                        fillInput(input, '${data.currentCompany.escapeJS()}');
                    }
                    
                    // Current title
                    else if (matchesField(input, ['current title', 'job title', 'position'])) {
                        fillInput(input, '${data.currentTitle.escapeJS()}');
                    }
                    
                    // Cover letter / Summary
                    else if (matchesField(input, ['cover letter', 'summary', 'about', 'why', 'introduce'])) {
                        if (!input.value) fillInput(input, '${data.summary.escapeJS()}');
                    }
                });
                
                console.log('✅ JSON-LD/Generic autofill complete');
                return 'success';
            })();
        """.trimIndent()
    }

    private fun getGenericAutofillScript(data: AutofillData): String {
        return getJSONLDAutofillScript(data) // Same as JSON-LD
    }

    // Helper to escape JavaScript strings
    private fun String.escapeJS(): String {
        return this
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

enum class JobBoardType {
    GREENHOUSE,
    LEVER,
    JSONLD,
    GENERIC
}