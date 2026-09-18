package com.example.ui.editor.sheets

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiActionType
import com.example.data.ai.AiProvider
import com.example.data.ai.ExtractedAiCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
    activeProvider: AiProvider,
    currentApiKey: String,
    currentModel: String,
    isGenerating: Boolean,
    lastAiResult: ExtractedAiCode?,
    errorMessage: String?,
    onSelectProvider: (AiProvider) -> Unit,
    onSaveApiKey: (AiProvider, String) -> Unit,
    onSelectModel: (AiProvider, String) -> Unit,
    onExecuteAiAction: (actionType: AiActionType, prompt: String) -> Unit,
    onApplyCodeToProject: (html: String?, css: String?, js: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var showSettings by remember { mutableStateOf(currentApiKey.isBlank()) }
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var selectedAction by remember { mutableStateOf(AiActionType.GENERATE_NEW) }
    var promptInput by remember { mutableStateOf("") }
    var selectedCodePreviewTab by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf(false) }

    LaunchedEffect(activeProvider, currentApiKey) {
        apiKeyInput = currentApiKey
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "دستیار چندگانه هوش مصنوعی RITM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (currentApiKey.isNotBlank()) Color(0xFF10B981) else MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activeProvider.displayName} (${currentModel})",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentApiKey.isNotBlank()) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier.testTag("toggle_ai_settings_btn")
                ) {
                    Icon(
                        imageVector = if (showSettings) Icons.Default.Close else Icons.Default.Settings,
                        contentDescription = "تنظیمات API Key",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Provider Switcher (DeepSeek, Gemini, ChatGPT, Grok, Claude)
            Text(
                text = "انتخاب ارائه‌دهنده هوش مصنوعی:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiProvider.values().forEach { provider ->
                    val isSelected = activeProvider == provider
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable {
                                onSelectProvider(provider)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("provider_chip_${provider.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = provider.displayName,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Settings Box (API Key & Model Selection for current provider)
            AnimatedVisibility(visible = showSettings) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("ai_settings_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تنظیمات کلید API اختصاصی برای ${activeProvider.displayName}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = activeProvider.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text(activeProvider.keyPlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_api_key_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model Selector Chips for Active Provider
                        Text(
                            text = "انتخاب مدل ${activeProvider.displayName}:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeProvider.availableModels.forEach { (modelId, label) ->
                                val isSelected = currentModel == modelId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSelectModel(activeProvider, modelId) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onSaveApiKey(activeProvider, apiKeyInput)
                                showSettings = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_ai_key_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ذخیره و فعال‌سازی ${activeProvider.displayName}")
                        }
                    }
                }
            }

            // Quick Action Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiActionType.values().forEach { action ->
                    val isSelected = selectedAction == action
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable {
                                selectedAction = action
                                if (action == AiActionType.FIX_BUGS && promptInput.isBlank()) {
                                    promptInput = "بررسی کدهای پروژه و برطرف کردن تمام باگ‌ها و مشکلات"
                                } else if (action == AiActionType.BEAUTIFY_MODERNIZE && promptInput.isBlank()) {
                                    promptInput = "زیباسازی ظاهر با انیمیشن و استایل‌های مدرن CSS"
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("ai_action_chip_${action.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = action.titleFa,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prompt Input Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            when (selectedAction) {
                                AiActionType.GENERATE_NEW -> "چه سایتی می‌خواهی؟ (مثلاً: لندینگ‌پیج شیک با تم دارک)"
                                AiActionType.FIX_BUGS -> "توضیح باگ یا خطاها..."
                                AiActionType.BEAUTIFY_MODERNIZE -> "چه سبک طراحی یا انیمیشنی می‌خواهی؟"
                                AiActionType.EXPLAIN_CODE -> "چه بخشی از کد را می‌خواهی توضیح دهم؟"
                                AiActionType.CUSTOM_CHAT -> "دستور یا سؤال خود را بنویسید..."
                            },
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (!isGenerating) {
                            onExecuteAiAction(selectedAction, promptInput)
                        }
                    },
                    enabled = !isGenerating && currentApiKey.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("send_ai_prompt_btn")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "ارسال")
                    }
                }
            }

            // Error Notice
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Response & Code Extraction Panel
            if (lastAiResult != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Action Bar: Apply Code & Copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نتیجه ${activeProvider.displayName}:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row {
                                if (lastAiResult.htmlCode != null || lastAiResult.cssCode != null || lastAiResult.jsCode != null) {
                                    Button(
                                        onClick = {
                                            onApplyCodeToProject(
                                                lastAiResult.htmlCode,
                                                lastAiResult.cssCode,
                                                lastAiResult.jsCode
                                            )
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("apply_ai_code_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("اعمال روی پروژه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(lastAiResult.explanation))
                                        copiedNotice = true
                                    },
                                    modifier = Modifier.testTag("copy_ai_response_btn")
                                ) {
                                    Icon(
                                        imageVector = if (copiedNotice) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "کپی پاسخ",
                                        tint = if (copiedNotice) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Code vs Explanation Tabs
                        val availableTabs = mutableListOf("توضیحات")
                        if (lastAiResult.htmlCode != null) availableTabs.add("HTML")
                        if (lastAiResult.cssCode != null) availableTabs.add("CSS")
                        if (lastAiResult.jsCode != null) availableTabs.add("JS")

                        if (availableTabs.size > 1) {
                            TabRow(
                                selectedTabIndex = selectedCodePreviewTab.coerceIn(0, availableTabs.size - 1),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availableTabs.forEachIndexed { idx, title ->
                                    Tab(
                                        selected = selectedCodePreviewTab == idx,
                                        onClick = { selectedCodePreviewTab = idx },
                                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Content Display Box
                        val currentContentText = when {
                            selectedCodePreviewTab == 0 -> lastAiResult.explanation
                            availableTabs.getOrNull(selectedCodePreviewTab) == "HTML" -> lastAiResult.htmlCode ?: ""
                            availableTabs.getOrNull(selectedCodePreviewTab) == "CSS" -> lastAiResult.cssCode ?: ""
                            availableTabs.getOrNull(selectedCodePreviewTab) == "JS" -> lastAiResult.jsCode ?: ""
                            else -> lastAiResult.explanation
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E2E))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = currentContentText,
                                color = Color(0xFFCDD6F4),
                                fontSize = 12.sp,
                                fontFamily = if (selectedCodePreviewTab == 0) FontFamily.Default else FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else if (!isGenerating) {
                // Empty state guidance
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "هوش مصنوعی ${activeProvider.displayName} آماده است",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "می‌توانید بین هوش مصنوعی‌های DeepSeek, Gemini, GPT, Grok, Claude سوییچ کنید و با کلیدهای اختصاصی خود به صورت نامحدود کدنویسی کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
