package com.example.ui.editor

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.ui.editor.sheets.AiAssistantSheet
import com.example.ui.editor.sheets.CdnLibrariesSheet
import com.example.ui.editor.sheets.ExportShareSheet
import com.example.ui.editor.sheets.ThemeSelectorSheet
import com.example.ui.editor.syntax.CodeLanguage
import com.example.ui.editor.syntax.CodeSyntaxVisualTransformation
import com.example.ui.intro.IntroScreen
import com.example.ui.preview.FullscreenPreviewScreen
import com.example.ui.project.NewProjectDialog
import com.example.ui.project.ProjectListSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    projects: List<ProjectEntity>,
    uiState: EditorUiState
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Beautiful Intro Screen with RITM Watermark
    if (uiState.showIntro) {
        IntroScreen(
            onContinue = { viewModel.dismissIntro() }
        )
        return
    }

    val currentProject = uiState.currentProject

    // Fullscreen Preview Screen
    if (uiState.isFullscreenPreview && currentProject != null) {
        FullscreenPreviewScreen(
            project = currentProject,
            onBack = { viewModel.setFullscreenPreview(false) },
            onShare = { viewModel.setShowExportSheet(true) },
            onExportZip = { viewModel.exportAsZip(context) }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRenameDialog = true }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentProject?.title ?: "پروژه جدید",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "ویرایش نام",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            // Auto-save Status indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (statusText, statusColor) = when (uiState.saveStatus) {
                                    SaveStatus.SAVED -> "ذخیره شد" to Color(0xFF10B981)
                                    SaveStatus.SAVING -> "ذخیره..." to Color(0xFFF59E0B)
                                    SaveStatus.UNSAVED -> "تغییرات ذخیره‌نشده" to Color(0xFF94A3B8)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    color = statusColor
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setShowProjectsSheet(true) },
                        modifier = Modifier.testTag("open_projects_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "پروژه‌ها"
                        )
                    }
                },
                actions = {
                    // AI Assistant Action Button (Distinctive Highlight)
                    Button(
                        onClick = { viewModel.setShowAiSheet(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("open_ai_assistant_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "هوش مصنوعی",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Play / Preview Button
                    IconButton(
                        onClick = { viewModel.setFullscreenPreview(true) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .size(36.dp)
                            .testTag("top_preview_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "اجرای پیش‌نمایش",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Overflow Menu to avoid cluttering the toolbar
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.testTag("editor_more_options_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "گزینه‌های بیشتر"
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("ذخیره فوری") },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.saveImmediately()
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("انتخاب تم کدنویسی") },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.setShowThemeSheet(true)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.ColorLens, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("کتابخانه‌های خارجی (CDN)") },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.setShowLibrariesSheet(true)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Extension, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("پیش‌نمایش در مرورگر کروم") },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.openInBrowser(context)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("خروجی زیپ و اشتراک") },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.setShowExportSheet(true)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (uiState.isSyntaxHighlightingEnabled) "غیرفعال‌سازی رنگ‌آمیزی کد" else "فعال‌سازی رنگ‌آمیزی کد")
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.toggleSyntaxHighlighting()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = if (uiState.isSyntaxHighlightingEnabled) Color(0xFFF59E0B) else Color.Gray
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (uiState.showQuickSymbolsBar) "پنهان‌سازی نوار نمادها" else "نمایش نوار نمادها")
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.toggleQuickSymbolsBar()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("درباره RITM و اینترو") },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.showIntroScreen()
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(uiState.editorTheme.background)
        ) {
            // Clean Tabs Bar (HTML, CSS, JS) + Editor Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(uiState.editorTheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple(CodeLanguage.HTML, "index.html", "HTML"),
                    Triple(CodeLanguage.CSS, "style.css", "CSS"),
                    Triple(CodeLanguage.JS, "script.js", "JS")
                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEach { (lang, filename, badge) ->
                        val isSelected = uiState.currentTab == lang
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) uiState.editorTheme.background else Color.Transparent
                                )
                                .clickable { viewModel.setTab(lang) }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("tab_${lang.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val badgeColor = when (lang) {
                                    CodeLanguage.HTML -> Color(0xFFE44D26)
                                    CodeLanguage.CSS -> Color(0xFF264DE4)
                                    CodeLanguage.JS -> Color(0xFFF7DF1E)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(badgeColor)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = badge,
                                        color = if (lang == CodeLanguage.JS) Color.Black else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = filename,
                                    color = if (isSelected) uiState.editorTheme.textColor else uiState.editorTheme.lineNumbersText,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Compact font size and line-wrap buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "A-",
                        color = uiState.editorTheme.textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.setFontSize(uiState.fontSizeSp - 1) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "${uiState.fontSizeSp}",
                        color = uiState.editorTheme.lineNumbersText,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Text(
                        text = "A+",
                        color = uiState.editorTheme.textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.setFontSize(uiState.fontSizeSp + 1) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    IconButton(
                        onClick = { viewModel.toggleWordWrap() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WrapText,
                            contentDescription = "شکستن خطوط",
                            tint = if (uiState.isWordWrap) uiState.editorTheme.cursorColor else uiState.editorTheme.lineNumbersText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Code Editor Area with Line Numbers
            val currentCode = when (uiState.currentTab) {
                CodeLanguage.HTML -> currentProject?.htmlCode ?: ""
                CodeLanguage.CSS -> currentProject?.cssCode ?: ""
                CodeLanguage.JS -> currentProject?.jsCode ?: ""
            }

            val linesCount = remember(currentCode) {
                var count = 1
                for (i in 0 until currentCode.length) {
                    if (currentCode[i] == '\n') count++
                }
                count
            }

            val isLargeDocument = remember(linesCount, currentCode.length) {
                linesCount > 500 || currentCode.length > 15000
            }

            val lineNumbersText = remember(linesCount) {
                // Prevent Skia GPU texture overflow (> 8192px height) on large documents
                val maxLinesToRender = linesCount.coerceAtMost(500)
                val sb = java.lang.StringBuilder(maxLinesToRender * 5)
                for (i in 1..maxLinesToRender) {
                    sb.append(i).append('\n')
                }
                if (linesCount > 500) {
                    sb.append("+\n")
                }
                sb.toString()
            }

            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Line numbers gutter
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(uiState.editorTheme.lineNumbersBg)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .verticalScroll(verticalScrollState)
                    ) {
                        Text(
                            text = lineNumbersText,
                            color = uiState.editorTheme.lineNumbersText,
                            fontSize = uiState.fontSizeSp.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = (uiState.fontSizeSp * 1.45).sp,
                            textAlign = TextAlign.End
                        )
                    }

                    // Gutter Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(uiState.editorTheme.lineNumbersText.copy(alpha = 0.2f))
                    )

                    // Text editor field
                    val modifierForField = if (uiState.isWordWrap) {
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .verticalScroll(verticalScrollState)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                    }

                    BasicTextField(
                        value = currentCode,
                        onValueChange = { viewModel.onCodeChanged(it) },
                        modifier = modifierForField.testTag("code_editor_field"),
                        textStyle = TextStyle(
                            color = uiState.editorTheme.textColor,
                            fontSize = uiState.fontSizeSp.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = (uiState.fontSizeSp * 1.45).sp
                        ),
                        cursorBrush = SolidColor(uiState.editorTheme.cursorColor),
                        visualTransformation = if (isLargeDocument) {
                            VisualTransformation.None
                        } else {
                            CodeSyntaxVisualTransformation(
                                language = uiState.currentTab,
                                theme = uiState.editorTheme,
                                isSyntaxHighlightingEnabled = uiState.isSyntaxHighlightingEnabled
                            )
                        }
                    )
                }
            }

            // Quick Developer Symbols Toolbar (Optional & Collapsible)
            if (uiState.showQuickSymbolsBar) {
                QuickSnippetToolbar(
                    language = uiState.currentTab,
                    theme = uiState.editorTheme,
                    onInsert = { snippet -> viewModel.insertSnippet(snippet) }
                )
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog && currentProject != null) {
        var newTitle by remember { mutableStateOf(currentProject.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("تغییر نام پروژه") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("نام پروژه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_project_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.updateProjectTitle(newTitle.trim())
                        }
                        showRenameDialog = false
                    },
                    modifier = Modifier.testTag("confirm_rename_btn")
                ) {
                    Text("تأیید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Projects Sheet
    if (uiState.showProjectsSheet) {
        ProjectListSheet(
            projects = projects,
            currentProjectId = currentProject?.id ?: -1,
            onSelectProject = { id -> viewModel.selectProject(id) },
            onDeleteProject = { id -> viewModel.deleteProject(id) },
            onDuplicateProject = { prj -> viewModel.duplicateProject(prj) },
            onToggleFavorite = { prj -> viewModel.toggleFavorite(prj) },
            onOpenNewProjectDialog = {
                viewModel.setShowProjectsSheet(false)
                viewModel.setShowNewProjectDialog(true)
            },
            onDismiss = { viewModel.setShowProjectsSheet(false) }
        )
    }

    // New Project Dialog
    if (uiState.showNewProjectDialog) {
        NewProjectDialog(
            onConfirm = { title, template ->
                viewModel.createNewProject(title, template)
            },
            onDismiss = { viewModel.setShowNewProjectDialog(false) }
        )
    }

    // Theme Selector Sheet
    if (uiState.showThemeSheet) {
        ThemeSelectorSheet(
            currentTheme = uiState.editorTheme,
            onSelectTheme = { viewModel.setTheme(it) },
            onDismiss = { viewModel.setShowThemeSheet(false) }
        )
    }

    // CDN Libraries Sheet
    if (uiState.showLibrariesSheet) {
        CdnLibrariesSheet(
            onInjectSnippet = { snippet, name ->
                viewModel.injectLibrary(snippet, name)
            },
            onDismiss = { viewModel.setShowLibrariesSheet(false) }
        )
    }

    // Export & Share Sheet
    if (uiState.showExportSheet && currentProject != null) {
        ExportShareSheet(
            projectTitle = currentProject.title,
            onOpenInChrome = { viewModel.openInBrowser(context) },
            onExportZip = { viewModel.exportAsZip(context) },
            onExportHtml = { viewModel.exportAsHtml(context) },
            onCopyDataUri = { viewModel.copyDirectPreviewLink(context) },
            onShareText = { viewModel.shareCodeText(context) },
            onDismiss = { viewModel.setShowExportSheet(false) }
        )
    }

    // AI Coding Assistant Sheet
    if (uiState.showAiSheet) {
        AiAssistantSheet(
            activeProvider = uiState.aiProvider,
            currentApiKey = uiState.aiApiKey,
            currentModel = uiState.aiModel,
            isGenerating = uiState.isAiGenerating,
            lastAiResult = uiState.lastAiResult,
            errorMessage = uiState.aiErrorMessage,
            onSelectProvider = { viewModel.selectAiProvider(it) },
            onSaveApiKey = { provider, key -> viewModel.saveAiApiKeyForProvider(provider, key) },
            onSelectModel = { provider, model -> viewModel.selectAiModelForProvider(provider, model) },
            onExecuteAiAction = { action, prompt -> viewModel.executeAiAction(action, prompt) },
            onApplyCodeToProject = { html, css, js -> viewModel.applyAiCode(html, css, js) },
            onDismiss = { viewModel.setShowAiSheet(false) }
        )
    }
}

@Composable
fun QuickSnippetToolbar(
    language: CodeLanguage,
    theme: com.example.ui.editor.theme.EditorThemeType,
    onInsert: (String) -> Unit
) {
    val snippets = remember(language) {
        when (language) {
            CodeLanguage.HTML -> listOf(
                "  " to "Tab",
                "<>" to "<>",
                "</>" to "</>",
                "<div>\n  \n</div>" to "<div>",
                "<p></p>" to "<p>",
                "<span></span>" to "<span>",
                "<a href=\"\"></a>" to "<a>",
                "<button></button>" to "<btn>",
                "<script>\n  \n</script>" to "<script>",
                "<style>\n  \n</style>" to "<style>",
                " class=\"\"" to "class",
                " id=\"\"" to "id",
                "\"\"" to "\"\"",
                "''" to "''",
                "=" to "=",
                "<!--  -->" to "<!-- -->"
            )
            CodeLanguage.CSS -> listOf(
                "  " to "Tab",
                "{\n  \n}" to "{ }",
                ":" to ":",
                ";" to ";",
                "#" to "#",
                "." to ".",
                "px" to "px",
                "rem" to "rem",
                "%" to "%",
                "color: ;" to "color",
                "background: ;" to "bg",
                "display: flex;" to "flex",
                "margin: 0;" to "margin",
                "padding: 0;" to "padding",
                "/*  */" to "/* */"
            )
            CodeLanguage.JS -> listOf(
                "  " to "Tab",
                "()" to "( )",
                "{\n  \n}" to "{ }",
                "[]" to "[ ]",
                " => " to "=>",
                "const " to "const",
                "let " to "let",
                "function () {\n  \n}" to "func",
                "console.log();" to "log",
                "document." to "doc",
                "addEventListener('', () => {\n  \n});" to "event",
                " === " to "===",
                " !== " to "!==",
                " && " to "&&",
                " || " to "||",
                ";" to ";",
                "\"\"" to "\"\"",
                "``" to "``",
                "// " to "//"
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        snippets.forEach { (code, label) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(theme.background)
                    .clickable { onInsert(code) }
                    .border(1.dp, theme.lineNumbersText.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("snippet_${label.filter { it.isLetterOrDigit() }.ifBlank { "char" }}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = theme.textColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
