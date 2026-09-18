package com.example.ui.preview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ProjectEntity
import com.example.ui.editor.export.ExportHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConsoleMessage(
    val id: Long = System.currentTimeMillis(),
    val type: String, // "info", "warn", "error"
    val message: String,
    val time: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)

enum class ViewportMode(val title: String, val widthDp: Int?) {
    RESPONSIVE("تمام‌صفحه", null),
    MOBILE("موبایل", 375),
    TABLET("تبلت", 768)
}

class AndroidConsoleBridge(private val onNewLog: (type: String, message: String) -> Unit) {
    @JavascriptInterface
    fun onLog(type: String, message: String) {
        onNewLog(type, message)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenPreviewScreen(
    project: ProjectEntity,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onExportZip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedViewport by remember { mutableStateOf(ViewportMode.RESPONSIVE) }
    var showConsoleSheet by remember { mutableStateOf(false) }
    val consoleLogs = remember { mutableStateListOf<ConsoleMessage>() }
    var consoleInput by remember { mutableStateOf("") }

    val errorCount = consoleLogs.count { it.type == "error" }

    BackHandler {
        onBack()
    }

    val bundledHtml = remember(project.htmlCode, project.cssCode, project.jsCode) {
        ExportHelper.bundleToSingleHtml(
            project.htmlCode,
            project.cssCode,
            project.jsCode,
            withConsoleCatcher = true
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "پیش‌نمایش: ${project.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = selectedViewport.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت به کد"
                        )
                    }
                },
                actions = {
                    // Viewport switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedViewport = ViewportMode.RESPONSIVE },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = "نمای کامل",
                                tint = if (selectedViewport == ViewportMode.RESPONSIVE)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { selectedViewport = ViewportMode.MOBILE },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = "نمای موبایل",
                                tint = if (selectedViewport == ViewportMode.MOBILE)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { selectedViewport = ViewportMode.TABLET },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tablet,
                                contentDescription = "نمای تبلت",
                                tint = if (selectedViewport == ViewportMode.TABLET)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Console button with badge
                    IconButton(
                        onClick = { showConsoleSheet = true },
                        modifier = Modifier.testTag("open_console_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (errorCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("$errorCount")
                                    }
                                } else if (consoleLogs.isNotEmpty()) {
                                    Badge {
                                        Text("${consoleLogs.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "کنسول توسعه‌دهنده",
                                tint = if (errorCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Refresh
                    IconButton(
                        onClick = {
                            consoleLogs.clear()
                            webViewInstance?.loadDataWithBaseURL(
                                "https://appassets.androidplatform.net/",
                                bundledHtml,
                                "text/html",
                                "utf-8",
                                null
                            )
                        },
                        modifier = Modifier.testTag("preview_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تازه‌سازی پیش‌نمایش"
                        )
                    }

                    // Open in Chrome / External Browser
                    IconButton(
                        onClick = {
                            ExportHelper.openInBrowser(context, project)
                        },
                        modifier = Modifier.testTag("preview_open_chrome_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "باز کردن در کروم",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Export Zip
                    IconButton(
                        onClick = onExportZip,
                        modifier = Modifier.testTag("preview_export_zip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = "خروجی زیپ"
                        )
                    }

                    // Share
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("preview_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "اشتراک‌گذاری"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            val viewportModifier = when (selectedViewport) {
                ViewportMode.RESPONSIVE -> Modifier.fillMaxSize()
                ViewportMode.MOBILE -> Modifier
                    .width(375.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                ViewportMode.TABLET -> Modifier
                    .width(768.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            }

            Box(modifier = viewportModifier.background(Color.White)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            @SuppressLint("SetJavaScriptEnabled")
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.allowFileAccess = true

                            addJavascriptInterface(
                                AndroidConsoleBridge { type, msg ->
                                    post {
                                        consoleLogs.add(ConsoleMessage(type = type, message = msg))
                                    }
                                },
                                "AndroidConsole"
                            )

                            webChromeClient = object : WebChromeClient() {
                                override fun onJsAlert(
                                    view: WebView?,
                                    url: String?,
                                    message: String?,
                                    result: JsResult?
                                ): Boolean {
                                    consoleLogs.add(
                                        ConsoleMessage(type = "info", message = "[Alert]: $message")
                                    )
                                    result?.confirm()
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    isLoading = false
                                    consoleLogs.add(
                                        ConsoleMessage(
                                            type = "error",
                                            message = "خطای بارگذاری: ${error?.description}"
                                        )
                                    )
                                }
                            }

                            loadDataWithBaseURL(
                                "https://appassets.androidplatform.net/",
                                bundledHtml,
                                "text/html",
                                "utf-8",
                                null
                            )
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    }
                )
            }
        }
    }

    // Developer Console BottomSheet
    if (showConsoleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConsoleSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E1E2E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "کنسول توسعه‌دهنده (Console)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { consoleLogs.clear() },
                            modifier = Modifier.testTag("clear_console_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "پاک کردن لاگ‌ها",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Logs list
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF11111B))
                        .padding(8.dp)
                ) {
                    if (consoleLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "هیچ لاگ یا خطایی ثبت نشده است.",
                                color = Color(0xFF6C7086),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(consoleLogs) { log ->
                                val (color, icon) = when (log.type) {
                                    "error" -> Color(0xFFF38BA8) to "✕"
                                    "warn" -> Color(0xFFFAB387) to "⚠"
                                    else -> Color(0xFFA6ADC8) to "ℹ"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${log.time} $icon",
                                        color = color,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = log.message,
                                        color = color,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // JS Evaluate Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = consoleInput,
                        onValueChange = { consoleInput = it },
                        placeholder = {
                            Text(
                                "دستور JS بنویسید (مثلاً: 2+2 یا document.title)",
                                color = Color(0xFF6C7086),
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("console_js_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF313244)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (consoleInput.isNotBlank()) {
                                val script = consoleInput
                                consoleLogs.add(ConsoleMessage(type = "info", message = "> $script"))
                                webViewInstance?.evaluateJavascript(script) { result ->
                                    consoleLogs.add(
                                        ConsoleMessage(
                                            type = "info",
                                            message = "< $result"
                                        )
                                    )
                                }
                                consoleInput = ""
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                            .testTag("console_js_eval_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "اجرا",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}
