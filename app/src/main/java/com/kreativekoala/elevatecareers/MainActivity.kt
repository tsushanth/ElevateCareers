package com.kreativekoala.elevatecareers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.kreativekoala.elevatecareers.data.LinkedInManager
import com.kreativekoala.elevatecareers.data.OnboardingManager
import com.kreativekoala.elevatecareers.data.SupabaseClientProvider
import com.kreativekoala.elevatecareers.ui.auth.AuthViewModel
import com.kreativekoala.elevatecareers.ui.auth.LinkedInAuthHelper
import com.kreativekoala.elevatecareers.ui.auth.SignInScreen
import com.kreativekoala.elevatecareers.ui.auth.SignUpScreen
import com.kreativekoala.elevatecareers.ui.onboarding.OnboardingScreen
import com.kreativekoala.elevatecareers.ui.profile.ProfileScreen
import com.kreativekoala.elevatecareers.ui.profile.ProfileViewModel
import com.kreativekoala.elevatecareers.ui.settings.SettingsScreen
import com.kreativekoala.elevatecareers.ui.theme.ElevateCareersTheme
import com.kreativekoala.elevatecareers.ui.resume.AiResumeBuilderScreen
import com.kreativekoala.elevatecareers.ui.resume.ResumeOptionsScreen
import com.kreativekoala.elevatecareers.ui.resume.ResumePreviewScreen
import com.kreativekoala.elevatecareers.ui.application.JobApplicationScreen
import com.kreativekoala.elevatecareers.ui.factories.ViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this)
        LinkedInManager.initialize(this)
        handleLinkedInCallback(intent)

        setContent {
            ElevateCareersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ElevateCareersApp(authViewModel = authViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLinkedInCallback(intent)
    }

    private fun handleLinkedInCallback(intent: Intent) {
        val data = intent.data

        if (data != null && data.scheme == "elevatecareers" && data.host == "linkedin-callback") {
            Log.d("MainActivity", "🔵 LinkedIn OAuth callback received")

            val code = LinkedInAuthHelper.extractAuthorizationCode(data)

            if (code != null) {
                Log.d("MainActivity", "✅ LinkedIn code received")

                lifecycleScope.launch {
                    try {
                        val tokenResult = LinkedInAuthHelper.exchangeCodeForAccessToken(code)

                        tokenResult.fold(
                            onSuccess = { accessToken ->
                                val profileResult = LinkedInAuthHelper.fetchUserProfile(accessToken)

                                profileResult.fold(
                                    onSuccess = { profile ->
                                        Log.d("MainActivity", "✅ LinkedIn profile: ${profile.fullName}")

                                        LinkedInManager.saveProfile(this@MainActivity, profile)

                                        if (profile.email != null) {
                                            authViewModel.signInWithLinkedIn(profile)

                                            runOnUiThread {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    getString(R.string.linkedin_connected, profile.fullName),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } else {
                                            showLinkedInError(getString(R.string.linkedin_email_required_error))
                                        }
                                    },
                                    onFailure = { error ->
                                        showLinkedInError(getString(R.string.failed_fetch_profile, error.message ?: ""))
                                    }
                                )
                            },
                            onFailure = { error ->
                                showLinkedInError(getString(R.string.linkedin_login_failed, error.message ?: ""))
                            }
                        )
                    } catch (e: Exception) {
                        showLinkedInError(getString(R.string.error_generic, e.message ?: ""))
                    }
                }
            } else {
                showLinkedInError(getString(R.string.no_authorization_code))
            }
        }
    }

    private fun showLinkedInError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun ElevateCareersApp(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // ⭐ Initialize Supabase client and ViewModelFactory
    val supabaseClient = remember { SupabaseClientProvider.getClient(context) }
    val viewModelFactory = remember { ViewModelFactory(supabaseClient) }

    val profileViewModel: ProfileViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    val hasCompletedOnboarding = remember {
        OnboardingManager.isOnboardingComplete(context)
    }

    val startDestination = if (hasCompletedOnboarding) {
        "job_list"
    } else {
        "sign_in"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ============================================
        // AUTHENTICATION SCREENS
        // ============================================

        composable("sign_in") {
            SignInScreen(
                viewModel = authViewModel,
                onSignInSuccess = {
                    navController.navigate("onboarding") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("sign_up")
                },
                onProceedWithoutSignIn = {
                    navController.navigate("job_list") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                }
            )
        }

        composable("sign_up") {
            SignUpScreen(
                viewModel = authViewModel,
                onSignUpSuccess = {
                    navController.navigate("onboarding") {
                        popUpTo("sign_up") { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ============================================
        // ONBOARDING FLOW
        // ============================================

        composable("onboarding") {
            OnboardingScreen(
                navController = navController,
                profileViewModel = profileViewModel,
                onComplete = {
                    navController.navigate("job_list") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ============================================
        // RESUME BUILDER SCREENS
        // ============================================

        composable("resume-options") {
            ResumeOptionsScreen(
                navController = navController
            )
        }

        composable("ai-resume-builder") {
            AiResumeBuilderScreen(
                navController = navController
            )
        }

        composable(
            route = "resume-preview/{pdfUrl}",
            arguments = listOf(
                navArgument("pdfUrl") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val pdfUrl = backStackEntry.arguments?.getString("pdfUrl")
            ResumePreviewScreen(
                navController = navController,
                pdfUrl = pdfUrl
            )
        }

        composable("upload-resume") {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text(stringResource(R.string.upload_resume_coming_soon))
            }
        }

        composable("manual-resume") {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text(stringResource(R.string.manual_entry_coming_soon))
            }
        }

        // ============================================
        // MAIN APP SCREENS
        // ============================================

        composable("job_list") {
            // ⭐ Use ViewModelFactory to create JobListViewModel with SupabaseClient
            val viewModel: JobListViewModel = viewModel(factory = viewModelFactory)
            JobListScreen(
                viewModel = viewModel,
                onJobClick = { job ->
                    // ⭐ Navigate to in-app application screen instead of browser
                    val jobJson = Json.encodeToString(job)
                    val encodedJob = URLEncoder.encode(jobJson, "UTF-8")
                    navController.navigate("job_application/$encodedJob")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        // ⭐ NEW: In-app job application screen with autofill
        composable(
            route = "job_application/{jobJson}",
            arguments = listOf(
                navArgument("jobJson") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedJob = backStackEntry.arguments?.getString("jobJson")
            val jobJson = URLDecoder.decode(encodedJob, "UTF-8")
            val job = Json.decodeFromString<Job>(jobJson)

            // ⭐ Use ViewModelFactory for JobApplicationViewModel
            val viewModel: com.kreativekoala.elevatecareers.ui.application.JobApplicationViewModel =
                viewModel(factory = viewModelFactory)

            JobApplicationScreen(
                job = job,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "job_detail/{jobJson}",
            arguments = listOf(
                navArgument("jobJson") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedJob = backStackEntry.arguments?.getString("jobJson")
            val jobJson = URLDecoder.decode(encodedJob, "UTF-8")
            val job = Json.decodeFromString<Job>(jobJson)

            JobDetailScreen(
                job = job,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                viewModel = profileViewModel
            )
        }

        // ============================================
        // SETTINGS
        // ============================================

        composable("settings") {
            SettingsScreen(
                authViewModel = authViewModel,
                onSignOut = {
                    navController.navigate("sign_in") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}