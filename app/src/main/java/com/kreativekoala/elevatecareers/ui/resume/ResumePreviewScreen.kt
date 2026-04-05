package com.kreativekoala.elevatecareers.ui.resume

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kreativekoala.elevatecareers.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kreativekoala.elevatecareers.data.model.ResumeData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumePreviewScreen(
    navController: NavController,
    pdfUrl: String?,
    viewModel: ResumeBuilderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.resume_preview)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    // Share button
                    IconButton(
                        onClick = {
                            pdfUrl?.let { url ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, url)
                                }
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_resume)))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, stringResource(R.string.share))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Download PDF button
                    Button(
                        onClick = {
                            pdfUrl?.let { url ->
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(url)
                                    type = "application/pdf"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.download_pdf))
                    }

                    // Edit button
                    OutlinedButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.edit))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Personal Info
            item {
                ResumeSection(title = stringResource(R.string.personal_information)) {
                    PersonalInfoPreview(state.resumeData)
                }
            }

            // Education
            if (state.resumeData.education.isNotEmpty()) {
                item {
                    ResumeSection(title = stringResource(R.string.education)) {
                        state.resumeData.education.forEach { edu ->
                            EducationItemPreview(edu)
                        }
                    }
                }
            }

            // Experience
            if (state.resumeData.experience.isNotEmpty()) {
                item {
                    ResumeSection(title = stringResource(R.string.experience_label)) {
                        state.resumeData.experience.forEach { exp ->
                            ExperienceItemPreview(exp)
                        }
                    }
                }
            }

            // Skills
            if (state.resumeData.skills.isNotEmpty()) {
                item {
                    ResumeSection(title = stringResource(R.string.skills)) {
                        SkillsPreview(state.resumeData.skills)
                    }
                }
            }
        }
    }
}

@Composable
fun ResumeSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Divider()
            content()
        }
    }
}

@Composable
fun PersonalInfoPreview(resumeData: ResumeData) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (resumeData.personalInfo.name.isNotEmpty()) {
            InfoRow(icon = Icons.Default.Person, text = resumeData.personalInfo.name)
        }
        if (resumeData.personalInfo.email.isNotEmpty()) {
            InfoRow(icon = Icons.Default.Email, text = resumeData.personalInfo.email)
        }
        if (resumeData.personalInfo.phone.isNotEmpty()) {
            InfoRow(icon = Icons.Default.Phone, text = resumeData.personalInfo.phone)
        }
        if (resumeData.personalInfo.location.isNotEmpty()) {
            InfoRow(icon = Icons.Default.LocationOn, text = resumeData.personalInfo.location)
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun EducationItemPreview(education: com.kreativekoala.elevatecareers.data.model.Education) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = education.school,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${education.degree} in ${education.field}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (education.graduationYear.isNotEmpty()) {
            Text(
                text = stringResource(R.string.graduated, education.graduationYear),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExperienceItemPreview(experience: com.kreativekoala.elevatecareers.data.model.Experience) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = experience.position,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${experience.company} • ${experience.duration}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (experience.responsibilities.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            experience.responsibilities.forEach { responsibility ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("•", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = responsibility,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun SkillsPreview(skills: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEach { skill ->
            SuggestionChip(
                onClick = {},
                label = { Text(skill) }
            )
        }
    }
}

// FlowRow implementation (or use accompanist-flowlayout)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simple implementation - you might want to use a library for better flow layout
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}