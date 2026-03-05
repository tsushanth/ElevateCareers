package com.kreativekoala.elevatecareers.ui.onboarding

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kreativekoala.elevatecareers.data.LinkedInManager
import com.kreativekoala.elevatecareers.data.OnboardingManager
import com.kreativekoala.elevatecareers.ui.profile.ProfileViewModel
import com.kreativekoala.elevatecareers.ui.auth.LinkedInAuthHelper
import com.kreativekoala.elevatecareers.ui.components.CustomFilePicker
import java.io.InputStream

@Composable
fun OnboardingScreen(
    navController: NavController, // ⭐ ADD THIS
    profileViewModel: ProfileViewModel = viewModel(),
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(0) }
    var resumeFileName by remember { mutableStateOf<String?>(null) }
    var isUploadingResume by remember { mutableStateOf(false) }
    var showCustomFilePicker by remember { mutableStateOf(false) }

    // Use LinkedInManager state
    val isLinkedInConnected by LinkedInManager.isLinkedInConnected.collectAsState()
    val linkedInProfile by LinkedInManager.linkedInProfile.collectAsState()

    val scrollState = rememberScrollState()

    // LinkedIn OAuth launcher
    val linkedInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Toast.makeText(
            context,
            "Complete LinkedIn login in your browser",
            Toast.LENGTH_SHORT
        ).show()
    }

    val systemFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                isUploadingResume = true
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val fileData = inputStream?.readBytes()
                val fileName = uri.lastPathSegment ?: "resume.pdf"

                fileData?.let { data ->
                    profileViewModel.uploadResume(data, fileName)
                    resumeFileName = fileName

                    Toast.makeText(
                        context,
                        "✅ Resume uploaded successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "❌ Failed to upload: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isUploadingResume = false
            }
        } else {
            Toast.makeText(
                context,
                "No file selected",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (currentStep + 1) / 3f },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (currentStep) {
            0 -> WelcomeStep(
                onNext = {
                    currentStep = if (isLinkedInConnected) 2 else 1
                },
                onSkip = {
                    OnboardingManager.markOnboardingComplete(context)
                    onComplete()
                }
            )
            1 -> LinkedInStep(
                isLinked = isLinkedInConnected,
                profileName = linkedInProfile?.fullName,
                onLinkLinkedIn = {
                    val intent = LinkedInAuthHelper.createAuthIntent(context)
                    linkedInLauncher.launch(intent)
                },
                onNext = { currentStep = 2 },
                onSkip = { currentStep = 2 }
            )
            2 -> ResumeStep(
                navController = navController, // ⭐ PASS navController
                fileName = resumeFileName,
                isUploading = isUploadingResume,
                onUploadResume = {
                    systemFilePickerLauncher.launch("application/pdf")
                },
                onUploadFromDevice = {
                    showCustomFilePicker = true
                },
                onComplete = {
                    OnboardingManager.markOnboardingComplete(context)
                    onComplete()
                },
                onSkip = {
                    OnboardingManager.markOnboardingComplete(context)
                    onComplete()
                }
            )
        }
    }

    if (showCustomFilePicker) {
        CustomFilePicker(
            onFileSelected = { file ->
                isUploadingResume = true

                try {
                    val fileData = file.readBytes()
                    profileViewModel.uploadResume(fileData, file.name)
                    resumeFileName = file.name

                    Toast.makeText(
                        context,
                        "✅ Resume uploaded: ${file.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                    showCustomFilePicker = false
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "❌ Failed to upload: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    isUploadingResume = false
                }
            },
            onDismiss = {
                showCustomFilePicker = false
                Toast.makeText(
                    context,
                    "File selection cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun WelcomeStep(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Work,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Welcome to ElevateCareers",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Let's get you set up to find your dream job",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }

        TextButton(onClick = onSkip) {
            Text("Skip Setup")
        }
    }
}

@Composable
fun LinkedInStep(
    isLinked: Boolean,
    profileName: String?,
    onLinkLinkedIn: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Connect LinkedIn",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "We'll auto-fill your profile with your LinkedIn information",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLinked && profileName != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Connected as $profileName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        } else {
            Button(
                onClick = onLinkLinkedIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Link, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect LinkedIn")
            }

            TextButton(onClick = onSkip) {
                Text("Skip for now")
            }
        }
    }
}

@Composable
fun ResumeStep(
    navController: NavController, // ⭐ ADD THIS
    fileName: String?,
    isUploading: Boolean,
    onUploadResume: () -> Unit,
    onUploadFromDevice: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var showPickerChoice by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Upload Your Resume",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "We'll automatically extract your skills and experience",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (fileName != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Resume uploaded!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showPickerChoice = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change resume",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Complete Setup")
            }

            TextButton(onClick = onComplete) {
                Text("Continue with this resume")
            }
        } else {
            Button(
                onClick = { showPickerChoice = true },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Resume")
                }
            }

            TextButton(onClick = onSkip) {
                Text("Skip for now")
            }

            Text(
                text = "You can always upload your resume later from Settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    // ⭐ UPDATED: Choice dialog with AI Voice option
    if (showPickerChoice) {
        AlertDialog(
            onDismissRequest = { showPickerChoice = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Create or Upload Resume") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How would you like to add your resume?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // ⭐ OPTION 1: AI Voice Builder (NEW - Recommended)
                    Card(
                        onClick = {
                            showPickerChoice = false
                            navController.navigate("ai-resume-builder")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Build with AI Voice",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "NEW",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = "Chat with AI to create your resume",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // OPTION 2: Google Drive / Cloud
                    Card(
                        onClick = {
                            showPickerChoice = false
                            onUploadResume()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Upload from Cloud",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Google Drive, Dropbox, etc.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // OPTION 3: Device Storage
                    Card(
                        onClick = {
                            showPickerChoice = false
                            onUploadFromDevice()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Device Storage",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Browse local files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPickerChoice = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}