package com.example.ui.editor.syntax

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.editor.theme.EditorThemeType
import java.util.regex.Pattern

enum class CodeLanguage {
    HTML,
    CSS,
    JS
}

class CodeSyntaxVisualTransformation(
    private val language: CodeLanguage,
    private val theme: EditorThemeType,
    private val isSyntaxHighlightingEnabled: Boolean = true
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        // Only run real-time visual regex transformation if enabled and reasonable size (< 25,000 chars)
        if (!isSyntaxHighlightingEnabled || text.text.length > 25000) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        return try {
            val highlighted = highlightCode(text.text, language, theme)
            TransformedText(highlighted, OffsetMapping.Identity)
        } catch (_: Throwable) {
            // Safe fallback: never crash under any circumstances
            TransformedText(text, OffsetMapping.Identity)
        }
    }

    companion object {
        // Safe, non-backtracking regex patterns optimized for fast parsing
        private val HTML_DOCTYPE = Pattern.compile("(?i)<!DOCTYPE[^>]{0,100}>")
        private val HTML_COMMENT = Pattern.compile("<!--[\\s\\S]{0,300}?-->")
        private val HTML_TAG = Pattern.compile("</?[a-zA-Z0-9\\-]+|/?>")
        private val HTML_ATTR_NAME = Pattern.compile("[a-zA-Z0-9\\-_:]+(?=\\s*=)")
        private val HTML_STRINGS = Pattern.compile("\"[^\"\\r\\n]{0,200}\"|'[^'\\r\\n]{0,200}'")

        private val CSS_COMMENT = Pattern.compile("/\\*[\\s\\S]{0,300}?\\*/")
        private val CSS_SELECTOR = Pattern.compile("[.#]?[a-zA-Z0-9_\\-]+(?=\\s*\\{)")
        private val CSS_PROPERTY = Pattern.compile("(?m)^\\s*([a-zA-Z\\-]+)(?=\\s*:)")
        private val CSS_NUMBERS = Pattern.compile("\\b\\d+(\\.\\d+)?(px|rem|em|%|vh|vw|s|ms|deg|fr)?\\b")
        private val CSS_COLOR_HEX = Pattern.compile("#([0-9a-fA-F]{3,8})\\b")
        private val CSS_STRINGS = Pattern.compile("\"[^\"\\r\\n]{0,150}\"|'[^'\\r\\n]{0,150}'")

        private val JS_COMMENT_LINE = Pattern.compile("//[^\\r\\n]{0,250}")
        private val JS_COMMENT_BLOCK = Pattern.compile("/\\*[\\s\\S]{0,300}?\\*/")
        private val JS_KEYWORDS = Pattern.compile(
            "\\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|" +
                    "continue|default|new|this|class|extends|super|import|export|from|as|async|await|" +
                    "try|catch|finally|throw|typeof|instanceof|void|delete|in|of)\\b"
        )
        private val JS_BOOLEAN_NULL = Pattern.compile("\\b(true|false|null|undefined|NaN|Infinity)\\b")
        private val JS_BUILTINS = Pattern.compile(
            "\\b(console|document|window|Math|JSON|Array|Object|String|Number|Boolean|Promise|Set|Map|Date|" +
                    "setTimeout|setInterval|clearTimeout|clearInterval|addEventListener|getElementById|" +
                    "querySelector|querySelectorAll)\\b"
        )
        private val JS_STRINGS = Pattern.compile("\"[^\"\\r\\n]{0,200}\"|'[^'\\r\\n]{0,200}'|`[^`]{0,200}`")
        private val JS_NUMBERS = Pattern.compile("\\b\\d+(\\.\\d+)?\\b")
        private val JS_FUNCTION_CALL = Pattern.compile("\\b([a-zA-Z_$][a-zA-Z0-9_$]*)(?=\\s*\\()")

        fun highlightCode(
            code: String,
            language: CodeLanguage,
            theme: EditorThemeType
        ): AnnotatedString {
            return buildAnnotatedString {
                append(code)

                try {
                    when (language) {
                        CodeLanguage.HTML -> highlightHtml(code, theme)
                        CodeLanguage.CSS -> highlightCss(code, theme)
                        CodeLanguage.JS -> highlightJs(code, theme)
                    }
                } catch (_: Throwable) {
                    // Ignore highlighting errors and preserve text
                }
            }
        }

        private fun AnnotatedString.Builder.highlightHtml(code: String, theme: EditorThemeType) {
            applyRegex(HTML_DOCTYPE, code) { start, end ->
                addStyle(SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold), start, end)
            }
            applyRegex(HTML_TAG, code) { start, end ->
                addStyle(SpanStyle(color = theme.tagColor, fontWeight = FontWeight.SemiBold), start, end)
            }
            applyRegex(HTML_ATTR_NAME, code) { start, end ->
                addStyle(SpanStyle(color = theme.attributeColor), start, end)
            }
            applyRegex(HTML_STRINGS, code) { start, end ->
                addStyle(SpanStyle(color = theme.stringColor), start, end)
            }
            applyRegex(HTML_COMMENT, code) { start, end ->
                addStyle(SpanStyle(color = theme.commentColor), start, end)
            }
        }

        private fun AnnotatedString.Builder.highlightCss(code: String, theme: EditorThemeType) {
            applyRegex(CSS_SELECTOR, code) { start, end ->
                addStyle(SpanStyle(color = theme.selectorColor, fontWeight = FontWeight.SemiBold), start, end)
            }
            applyRegex(CSS_PROPERTY, code) { start, end ->
                addStyle(SpanStyle(color = theme.propertyColor), start, end)
            }
            applyRegex(CSS_COLOR_HEX, code) { start, end ->
                addStyle(SpanStyle(color = theme.stringColor, fontWeight = FontWeight.Bold), start, end)
            }
            applyRegex(CSS_NUMBERS, code) { start, end ->
                addStyle(SpanStyle(color = theme.numberColor), start, end)
            }
            applyRegex(CSS_STRINGS, code) { start, end ->
                addStyle(SpanStyle(color = theme.stringColor), start, end)
            }
            applyRegex(CSS_COMMENT, code) { start, end ->
                addStyle(SpanStyle(color = theme.commentColor), start, end)
            }
        }

        private fun AnnotatedString.Builder.highlightJs(code: String, theme: EditorThemeType) {
            applyRegex(JS_FUNCTION_CALL, code) { start, end ->
                addStyle(SpanStyle(color = theme.selectorColor), start, end)
            }
            applyRegex(JS_BUILTINS, code) { start, end ->
                addStyle(SpanStyle(color = theme.attributeColor, fontWeight = FontWeight.SemiBold), start, end)
            }
            applyRegex(JS_KEYWORDS, code) { start, end ->
                addStyle(SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold), start, end)
            }
            applyRegex(JS_BOOLEAN_NULL, code) { start, end ->
                addStyle(SpanStyle(color = theme.numberColor, fontWeight = FontWeight.Bold), start, end)
            }
            applyRegex(JS_NUMBERS, code) { start, end ->
                addStyle(SpanStyle(color = theme.numberColor), start, end)
            }
            applyRegex(JS_STRINGS, code) { start, end ->
                addStyle(SpanStyle(color = theme.stringColor), start, end)
            }
            applyRegex(JS_COMMENT_BLOCK, code) { start, end ->
                addStyle(SpanStyle(color = theme.commentColor), start, end)
            }
            applyRegex(JS_COMMENT_LINE, code) { start, end ->
                addStyle(SpanStyle(color = theme.commentColor), start, end)
            }
        }

        private inline fun applyRegex(
            pattern: Pattern,
            code: String,
            crossinline action: (start: Int, end: Int) -> Unit
        ) {
            try {
                val matcher = pattern.matcher(code)
                var count = 0
                // Guard against infinite loops or thousands of matches
                while (matcher.find() && count < 800) {
                    count++
                    val start = matcher.start()
                    val end = matcher.end()
                    if (start in 0 until end && end <= code.length) {
                        action(start, end)
                    }
                }
            } catch (_: Throwable) {
                // Safeguard against any regex engine faults
            }
        }
    }
}
