package com.example.data.dictionary

import com.example.data.db.UserDictionaryDao
import com.example.data.db.UserWordEntity
import com.example.model.LanguageId
import kotlin.math.abs
import kotlin.math.min

class SuggestionEngine(private val userDao: UserDictionaryDao) {

    private val coreWords: Map<String, List<String>> = mapOf(
        "ar" to listOf(
            "السلام", "عليكم", "ورحمة", "الله", "وبركاته", "مرحبا", "أهلا", "وسهلا", "كيف", "حالك",
            "الحمد", "لله", "بخير", "شكرا", "جزيلا", "عفوا", "تفضل", "إن", "شاء", "ما", "الخير",
            "صباح", "مساء", "النور", "الفل", "اليوم", "غدا", "أمس", "الآن", "هنا", "هناك", "نعم", "لا",
            "من", "إلى", "على", "في", "عن", "مع", "هذا", "هذه", "ذلك", "تلك", "الذي", "التي", "أنا",
            "أنت", "هو", "هي", "نحن", "هم", "أريد", "أحب", "أعمل", "ممتاز", "رائع", "جميل", "جدا",
            "حسنا", "موافق", "تم", "انتظر", "دقيقة", "لحظة", "ساعة", "رسالة", "تطبيق", "هاتف", "كيبورد",
            "لوحة", "المفاتيح", "العربية", "الإنجليزية", "ترجمة", "حافظة", "نسخ", "لصق", "بحث", "إرسال",
            "جديد", "قديم", "كبير", "صغير", "سريع", "سهل", "مفيد", "مهم", "ممكن", "أكيد", "طبعا",
            "مبروك", "بالتوفيق", "تقبل", "تحياتي", "معاذ", "محمد", "أحمد", "فواز", "سارة", "نورة",
            "الرياض", "جدة", "مكة", "دبي", "القاهرة", "عمان", "الكويت", "الدوحة", "البحرين", "مسقط"
        ),
        "en" to listOf(
            "the", "and", "you", "that", "was", "for", "are", "with", "his", "they", "this", "have",
            "from", "one", "had", "word", "but", "not", "what", "all", "were", "when", "your", "can",
            "said", "there", "use", "each", "which", "she", "how", "their", "will", "other", "about",
            "out", "many", "then", "them", "these", "some", "her", "would", "make", "like", "time",
            "hello", "thanks", "thank", "please", "good", "morning", "evening", "night", "great",
            "awesome", "keyboard", "android", "message", "today", "tomorrow", "meeting", "work",
            "home", "family", "friend", "love", "happy", "sorry", "welcome", "sure", "okay", "yes",
            "because", "before", "after", "always", "never", "really", "actually", "maybe", "done"
        ),
        "fr" to listOf("bonjour", "merci", "salut", "comment", "bien", "oui", "non", "avec", "pour", "dans", "maison", "temps", "toujours"),
        "es" to listOf("hola", "gracias", "buenos", "días", "noches", "como", "estas", "bien", "si", "no", "por", "favor", "amigo", "casa"),
        "tr" to listOf("merhaba", "teşekkürler", "nasılsın", "iyiyim", "evet", "hayır", "güzel", "tamam", "bugün", "yarın", "lütfen"),
        "fa" to listOf("سلام", "خوبی", "ممنون", "متشکرم", "بله", "خیر", "امروز", "فردا", "عالی", "دوست", "خانه", "کار"),
        "ur" to listOf("السلام", "علیکم", "شکریہ", "کیسے", "ہیں", "ٹھیک", "ہوں", "آج", "کل", "اچھا", "بہت", "پیار"),
        "ru" to listOf("привет", "спасибо", "как", "дела", "хорошо", "да", "нет", "пожалуйста", "сегодня", "завтра", "дом", "друг"),
        "de" to listOf("hallo", "danke", "bitte", "guten", "morgen", "abend", "ja", "nein", "gut", "heute", "morgen", "freund", "haus"),
    )

