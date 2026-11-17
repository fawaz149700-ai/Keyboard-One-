package com.example.data.translate

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TranslationLanguageInfo(
    val code: String,
    val arabicName: String,
    val mlKitCode: String,
)

data class TranslationResult(
    val translatedText: String,
    val detectedSourceCode: String,
    val usedOfflineMlKit: Boolean,
)

class TranslationRepository {

    private val modelManager by lazy { RemoteModelManager.getInstance() }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    val supportedLanguages = listOf(
        TranslationLanguageInfo("ar", "العربية", TranslateLanguage.ARABIC),
        TranslationLanguageInfo("en", "الإنجليزية", TranslateLanguage.ENGLISH),
        TranslationLanguageInfo("fr", "الفرنسية", TranslateLanguage.FRENCH),
        TranslationLanguageInfo("es", "الإسبانية", TranslateLanguage.SPANISH),
        TranslationLanguageInfo("tr", "التركية", TranslateLanguage.TURKISH),
        TranslationLanguageInfo("fa", "الفارسية", TranslateLanguage.PERSIAN),
        TranslationLanguageInfo("ur", "الأردية", TranslateLanguage.URDU),
        TranslationLanguageInfo("ru", "الروسية", TranslateLanguage.RUSSIAN),
        TranslationLanguageInfo("de", "الألمانية", TranslateLanguage.GERMAN),
    )

    fun mapToMlKit(code: String): String? =
        supportedLanguages.firstOrNull { it.code == code }?.mlKitCode
            ?: TranslateLanguage.fromLanguageTag(code)

    suspend fun getDownloadedLanguageCodes(): Set<String> {
        val models = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).awaitTask()
        val downloadedMlKit = models.map { it.language }.toSet()
        return supportedLanguages.filter { it.mlKitCode in downloadedMlKit }.map { it.code }.toSet()
    }

    suspend fun downloadLanguageModel(code: String): Result<Unit> = runCatching {
        val mlKitCode = mapToMlKit(code) ?: error("Unsupported language: $code")
        val model = TranslateRemoteModel.Builder(mlKitCode).build()
        val conditions = DownloadConditions.Builder().build()
        modelManager.download(model, conditions).awaitTask()
        Unit
    }

    suspend fun deleteLanguageModel(code: String): Result<Unit> = runCatching {
        val mlKitCode = mapToMlKit(code) ?: error("Unsupported language: $code")
        val model = TranslateRemoteModel.Builder(mlKitCode).build()
        modelManager.deleteDownloadedModel(model).awaitTask()
        Unit
    }

    suspend fun detectLanguage(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "ar"
        val fromMlKit = runCatching {
            val identifier = LanguageIdentification.getClient()
            val code = identifier.identifyLanguage(trimmed).awaitTask()
            identifier.close()
            if (code == "und") null else code
        }.getOrNull()
        if (!fromMlKit.isNullOrBlank()) return fromMlKit
        // Heuristic fallback for Arabic/Persian/Cyrillic scripts
        val hasArabic = trimmed.any { it in '\u0600'..'\u06FF' }
        if (hasArabic) return "ar"
        val hasCyrillic = trimmed.any { it in '\u0400'..'\u04FF' }
        if (hasCyrillic) return "ru"
        return "en"
    }

    suspend fun translate(
        text: String,
        sourceCode: String,
        targetCode: String,
        allowCloudFallback: Boolean,
    ): Result<TranslationResult> = runCatching {
        val clean = text.trim()
        if (clean.isEmpty()) error("النص فارغ")

        val resolvedSource = if (sourceCode == "auto") detectLanguage(clean) else sourceCode
        if (resolvedSource == targetCode) {
            return@runCatching TranslationResult(clean, resolvedSource, true)
        }

        val srcMlKit = mapToMlKit(resolvedSource)
        val tgtMlKit = mapToMlKit(targetCode)

        if (srcMlKit != null && tgtMlKit != null) {
            val downloaded = getDownloadedLanguageCodes()
            val bothReady = resolvedSource in downloaded && targetCode in downloaded
            if (bothReady || !allowCloudFallback) {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(srcMlKit)
                    .setTargetLanguage(tgtMlKit)
                    .build()
                val translator = Translation.getClient(options)
                try {
                    val conditions = DownloadConditions.Builder().build()
                    translator.downloadModelIfNeeded(conditions).awaitTask()
                    val translated = translator.translate(clean).awaitTask()
                    return@runCatching TranslationResult(translated, resolvedSource, true)
                } finally {
                    translator.close()
                }
            }
        }

        if (allowCloudFallback) {
            val cloud = translateViaCloud(clean, resolvedSource, targetCode)
            return@runCatching TranslationResult(cloud, resolvedSource, false)
        } else {
            error("يرجى تنزيل حزمة اللغة للترجمة بدون إنترنت أو تفعيل الترجمة السحابية من الإعدادات")
        }
    }

    private suspend fun translateViaCloud(
        text: String,
        source: String,
        target: String,
    ): String = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(text, "UTF-8")
        val pair = URLEncoder.encode("$source|$target", "UTF-8")
        val url = "https://api.mymemory.translated.net/get?q=$q&langpair=$pair"
        val req = Request.Builder().url(url).get().build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("فشل الاتصال بخدمة الترجمة (${resp.code})")
            val body = resp.body?.string().orEmpty()
            val jsonObj = JSONObject(body)
            val data = jsonObj.getJSONObject("responseData")
            val translated = data.optString("translatedText", "")
            if (translated.isBlank()) error("لم يتم الحصول على ترجمة")
            translated
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { e ->
            if (cont.isActive) cont.resumeWithException(e)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
