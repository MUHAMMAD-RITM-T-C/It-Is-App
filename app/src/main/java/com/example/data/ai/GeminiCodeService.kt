package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ExtractedAiCode(
    val explanation: String,
    val htmlCode: String? = null,
    val cssCode: String? = null,
    val jsCode: String? = null
)

class GeminiCodeService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateCode(
        apiKey: String,
        model: String,
        userPrompt: String,
        currentHtml: String,
        currentCss: String,
        currentJs: String,
        actionType: AiActionType
    ): Result<ExtractedAiCode> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لطفاً ابتدا کلید API هوش مصنوعی (Gemini API Key) خود را وارد کنید.")
            )
        }

        try {
            val systemInstructionText = """
                شما یک دستیار ارشد هوش مصنوعی برای توسعه‌دهندگان وب در اپلیکیشن HTML Code Studio هستید.
                وظیفه شما نوشتن، عیب‌یابی، بهینه‌سازی و تولید کدهای تمیز و مدرن HTML5، CSS3 و JavaScript است.
                پاسخ‌های شما باید شامل توضیحات شفاف به زبان فارسی باشد.
                وقتی کد پیشنهاد می‌دهید، حتماً از بلوک‌های کد مارک‌داون با مشخص کردن زبان استفاده کنید:
                ```html
                ...
                ```
                ```css
                ...
                ```
                ```javascript
                ...
                ```
                کدها باید کاملاً مستقل، معتبر و بدون خطای نگارشی باشند.
            """.trimIndent()

            val combinedPrompt = buildPrompt(actionType, userPrompt, currentHtml, currentCss, currentJs)

            // Construct JSON request
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", combinedPrompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    val sysParts = JSONArray().apply {
                        val sysPart = JSONObject().apply {
                            put("text", systemInstructionText)
                        }
                        put(sysPart)
                    }
                    put("parts", sysParts)
                }
                put("systemInstruction", systemInstructionObj)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.3)
                }
                put("generationConfig", genConfig)
            }

            val targetModel = if (model.isNotBlank()) model else "gemini-2.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBodyString, response.code)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val fullText = parseCandidatesText(responseBodyString)
            val extracted = extractCodeBlocks(fullText)
            Result.success(extracted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        actionType: AiActionType,
        userPrompt: String,
        currentHtml: String,
        currentCss: String,
        currentJs: String
    ): String {
        val baseContext = """
            کدهای فعلی پروژه:
            --- HTML ---
            $currentHtml
            
            --- CSS ---
            $currentCss
            
            --- JavaScript ---
            $currentJs
        """.trimIndent()

        return when (actionType) {
            AiActionType.GENERATE_NEW -> """
                کاربر درخواست ساخت پروژه جدید وب با مشخصات زیر را دارد:
                «$userPrompt»
                
                لطفاً کدهای کامل HTML5، CSS3 و JavaScript را به صورت جداگانه در بلوک‌های کد ارائه دهید.
                طراحی شیک، مدرن، واکنش‌گرا و کاربردی باشد.
            """.trimIndent()

            AiActionType.FIX_BUGS -> """
                $baseContext
                
                لطفاً کدهای فوق را بررسی کن و خطاهای احتمالی، باگ‌های ساختاری یا منطقی و مشکلات اجرایی را پیدا و برطرف کن.
                توضیح بده چه چیزهایی اصلاح شد و کدهای اصلاح‌شده را در بلوک‌های کد قرار بده.
                درخواست تکمیلی کاربر: ${userPrompt.ifBlank { "رفع تمام خطاهای کد" }}
            """.trimIndent()

            AiActionType.BEAUTIFY_MODERNIZE -> """
                $baseContext
                
                لطفاً ظاهر، استایل و انیمیشن‌های این پروژه را به زیباترین شکل مدرن کن (با افکت‌های چشم‌نواز، گرادینت، فونت خوانا، دکمه‌های شکیل).
                کدهای بهینه‌شده را در بلوک‌های کد ارائه کن.
                درخواست ویژه کاربر: ${userPrompt.ifBlank { "مدرن‌سازی حداکثری ظاهر پروژه" }}
            """.trimIndent()

            AiActionType.EXPLAIN_CODE -> """
                $baseContext
                
                لطفاً نحوه کارکرد این کدها، معماری ساختار صفحه، استایل‌ها و رفتارهای جاوااسکریپت آن را به زبان ساده و روان فارسی توضیح بده.
                سؤال کاربر: ${userPrompt.ifBlank { "توضیح کامل ساختار و عملکرد این برنامه" }}
            """.trimIndent()

            AiActionType.CUSTOM_CHAT -> """
                $baseContext
                
                دستور / سؤال کاربر:
                $userPrompt
                
                اگر نیاز به تغییر یا اضافه کردن کدی است، حتماً کدهای به‌روزشده را در بلوک‌های کد مارک‌داون قرار بده.
            """.trimIndent()
        }
    }

    private fun parseCandidatesText(responseJson: String): String {
        return try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates") ?: return "پاسخی از مدل دریافت نشد."
            if (candidates.length() == 0) return "پاسخی از مدل دریافت نشد."
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return "محتوایی در پاسخ وجود ندارد."
            val parts = content.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                sb.append(part.optString("text", ""))
            }
            sb.toString()
        } catch (e: Exception) {
            "خطا در پردازش پاسخ هوش مصنوعی: ${e.message}"
        }
    }

    private fun parseErrorMessage(errorBody: String, httpCode: Int): String {
        return try {
            val root = JSONObject(errorBody)
            val errorObj = root.optJSONObject("error")
            val message = errorObj?.optString("message") ?: "خطای ناشناخته (کد: $httpCode)"
            when {
                httpCode == 400 && message.contains("API_KEY_INVALID", ignoreCase = true) ->
                    "کلید API هوش مصنوعی نامعتبر است. لطفاً کلید صحیح را در تنظیمات هوش مصنوعی وارد نمایید."
                httpCode == 429 ->
                    "محدودیت تعداد درخواست به پایان رسیده است. لطفاً لحظاتی دیگر تلاش کنید."
                else -> "خطای سرویس هوش مصنوعی ($httpCode): $message"
            }
        } catch (_: Exception) {
            "خطا در ارتباط با سرور هوش مصنوعی (کد وضعیت: $httpCode)"
        }
    }

    private fun extractCodeBlocks(text: String): ExtractedAiCode {
        var html: String? = null
        var css: String? = null
        var js: String? = null

        // Safe extraction of ```html ... ```
        val htmlRegex = Regex("```(?:html|htm)\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val cssRegex = Regex("```css\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val jsRegex = Regex("```(?:javascript|js)\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)

        html = htmlRegex.find(text)?.groups?.get(1)?.value?.trim()
        css = cssRegex.find(text)?.groups?.get(1)?.value?.trim()
        js = jsRegex.find(text)?.groups?.get(1)?.value?.trim()

        return ExtractedAiCode(
            explanation = text,
            htmlCode = html,
            cssCode = css,
            jsCode = js
        )
    }
}

enum class AiActionType(val titleFa: String) {
    GENERATE_NEW("تولید کد جدید"),
    FIX_BUGS("رفع باگ و خطاها"),
    BEAUTIFY_MODERNIZE("مدرن‌سازی استایل"),
    EXPLAIN_CODE("توضیح کدها"),
    CUSTOM_CHAT("درخواست سفارشی")
}