    private val nextWordBigrams: Map<String, List<String>> = mapOf(
        "السلام" to listOf("عليكم", "ورحمة"),
        "عليكم" to listOf("ورحمة", "السلام"),
        "ورحمة" to listOf("الله"),
        "الله" to listOf("وبركاته", "أكبر", "يبارك"),
        "صباح" to listOf("الخير", "النور", "الفل"),
        "مساء" to listOf("الخير", "النور", "الورد"),
        "إن" to listOf("شاء"),
        "شاء" to listOf("الله"),
        "شكرا" to listOf("جزيلا", "لك"),
        "كيف" to listOf("حالك", "الحال"),
        "thank" to listOf("you", "god"),
        "good" to listOf("morning", "evening", "night", "luck"),
        "how" to listOf("are", "about", "much"),
        "are" to listOf("you", "we", "they"),
    )

    suspend fun getSuggestions(
        languageId: LanguageId,
        currentWord: String,
        previousWord: String,
    ): List<String> {
        val code = languageId.code
        val trimmed = currentWord.trim()
        if (trimmed.isEmpty()) {
            val prev = previousWord.trim().lowercase()
            val bigrams = nextWordBigrams[previousWord.trim()] ?: nextWordBigrams[prev]
            if (!bigrams.isNullOrEmpty()) return bigrams.take(3)
            val defaults = when (languageId) {
                LanguageId.ARABIC -> listOf("السلام", "مرحبا", "شكرا")
                LanguageId.ENGLISH -> listOf("Hello", "Thanks", "Okay")
                LanguageId.FRENCH -> listOf("Bonjour", "Merci", "Oui")
                LanguageId.SPANISH -> listOf("Hola", "Gracias", "Bien")
                LanguageId.TURKISH -> listOf("Merhaba", "Teşekkürler", "Tamam")
                LanguageId.PERSIAN -> listOf("سلام", "ممنون", "خوبی")
                LanguageId.URDU -> listOf("السلام", "شکریہ", "کیسے")
                LanguageId.RUSSIAN -> listOf("Привет", "Спасибо", "Хорошо")
                LanguageId.GERMAN -> listOf("Hallo", "Danke", "Gut")
            }
            return defaults
        }

        val lower = trimmed.lowercase()
        val userMatches = userDao.searchByPrefix(code, lower, limit = 4).map { it.word }
        val builtIn = coreWords[code].orEmpty()
        val prefixMatches = builtIn.filter { it.lowercase().startsWith(lower) && it.lowercase() != lower }
        val fuzzyMatches = if (prefixMatches.size < 2 && lower.length >= 3) {
            builtIn.filter { candidate ->
                abs(candidate.length - lower.length) <= 1 &&
                    editDistance(candidate.lowercase(), lower) == 1
            }
        } else {
            emptyList()
        }

        val preserveUpper = trimmed.firstOrNull()?.isUpperCase() == true
        return (userMatches + prefixMatches + fuzzyMatches)
            .distinctBy { it.lowercase() }
            .take(3)
            .map { w ->
                if (preserveUpper && w.isNotEmpty() && !languageId.isRtl) {
                    w.replaceFirstChar { it.uppercaseChar() }
                } else {
                    w
                }
            }
    }

    suspend fun getAutoCorrectCandidate(
        languageId: LanguageId,
        word: String,
    ): String? {
        if (word.length < 3) return null
        val lower = word.lowercase()
        val userExact = userDao.findWord(lower, languageId.code)
        if (userExact != null) return null
        val builtIn = coreWords[languageId.code].orEmpty()
        if (builtIn.any { it.lowercase() == lower }) return null
        val best = builtIn.firstOrNull { candidate ->
            abs(candidate.length - lower.length) <= 1 && editDistance(candidate.lowercase(), lower) == 1
        } ?: return null
        return if (word.first().isUpperCase() && !languageId.isRtl) {
            best.replaceFirstChar { it.uppercaseChar() }
        } else {
            best
        }
    }

    suspend fun learnWord(word: String, languageId: LanguageId) {
        val clean = word.trim()
        if (clean.length < 2 || clean.any { it.isDigit() }) return
        val existing = userDao.findWord(clean, languageId.code)
        val updated = existing?.copy(
            frequency = existing.frequency + 1,
            lastUsedAt = System.currentTimeMillis(),
        ) ?: UserWordEntity(
            word = clean,
            languageCode = languageId.code,
            frequency = 1,
            lastUsedAt = System.currentTimeMillis(),
        )
        userDao.upsert(updated)
    }

    private fun editDistance(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }
}
