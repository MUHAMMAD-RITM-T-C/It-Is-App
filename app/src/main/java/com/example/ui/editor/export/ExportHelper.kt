package com.example.ui.editor.export

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.ProjectEntity
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportHelper {

    private const val TAG = "ExportHelper"

    fun bundleToSingleHtml(
        html: String,
        css: String,
        js: String,
        withConsoleCatcher: Boolean = false
    ): String {
        val consoleScript = if (withConsoleCatcher) {
            """
<script>
(function() {
    function sendLog(type, args) {
        try {
            var msg = Array.prototype.slice.call(args).map(function(item) {
                if (typeof item === 'object') {
                    try { return JSON.stringify(item); } catch(e) { return String(item); }
                }
                return String(item);
            }).join(' ');
            if (window.AndroidConsole && window.AndroidConsole.onLog) {
                window.AndroidConsole.onLog(type, msg);
            }
        } catch(e) {}
    }
    var origLog = console.log;
    var origWarn = console.warn;
    var origError = console.error;
    console.log = function() { origLog.apply(console, arguments); sendLog('info', arguments); };
    console.warn = function() { origWarn.apply(console, arguments); sendLog('warn', arguments); };
    console.error = function() { origError.apply(console, arguments); sendLog('error', arguments); };
    window.onerror = function(msg, url, line, col, error) {
        sendLog('error', ['خطای زمان اجرا: ' + msg + ' (سطر: ' + line + ')']);
    };
})();
</script>
"""
        } else ""

        val styleTag = if (css.isNotBlank()) "<style>\n$css\n</style>" else ""
        val scriptTag = if (js.isNotBlank()) "<script>\n$js\n</script>" else ""

        // If user already wrote complete <html> document
        return if (html.contains("</head>", ignoreCase = true)) {
            val headInjected = html.replace(
                Regex("</head>", RegexOption.IGNORE_CASE),
                "$consoleScript\n$styleTag\n</head>"
            )
            if (headInjected.contains("</body>", ignoreCase = true)) {
                headInjected.replace(
                    Regex("</body>", RegexOption.IGNORE_CASE),
                    "$scriptTag\n</body>"
                )
            } else {
                "$headInjected\n$scriptTag"
            }
        } else if (html.contains("<!DOCTYPE", ignoreCase = true) || html.contains("<html", ignoreCase = true)) {
            """$html
$consoleScript
$styleTag
$scriptTag"""
        } else {
            // Fragment HTML: wrap in full template
            """<!DOCTYPE html>
<html lang="fa">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  $consoleScript
  $styleTag
</head>
<body>
$html
$scriptTag
</body>
</html>"""
        }
    }

    fun generateDataUri(html: String, css: String, js: String): String {
        val fullHtml = bundleToSingleHtml(html, css, js, withConsoleCatcher = false)
        val base64 = Base64.encodeToString(fullHtml.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        return "data:text/html;charset=utf-8;base64,$base64"
    }

    fun createZipFile(context: Context, project: ProjectEntity): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = project.title.replace(Regex("[^a-zA-Z0-9آ-ی_-]"), "_").take(30).ifBlank { "project" }
        val zipFile = File(exportDir, "${safeName}_export.zip")

        val htmlLinked = if (project.htmlCode.contains("</head>", ignoreCase = true)) {
            project.htmlCode.replace(
                Regex("</head>", RegexOption.IGNORE_CASE),
                "<link rel=\"stylesheet\" href=\"style.css\">\n</head>"
            ).replace(
                Regex("</body>", RegexOption.IGNORE_CASE),
                "<script src=\"script.js\"></script>\n</body>"
            )
        } else {
            """<!DOCTYPE html>
<html lang="fa">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <link rel="stylesheet" href="style.css">
  <title>${project.title}</title>
</head>
<body>
${project.htmlCode}
  <script src="script.js"></script>
</body>
</html>"""
        }

        val standaloneHtml = bundleToSingleHtml(project.htmlCode, project.cssCode, project.jsCode)

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. index.html
            addEntry(zos, "index.html", htmlLinked)
            // 2. style.css
            addEntry(zos, "style.css", project.cssCode)
            // 3. script.js
            addEntry(zos, "script.js", project.jsCode)
            // 4. standalone.html (ready-to-run single file)
            addEntry(zos, "standalone_preview.html", standaloneHtml)
            // 5. README.md
            val readme = """# ${project.title}
تولید شده با اپلیکیشن RITM Html Editor
تاریخ خروجی: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}

## نحوه اجرا
- برای باز کردن پروژه در مرورگر، می‌توانید فایل `standalone_preview.html` یا `index.html` را در هر مرورگری اجرا کنید.
"""
            addEntry(zos, "README.md", readme)
        }

        return zipFile
    }

    private fun addEntry(zos: ZipOutputStream, fileName: String, content: String) {
        val entry = ZipEntry(fileName)
        zos.putNextEntry(entry)
        zos.write(content.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    fun createSingleHtmlFile(context: Context, project: ProjectEntity): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = project.title.replace(Regex("[^a-zA-Z0-9آ-ی_-]"), "_").take(30).ifBlank { "project" }
        val htmlFile = File(exportDir, "$safeName.html")
        val content = bundleToSingleHtml(project.htmlCode, project.cssCode, project.jsCode)
        htmlFile.writeText(content, StandardCharsets.UTF_8)
        return htmlFile
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Explicitly grant read uri permission to target packages resolving this intent
        grantUriPermissionToMatchingActivities(context, shareIntent, uri)

        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Safe sharing for text of any size without Binder TransactionTooLargeException crashes.
     */
    fun sharePlainText(context: Context, title: String, text: String) {
        try {
            // Android Binder Transaction limit is 1MB shared; anything over ~25KB risks TransactionTooLargeException
            if (text.length > 25_000) {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val safeFile = File(exportDir, "${title.take(20).replace(Regex("[^a-zA-Z0-9آ-ی_-]"), "_")}_code.html")
                safeFile.writeText(text, StandardCharsets.UTF_8)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    safeFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/html"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, "$title\n\n(کد کامل به دلیل حجم بالا در فایل پیوست ارسال شد)")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                grantUriPermissionToMatchingActivities(context, shareIntent, uri)
                val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری پروژه").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری کد").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share plain text", e)
        }
    }

    /**
     * Opens the bundled standalone HTML project directly in Chrome or the system browser.
     */
    fun openInBrowser(context: Context, project: ProjectEntity): Boolean {
        return try {
            val htmlFile = createSingleHtmlFile(context, project)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                htmlFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Explicitly grant read uri permission to potential viewers
            grantUriPermissionToMatchingActivities(context, viewIntent, uri)

            // 1. Try Google Chrome
            val chromePackages = listOf(
                "com.android.chrome",
                "com.google.android.apps.chrome",
                "com.chrome.beta",
                "com.chrome.dev",
                "com.chrome.canary"
            )

            for (pkg in chromePackages) {
                try {
                    context.packageManager.getPackageInfo(pkg, 0)
                    val chromeIntent = Intent(viewIntent).apply {
                        setPackage(pkg)
                    }
                    context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(chromeIntent)
                    return true
                } catch (_: Exception) {
                    // Not installed, continue trying next
                }
            }

            // 2. Try generic browser chooser with text/html
            try {
                val chooser = Intent.createChooser(viewIntent, "مشاهده در مرورگر وب").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Chooser failed for text/html, falling back to HTTPS scheme intent", e)
            }

            // 3. Fallback: try opening default system browser with view intent without specific MIME
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "All browser launch attempts failed", e)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening in browser", e)
            false
        }
    }

    private fun grantUriPermissionToMatchingActivities(context: Context, intent: Intent, uri: Uri) {
        try {
            val resolveInfoList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            for (resolveInfo in resolveInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not grant uri permissions to resolve infos", e)
        }
    }
}

