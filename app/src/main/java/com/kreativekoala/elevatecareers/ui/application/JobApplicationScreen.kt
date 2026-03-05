package com.kreativekoala.elevatecareers.ui.application

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kreativekoala.elevatecareers.Job
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicationScreen(
    job: Job,
    onBackClick: () -> Unit,
    viewModel: JobApplicationViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var webView: WebView? by remember { mutableStateOf(null) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var pageTitle by remember { mutableStateOf("") }
    var showVoiceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAutofillData()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = pageTitle.ifEmpty { job.companyName },
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    }
                )

                // Loading progress bar
                if (loadingProgress < 100) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        floatingActionButton = {
            // Voice input button - shows when there are unfilled fields
            if (uiState.unfilledFields.isNotEmpty() && !uiState.isAutoFilling) {
                ExtendedFloatingActionButton(
                    onClick = { showVoiceDialog = true },
                    icon = { Icon(androidx.compose.material.icons.Icons.Default.Mic, "Voice") },
                    text = { Text("Fill with Voice (${uiState.unfilledFields.size})") }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load autofill data",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAutofillData() }) {
                            Text("Retry")
                        }
                    }
                }

                uiState.autofillData != null -> {
                    JobApplicationWebView(
                        url = job.applyUrl,
                        autofillData = uiState.autofillData!!,
                        onWebViewCreated = { webView = it },
                        onProgressChanged = { loadingProgress = it },
                        onTitleChanged = { pageTitle = it },
                        onPageFinished = { view ->
                            // Auto-fill the form when page loads
                            viewModel.injectAutofill(view, job.applyUrl)
                        },
                        onUnfilledFieldsDetected = { fields ->
                            viewModel.setUnfilledFields(fields)
                        }
                    )
                }
            }

            // Autofill status overlay
            if (uiState.isAutoFilling) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Auto-filling application...")
                    }
                }
            }
        }
    }

    // Voice input dialog
    if (showVoiceDialog && uiState.autofillData != null) {
        VoiceInputDialog(
            unfilledFields = uiState.unfilledFields,
            onFieldFilled = { fieldName, value ->
                webView?.let { view ->
                    viewModel.fillField(view, fieldName, value)
                }
            },
            onDismiss = { showVoiceDialog = false }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JobApplicationWebView(
    url: String,
    autofillData: com.kreativekoala.elevatecareers.data.AutofillData,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onTitleChanged: (String) -> Unit,
    onPageFinished: (WebView) -> Unit,
    onUnfilledFieldsDetected: (List<String>) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.let { onPageFinished(it) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false // Let WebView handle navigation
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        title?.let { onTitleChanged(it) }
                    }
                }

                onWebViewCreated(this)
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}