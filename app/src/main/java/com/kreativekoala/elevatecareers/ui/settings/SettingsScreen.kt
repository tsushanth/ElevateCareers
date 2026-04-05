package com.kreativekoala.elevatecareers.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kreativekoala.elevatecareers.R
import com.kreativekoala.elevatecareers.data.LinkedInManager
import com.kreativekoala.elevatecareers.data.OnboardingManager
import com.kreativekoala.elevatecareers.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSignOut: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isLinkedInConnected by LinkedInManager.isLinkedInConnected.collectAsState()
    val linkedInProfile by LinkedInManager.linkedInProfile.collectAsState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Account Section
            SettingsSectionHeader(stringResource(R.string.account))

            if (!uiState.isAnonymous) {
                uiState.userEmail?.let { email ->
                    SettingsItem(
                        icon = Icons.Default.Email,
                        title = stringResource(R.string.email),
                        subtitle = email,
                        onClick = { }
                    )
                    HorizontalDivider()
                }
            }

            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.edit_profile),
                onClick = { /* Navigate to edit profile */ }
            )

            HorizontalDivider()

            // App Section
            SettingsSectionHeader(stringResource(R.string.app_section))

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.notifications),
                subtitle = stringResource(R.string.manage_notification_preferences),
                onClick = { /* Navigate to notifications */ }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Security,
                title = stringResource(R.string.privacy_security),
                subtitle = stringResource(R.string.control_data_privacy),
                onClick = { /* Navigate to privacy */ }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about),
                subtitle = stringResource(R.string.version_info),
                onClick = { /* Show about dialog */ }
            )

            HorizontalDivider()

            // Danger Zone
            SettingsSectionHeader(stringResource(R.string.account_actions))

            SettingsItem(
                icon = Icons.Default.Logout,
                title = stringResource(R.string.sign_out),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showSignOutDialog = true }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.delete_account),
                subtitle = stringResource(R.string.permanently_delete_account),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showDeleteAccountDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (isLinkedInConnected && linkedInProfile != null) {
            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.linkedin_profile),
                subtitle = linkedInProfile?.fullName ?: stringResource(R.string.connected),
                onClick = { }
            )
            HorizontalDivider()
        }

        // Sign Out Dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null
                    )
                },
                title = { Text(stringResource(R.string.sign_out)) },
                text = { Text(stringResource(R.string.sign_out_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Sign out from all systems
                            authViewModel.signOut()
                            LinkedInManager.disconnect(context)
                            OnboardingManager.resetOnboarding(context)
                            showSignOutDialog = false
                            onSignOut()
                        }
                    ) {
                        Text(stringResource(R.string.sign_out))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Delete Account Dialog
        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text(stringResource(R.string.delete_account)) },
                text = {
                    Column {
                        Text(stringResource(R.string.delete_account_confirmation))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.delete_account_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            authViewModel.deleteAccount()
                            showDeleteAccountDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == MaterialTheme.colorScheme.error) {
                titleColor
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}