package com.example.data.ai

enum class AiProvider(
    val id: String,
    val displayName: String,
    val company: String,
    val defaultModel: String,
    val availableModels: List<Pair<String, String>>,
    val keyPlaceholder: String,
    val keyPrefKey: String,
    val modelPrefKey: String,
    val description: String
) {
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        company = "DeepSeek AI",
        defaultModel = "deepseek-chat",
        availableModels = listOf(
            "deepseek-chat" to "DeepSeek-V3 (سریع و بسیار باکیفیت)",
            "deepseek-reasoner" to "DeepSeek-R1 (استدلال عمیق و منطقی)"
        ),
        keyPlaceholder = "sk-...",
        keyPrefKey = "api_key_deepseek",
        modelPrefKey = "model_deepseek",
        description = "هوش مصنوعی فوق‌العاده قدرتمند با درک کدنویسی عمیق و هزینه بسیار اقتصادی"
    ),
    GEMINI(
        id = "gemini",
        displayName = "Gemini",
        company = "Google",
        defaultModel = "gemini-2.5-flash",
        availableModels = listOf(
            "gemini-2.5-flash" to "Gemini 2.5 Flash (پیشنهادی گوگل)",
            "gemini-2.5-pro" to "Gemini 2.5 Pro (کدنویسی پیشرفته)",
            "gemini-1.5-flash" to "Gemini 1.5 Flash (پایدار)"
        ),
        keyPlaceholder = "AIzaSy...",
        keyPrefKey = "api_key_gemini",
        modelPrefKey = "model_gemini",
        description = "موتور هوش مصنوعی سریع گوگل با پشتیبانی عالی از کدهای وب"
    ),
    GPT(
        id = "gpt",
        displayName = "ChatGPT (GPT)",
        company = "OpenAI",
        defaultModel = "gpt-4o-mini",
        availableModels = listOf(
            "gpt-4o-mini" to "GPT-4o Mini (سریع و بهینه)",
            "gpt-4o" to "GPT-4o (هوش همه‌جانبه و دقیق)",
            "o3-mini" to "o3-mini (استدلال و حل مسئله)"
        ),
        keyPlaceholder = "sk-proj-...",
        keyPrefKey = "api_key_gpt",
        modelPrefKey = "model_gpt",
        description = "مدل‌های پرچم‌دار OpenAI برای تولید کد، معماری نرم‌افزار و خطایابی"
    ),
    GROK(
        id = "grok",
        displayName = "Grok",
        company = "xAI",
        defaultModel = "grok-2-latest",
        availableModels = listOf(
            "grok-2-latest" to "Grok 2 Latest (جدیدترین نسخه)",
            "grok-beta" to "Grok Beta"
        ),
        keyPlaceholder = "xai-...",
        keyPrefKey = "api_key_grok",
        modelPrefKey = "model_grok",
        description = "هوش مصنوعی پیشرفته xAI با سرعت پردازش بالا و بینش دقیق"
    ),
    CLAUDE(
        id = "claude",
        displayName = "Claude",
        company = "Anthropic",
        defaultModel = "claude-3-5-sonnet-latest",
        availableModels = listOf(
            "claude-3-5-sonnet-latest" to "Claude 3.5 Sonnet (سلطان کدنویسی و فرانت‌اند)",
            "claude-3-5-haiku-latest" to "Claude 3.5 Haiku (فوق‌العاده سریع)"
        ),
        keyPlaceholder = "sk-ant-...",
        keyPrefKey = "api_key_claude",
        modelPrefKey = "model_claude",
        description = "یکی از بهترین مدل‌های جهان در درک فرانت‌اند و ساخت صفحات وب تمیز"
    );

    companion object {
        fun fromId(id: String): AiProvider {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEEPSEEK
        }
    }
}
