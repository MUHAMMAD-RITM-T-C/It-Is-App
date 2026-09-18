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

class MultiProviderAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateCode(
        provider: AiProvider,
        apiKey: String,
        model: String,
        userPrompt: String,
        currentHtml: String,
        currentCss: String,
        currentJs: String,
        actionType: AiActionType
    ): Result<ExtractedAiCode> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لطفاً ابتدا کلید API اختصاصی برای ${provider.displayName} را در تنظیمات وارد کنید.")
            )
        }

        val targetModel = model.ifBlank { provider.defaultModel }

        val systemPrompt = """
            شما یک دستیار متخصص کدنویسی وب در اپلیکیشن «RITM Html Editor» هستید.
            کدهای تولیدی شما باید مدرن، استاندارد و با کیفیت بالا در زبان‌های HTML5, CSS3, JavaScript باشند.
            توضیحات و راهنمایی‌های خود را به زبان فارسی روان ارائه دهید.
            برای هر بخش از کد حتماً از بلوک‌های مشخص استفاده نمایید:
            ```html
            ...
            ```
            ```css
            ...
            ```
            ```javascript
            ...
            ```
            کدها باید کاملاً مستقل، دارای ظاهر شیک، رسپانسیو و بدون باگ باشند.
        """.trimIndent()

        val combinedPrompt = buildPrompt(actionType, userPrompt, currentHtml, currentCss, currentJs)

        try {
            val responseText: String = when (provider) {
                AiProvider.DEEPSEEK -> callOpenAiCompatible(
                    endpointUrl = "https://api.deepseek.com/chat/completions",
                    apiKey = trimmedKey,
                    model = targetModel,
                    systemPrompt = systemPrompt,
                    userPrompt = combinedPrompt,
                    providerName = "DeepSeek"
                )
                AiProvider.GPT -> callOpenAiCompatible(
                    endpointUrl = "https://api.openai.com/v1/chat/completions",
                    apiKey = trimmedKey,
                    model = targetModel,
                    systemPrompt = systemPrompt,
                    userPrompt = combinedPrompt,
                    providerName = "ChatGPT"
                )
                AiProvider.GROK -> callOpenAiCompatible(
                    endpointUrl = "https://api.x.ai/v1/chat/completions",
                    apiKey = trimmedKey,
                    model = targetModel,
                    systemPrompt = systemPrompt,
                    userPrompt = combinedPrompt,
                    providerName = "Grok"
                )
                AiProvider.CLAUDE -> callClaude(
                    apiKey = trimmedKey,
                    model = targetModel,
                    systemPrompt = systemPrompt,
                    userPrompt = combinedPrompt
                )
                AiProvider.GEMINI -> callGemini(
                    apiKey = trimmedKey,
                    model = targetModel,
                    systemPrompt = systemPrompt,
                    userPrompt = combinedPrompt
                )
            }

            val extracted = extractCodeBlocks(responseText)
            Result.success(extracted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callOpenAiCompatible(
        endpointUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        providerName: String
    ): String {
        val root = JSONObject().apply {
            put("model", model)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
            put("messages", messages)
            put("temperature", 0.3)
        }

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(root.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val err = parseOpenAiError(bodyString, response.code, providerName)
            throw Exception(err)
        }

        val resJson = JSONObject(bodyString)
        val choices = resJson.optJSONArray("choices") ?: throw Exception("پاسخی از $providerName دریافت نشد.")
        if (choices.length() == 0) throw Exception("پاسخ دریافتی از $providerName خالی است.")
        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.optJSONObject("message")
        return message?.optString("content", "") ?: throw Exception("محتوایی در پاسخ $providerName یافت نشد.")
    }

    private fun callClaude(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        val root = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            put("system", systemPrompt)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
            put("messages", messages)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(root.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val err = parseClaudeError(bodyString, response.code)
            throw Exception(err)
        }

        val resJson = JSONObject(bodyString)
        val contentArray = resJson.optJSONArray("content") ?: throw Exception("پاسخی از Claude دریافت نشد.")
        val sb = StringBuilder()
        for (i in 0 until contentArray.length()) {
            val item = contentArray.getJSONObject(i)
            if (item.optString("type") == "text") {
                sb.append(item.optString("text", ""))
            }
        }
        return sb.toString().ifBlank { throw Exception("محتوای متنی از Claude دریافت نشد.") }
    }

    private fun callGemini(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        val root = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)

            val sys = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                }
                put("parts", parts)
            }
            put("systemInstruction", sys)

            val genConfig = JSONObject().apply {
                put("temperature", 0.3)
            }
            put("generationConfig", genConfig)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(root.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val err = parseGeminiError(bodyString, response.code)
            throw Exception(err)
        }

        val resJson = JSONObject(bodyString)
        val candidates = resJson.optJSONArray("candidates") ?: throw Exception("پاسخی از Gemini دریافت نشد.")
        if (candidates.length() == 0) throw Exception("پاسخ مدل خالی است.")
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: throw Exception("محتوایی یافت نشد.")
        val parts = content.optJSONArray("parts") ?: throw Exception("محتوایی یافت نشد.")
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.getJSONObject(i).optString("text", ""))
        }
        return sb.toString()
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

    private fun parseOpenAiError(errorBody: String, httpCode: Int, providerName: String): String {
        return try {
            val root = JSONObject(errorBody)
            val errObj = root.optJSONObject("error")
            val msg = errObj?.optString("message") ?: "خطای ناشناخته"
            when (httpCode) {
                401 -> "کلید API برای $providerName اشتباه یا منقضی شده است. لطفاً کلید معتبر را در تنظیمات وارد کنید."
                429 -> "محدودیت تعداد درخواست یا اعتبار حساب $providerName تمام شده است. لطفاً سقف مصرف خود را بررسی کنید."
                500, 503 -> "سرورهای $providerName موقتاً با بار ترافیکی مواجه هستند. لطفاً لحظاتی دیگر مجدداً تلاش نمایید."
                else -> "خطای $providerName (کد $httpCode): $msg"
            }
        } catch (_: Exception) {
            "خطا در ارتباط با سرور $providerName (کد $httpCode)"
        }
    }

    private fun parseClaudeError(errorBody: String, httpCode: Int): String {
        return try {
            val root = JSONObject(errorBody)
            val errObj = root.optJSONObject("error")
            val msg = errObj?.optString("message") ?: "خطای سرور Claude"
            when (httpCode) {
                401 -> "کلید API برای Claude (Anthropic) معتبر نمی‌باشد. لطفاً کلید صحیح (sk-ant-...) را وارد نمایید."
                429 -> "محدودیت نرخ درخواست برای کلید Claude شما پر شده است."
                else -> "خطای سرویس Claude ($httpCode): $msg"
            }
        } catch (_: Exception) {
            "خطای ارتباط با سرور Claude (کد $httpCode)"
        }
    }

    private fun parseGeminiError(errorBody: String, httpCode: Int): String {
        return try {
            val root = JSONObject(errorBody)
            val errorObj = root.optJSONObject("error")
            val message = errorObj?.optString("message") ?: "خطای ناشناخته (کد: $httpCode)"
            when {
                httpCode == 400 && message.contains("API_KEY_INVALID", ignoreCase = true) ->
                    "کلید API هوش مصنوعی نامعتبر است. لطفاً کلید صحیح Gemini را وارد نمایید."
                httpCode == 429 ->
                    "محدودیت تعداد درخواست یا سهمیه تمام شده است. لطفاً اندکی بعد امتحان کنید."
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
