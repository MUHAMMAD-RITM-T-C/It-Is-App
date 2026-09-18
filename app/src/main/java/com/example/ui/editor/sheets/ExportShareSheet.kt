package com.example.ui.editor.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportShareSheet(
    projectTitle: String,
    onOpenInChrome: () -> Unit,
    onExportZip: () -> Unit,
    onExportHtml: () -> Unit,
    onCopyDataUri: () -> Unit,
    onShareText: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "اشتراک‌گذاری و اجرای پروژه",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = projectTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExportOptionCard(
                    icon = Icons.Default.OpenInBrowser,
                    title = "اجرا و پیش‌نمایش در مرورگر کروم",
                    subtitle = "باز کردن فوری و دقیق صفحه وب در مرورگر گوگل کروم یا پیش‌فرض سیستم",
                    tag = "open_in_chrome_option_btn",
                    onClick = {
                        onOpenInChrome()
                        onDismiss()
                    }
                )

                ExportOptionCard(
                    icon = Icons.Default.FolderZip,
                    title = "خروجی به صورت فایل ZIP",
                    subtitle = "شامل فایل‌های index.html، style.css، script.js و پیش‌نمایش مستقل",
                    tag = "export_zip_btn",
                    onClick = {
                        onExportZip()
                        onDismiss()
                    }
                )

                ExportOptionCard(
                    icon = Icons.Default.Html,
                    title = "خروجی فایل HTML مستقل",
                    subtitle = "فایل یکپارچه قابل باز شدن با دوبار کلیک در هر مرورگر",
                    tag = "export_html_btn",
                    onClick = {
                        onExportHtml()
                        onDismiss()
                    }
                )

                ExportOptionCard(
                    icon = Icons.Default.Link,
                    title = "کپی لینک مستقیم پروژه (Data URI)",
                    subtitle = "آدرس اینترنتی مستقل قابل پیست مستقیم در آدرس‌بار مرورگرها بدون نیاز به سرور",
                    tag = "copy_direct_link_btn",
                    onClick = {
                        onCopyDataUri()
                        onDismiss()
                    }
                )

                ExportOptionCard(
                    icon = Icons.Default.Code,
                    title = "اشتراک‌گذاری سریع متن کد",
                    subtitle = "ارسال مستقیم متن کدها به پیام‌رسان‌ها، تلگرام، واتس‌اپ و ایمیل",
                    tag = "share_code_text_btn",
                    onClick = {
                        onShareText()
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ExportOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
