package com.example.ui.editor

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiActionType
import com.example.data.ai.AiApiKeyManager
import com.example.data.ai.AiProvider
import com.example.data.ai.ExtractedAiCode
import com.example.data.ai.MultiProviderAiService
import com.example.data.local.AppDatabase
import com.example.data.model.ProjectEntity
import com.example.data.repository.ProjectRepository
import com.example.ui.editor.export.ExportHelper
import com.example.ui.editor.syntax.CodeLanguage
import com.example.ui.editor.theme.EditorThemeType
import com.example.ui.project.TemplateInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

enum class SaveStatus {
    SAVED,
    SAVING,
    UNSAVED
}

data class EditorUiState(
    val showIntro: Boolean = true,
    val currentProject: ProjectEntity? = null,
    val currentTab: CodeLanguage = CodeLanguage.HTML,
    val editorTheme: EditorThemeType = EditorThemeType.VS_CODE_DARK,
    val fontSizeSp: Int = 14,
    val isWordWrap: Boolean = true,
    val isSyntaxHighlightingEnabled: Boolean = true,
    val showQuickSymbolsBar: Boolean = true,
    val saveStatus: SaveStatus = SaveStatus.SAVED,
    val isFullscreenPreview: Boolean = false,
    val showProjectsSheet: Boolean = false,
    val showNewProjectDialog: Boolean = false,
    val showThemeSheet: Boolean = false,
    val showLibrariesSheet: Boolean = false,
    val showExportSheet: Boolean = false,
    val showAiSheet: Boolean = false,
    val aiProvider: AiProvider = AiProvider.DEEPSEEK,
    val aiApiKey: String = "",
    val aiModel: String = AiProvider.DEEPSEEK.defaultModel,
    val isAiGenerating: Boolean = false,
    val lastAiResult: ExtractedAiCode? = null,
    val aiErrorMessage: String? = null,
    val snackbarMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    private val aiApiKeyManager = AiApiKeyManager(application)
    private val multiAiService = MultiProviderAiService()

    val allProjects: StateFlow<List<ProjectEntity>>

    private val _uiState = MutableStateFlow(
        EditorUiState(
            aiProvider = aiApiKeyManager.getActiveProvider(),
            aiApiKey = aiApiKeyManager.getApiKey(aiApiKeyManager.getActiveProvider()),
            aiModel = aiApiKeyManager.getSelectedModel(aiApiKeyManager.getActiveProvider())
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ProjectRepository(db.projectDao())

        allProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Launch separately to avoid blocking collection
        viewModelScope.launch {
            try {
                repository.ensureDefaultProjectsExist()
            } catch (_: Throwable) {
                // Safeguard against SQLite lock or race conditions
            }
        }

        viewModelScope.launch {
            allProjects.collect { projects ->
                if (projects.isNotEmpty() && _uiState.value.currentProject == null) {
                    _uiState.value = _uiState.value.copy(currentProject = projects.first())
                }
            }
        }
    }

    fun dismissIntro() {
        _uiState.value = _uiState.value.copy(showIntro = false)
    }

    fun showIntroScreen() {
        _uiState.value = _uiState.value.copy(showIntro = true)
    }

    fun selectProject(projectId: Long) {
        viewModelScope.launch {
            try {
                val project = repository.getProjectDirect(projectId)
                if (project != null) {
                    _uiState.value = _uiState.value.copy(
                        currentProject = project,
                        saveStatus = SaveStatus.SAVED
                    )
                }
            } catch (e: Exception) {
                showSnackbar("خطا در بارگذاری پروژه: ${e.message}")
            }
        }
    }

    fun setTab(language: CodeLanguage) {
        _uiState.value = _uiState.value.copy(currentTab = language)
    }

    fun setTheme(theme: EditorThemeType) {
        _uiState.value = _uiState.value.copy(editorTheme = theme)
    }

    fun setFontSize(sizeSp: Int) {
        _uiState.value = _uiState.value.copy(fontSizeSp = sizeSp.coerceIn(11, 24))
    }

    fun toggleWordWrap() {
        _uiState.value = _uiState.value.copy(isWordWrap = !_uiState.value.isWordWrap)
    }

    fun toggleSyntaxHighlighting() {
        val newState = !_uiState.value.isSyntaxHighlightingEnabled
        _uiState.value = _uiState.value.copy(isSyntaxHighlightingEnabled = newState)
        showSnackbar(if (newState) "رنگ‌آمیزی کد فعال شد." else "رنگ‌آمیزی کد برای افزایش سرعت غیرفعال شد.")
    }

    fun toggleQuickSymbolsBar() {
        _uiState.value = _uiState.value.copy(showQuickSymbolsBar = !_uiState.value.showQuickSymbolsBar)
    }

    fun setFullscreenPreview(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isFullscreenPreview = visible)
    }

    fun setShowProjectsSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showProjectsSheet = show)
    }

    fun setShowNewProjectDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNewProjectDialog = show)
    }

    fun setShowThemeSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showThemeSheet = show)
    }

    fun setShowLibrariesSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLibrariesSheet = show)
    }

    fun setShowExportSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExportSheet = show)
    }

    fun setShowAiSheet(show: Boolean) {
        val provider = aiApiKeyManager.getActiveProvider()
        _uiState.value = _uiState.value.copy(
            showAiSheet = show,
            aiProvider = provider,
            aiApiKey = aiApiKeyManager.getApiKey(provider),
            aiModel = aiApiKeyManager.getSelectedModel(provider),
            aiErrorMessage = null
        )
    }

    fun selectAiProvider(provider: AiProvider) {
        aiApiKeyManager.setActiveProvider(provider)
        _uiState.value = _uiState.value.copy(
            aiProvider = provider,
            aiApiKey = aiApiKeyManager.getApiKey(provider),
            aiModel = aiApiKeyManager.getSelectedModel(provider),
            aiErrorMessage = null
        )
    }

    fun saveAiApiKeyForProvider(provider: AiProvider, key: String) {
        aiApiKeyManager.saveApiKey(provider, key)
        _uiState.value = _uiState.value.copy(
            aiProvider = provider,
            aiApiKey = aiApiKeyManager.getApiKey(provider),
            aiErrorMessage = null
        )
        showSnackbar("کلید API برای ${provider.displayName} ذخیره شد.")
    }

    fun selectAiModelForProvider(provider: AiProvider, model: String) {
        aiApiKeyManager.saveSelectedModel(provider, model)
        _uiState.value = _uiState.value.copy(
            aiModel = model
        )
    }

    fun executeAiAction(actionType: AiActionType, userPrompt: String) {
        val current = _uiState.value.currentProject ?: return
        val provider = _uiState.value.aiProvider
        val apiKey = _uiState.value.aiApiKey

        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                aiErrorMessage = "لطفاً ابتدا کلید API اختصاصی برای ${provider.displayName} را در بخش تنظیمات وارد کنید."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAiGenerating = true,
                aiErrorMessage = null
            )

            val result = multiAiService.generateCode(
                provider = provider,
                apiKey = apiKey,
                model = _uiState.value.aiModel,
                userPrompt = userPrompt,
                currentHtml = current.htmlCode,
                currentCss = current.cssCode,
                currentJs = current.jsCode,
                actionType = actionType
            )

            result.fold(
                onSuccess = { extracted ->
                    _uiState.value = _uiState.value.copy(
                        isAiGenerating = false,
                        lastAiResult = extracted,
                        aiErrorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isAiGenerating = false,
                        aiErrorMessage = error.message ?: "خطای ناشناخته در ارتباط با هوش مصنوعی"
                    )
                }
            )
        }
    }

    fun applyAiCode(html: String?, css: String?, js: String?) {
        val current = _uiState.value.currentProject ?: return
        val updated = current.copy(
            htmlCode = html ?: current.htmlCode,
            cssCode = css ?: current.cssCode,
            jsCode = js ?: current.jsCode,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(
            currentProject = updated,
            saveStatus = SaveStatus.SAVED
        )
        viewModelScope.launch {
            repository.update(updated)
            showSnackbar("کدهای هوش مصنوعی با موفقیت روی پروژه اعمال شدند.")
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun showSnackbar(message: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }

    // Code update with debounced auto-save
    fun onCodeChanged(newCode: String) {
        val current = _uiState.value.currentProject ?: return
        val updated = when (_uiState.value.currentTab) {
            CodeLanguage.HTML -> current.copy(htmlCode = newCode, updatedAt = System.currentTimeMillis())
            CodeLanguage.CSS -> current.copy(cssCode = newCode, updatedAt = System.currentTimeMillis())
            CodeLanguage.JS -> current.copy(jsCode = newCode, updatedAt = System.currentTimeMillis())
        }

        _uiState.value = _uiState.value.copy(
            currentProject = updated,
            saveStatus = SaveStatus.UNSAVED
        )

        // Debounced Auto-save (800ms)
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(800)
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVING)
            try {
                repository.update(updated)
            } catch (_: Exception) {
                // Safeguard against SQLite memory spikes on giant text
            }
            delay(200)
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVED)
        }
    }

    fun saveImmediately() {
        val current = _uiState.value.currentProject ?: return
        autoSaveJob?.cancel()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVING)
            try {
                repository.update(current)
            } catch (e: Exception) {
                showSnackbar("خطا در ذخیره سازی: ${e.message}")
            }
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVED)
            showSnackbar("پروژه با موفقیت ذخیره شد.")
        }
    }

    fun updateProjectTitle(newTitle: String) {
        val current = _uiState.value.currentProject ?: return
        val updated = current.copy(title = newTitle, updatedAt = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(currentProject = updated)
        viewModelScope.launch {
            repository.update(updated)
        }
    }

    fun createNewProject(title: String, template: TemplateInfo) {
        viewModelScope.launch {
            val newProject = ProjectEntity(
                title = title,
                htmlCode = template.htmlCode,
                cssCode = template.cssCode,
                jsCode = template.jsCode,
                templateType = template.id
            )
            val newId = repository.insert(newProject)
            selectProject(newId)
            _uiState.value = _uiState.value.copy(showNewProjectDialog = false)
            showSnackbar("پروژه «$title» با موفقیت ایجاد شد.")
        }
    }

    fun duplicateProject(project: ProjectEntity) {
        viewModelScope.launch {
            val duplicated = project.copy(
                id = 0,
                title = "${project.title} (کپی)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.insert(duplicated)
            selectProject(newId)
            showSnackbar("پروژه با موفقیت کپی شد.")
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.delete(projectId)
            if (_uiState.value.currentProject?.id == projectId) {
                val remaining = allProjects.value.filter { it.id != projectId }
                _uiState.value = _uiState.value.copy(currentProject = remaining.firstOrNull())
            }
            showSnackbar("پروژه حذف گردید.")
        }
    }

    fun toggleFavorite(project: ProjectEntity) {
        viewModelScope.launch {
            val updated = project.copy(isFavorite = !project.isFavorite)
            repository.update(updated)
            if (_uiState.value.currentProject?.id == project.id) {
                _uiState.value = _uiState.value.copy(currentProject = updated)
            }
        }
    }

    fun injectLibrary(snippet: String, libraryName: String) {
        val current = _uiState.value.currentProject ?: return
        val html = current.htmlCode

        val newHtml = if (html.contains("</head>", ignoreCase = true)) {
            html.replace(Regex("</head>", RegexOption.IGNORE_CASE), "  $snippet\n</head>")
        } else if (html.contains("</body>", ignoreCase = true)) {
            html.replace(Regex("</body>", RegexOption.IGNORE_CASE), "  $snippet\n</body>")
        } else {
            "$snippet\n$html"
        }

        onCodeChanged(newHtml)
        showSnackbar("کتابخانه «$libraryName» به کدهای HTML افزوده شد.")
    }

    fun insertSnippet(snippet: String) {
        val current = _uiState.value.currentProject ?: return
        val currentCode = when (_uiState.value.currentTab) {
            CodeLanguage.HTML -> current.htmlCode
            CodeLanguage.CSS -> current.cssCode
            CodeLanguage.JS -> current.jsCode
        }
        onCodeChanged(currentCode + snippet)
    }

    fun exportAsZip(context: Context) {
        val current = _uiState.value.currentProject ?: return
        try {
            val zipFile = ExportHelper.createZipFile(context, current)
            ExportHelper.shareFile(context, zipFile, "application/zip", "ارسال خروجو ZIP پروژه")
            showSnackbar("فایل زیپ با موفقیت آماده و به اشتراک گذاشته شد.")
        } catch (e: Exception) {
            showSnackbar("خطا در ایجاد خروجی زیپ: ${e.message}")
        }
    }

    fun exportAsHtml(context: Context) {
        val current = _uiState.value.currentProject ?: return
        try {
            val htmlFile = ExportHelper.createSingleHtmlFile(context, current)
            ExportHelper.shareFile(context, htmlFile, "text/html", "ارسال فایل HTML پروژه")
            showSnackbar("فایل HTML با موفقیت آماده و به اشتراک گذاشته شد.")
        } catch (e: Exception) {
            showSnackbar("خطا در صدور فایل HTML: ${e.message}")
        }
    }

    fun shareCodeText(context: Context) {
        val current = _uiState.value.currentProject ?: return
        val bundled = ExportHelper.bundleToSingleHtml(current.htmlCode, current.cssCode, current.jsCode)
        ExportHelper.sharePlainText(context, current.title, bundled)
    }

    fun copyDirectPreviewLink(context: Context) {
        val current = _uiState.value.currentProject ?: return
        try {
            val dataUri = ExportHelper.generateDataUri(current.htmlCode, current.cssCode, current.jsCode)
            if (dataUri.length > 250_000) {
                showSnackbar("به دلیل حجم بالای کدها، لطفاً از گزینه «خروجی فایل HTML» یا «اشتراک زیپ» استفاده فرمایید.")
                return
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("RITM HTML Editor Link", dataUri)
            clipboard.setPrimaryClip(clip)
            showSnackbar("لینک مستقیم در کلیپ‌بورد کپی شد!")
        } catch (e: Exception) {
            showSnackbar("خطا در ساخت لینک: ${e.message}")
        }
    }

    fun openInBrowser(context: Context) {
        val current = _uiState.value.currentProject ?: return
        try {
            val opened = ExportHelper.openInBrowser(context, current)
            if (opened) {
                showSnackbar("پروژه در مرورگر وب با موفقیت باز شد.")
            } else {
                showSnackbar("هیچ مرورگر وبی در دستگاه یافت نشد. لطفاً گوگل کروم را نصب نمایید.")
            }
        } catch (e: Exception) {
            showSnackbar("خطا در اجرای مرورگر: ${e.message ?: "خطای ناشناخته"}")
        }
    }

    fun handleIncomingHtmlUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val stringBuilder = StringBuilder()
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line).append("\n")
                            line = reader.readLine()
                        }
                    }
                }

                val loadedHtml = stringBuilder.toString()
                if (loadedHtml.isNotBlank()) {
                    val fileName = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".html")?.removeSuffix(".htm")
                        ?: "فایل وارد شده"

                    val newProject = ProjectEntity(
                        title = fileName,
                        htmlCode = loadedHtml,
                        cssCode = "",
                        jsCode = "",
                        templateType = "imported"
                    )
                    val id = repository.insert(newProject)
                    selectProject(id)
                    showSnackbar("فایل «$fileName» با موفقیت در ویرایشگر باز شد.")
                }
            } catch (e: Exception) {
                showSnackbar("خطا در باز کردن فایل HTML: ${e.message}")
            }
        }
    }
}
