package com.example.data.emoji

data class EmojiEntry(
    val emoji: String,
    val keywords: String,
    val supportsSkinTone: Boolean = false,
)

enum class EmojiCategory(val titleAr: String, val icon: String) {
    RECENT("الأخيرة", "🕒"),
    FACES("الوجوه", "😀"),
    ANIMALS("الحيوانات", "🦁"),
    FOOD("الطعام", "🍔"),
    ACTIVITIES("الأنشطة", "⚽"),
    TRAVEL("السفر", "✈️"),
    OBJECTS("الأشياء", "💡"),
    SYMBOLS("الرموز", "❤️"),
    FLAGS("الأعلام", "🇸🇦"),
    STICKERS("ملصقات", "✨"),
}

object EmojiCatalog {

    val skinTones = listOf("", "🏻", "🏼", "🏽", "🏾", "🏿")

    val defaultQuickEmojis = listOf(
        "😂", "❤️", "😍", "😭", "🙏", "👍", "🔥", "😊", "✨", "🥰", "🤣", "🤍", "🌹", "👏", "🎉", "😎"
    )

    val stickersAndKaomoji = listOf(
        "(◕‿◕✿)", "¯\\_(ツ)_/¯", "(╯°□°)╯︵ ┻━┻", "ʕ•ᴥ•ʔ", "(づ｡◕‿‿◕｡)づ",
        "✨ مـبـروك ✨", "❤️ أحـبـك ❤️", "🌹 صـبـاح الـخـيـر 🌹", "🌙 مـسـاء الـنـور 🌙",
        "🔥 أسـطـورة 🔥", "👏 كـفـو 👏", "🤲 اللهم آمين 🤲", "★彡 تم 彡★", "꧁ مرحباً ꧂"
    )

    val entriesByCategory: Map<EmojiCategory, List<EmojiEntry>> = mapOf(
        EmojiCategory.FACES to listOf(
            EmojiEntry("😀", "smile happy ابتسامة سعيد"),
            EmojiEntry("😃", "grin happy فرحان"),
            EmojiEntry("😄", "laugh joy ضحك"),
            EmojiEntry("😁", "beam teeth ابتسامة"),
            EmojiEntry("😆", "laughing haha ههه"),
            EmojiEntry("😅", "sweat nervous عرق"),
            EmojiEntry("🤣", "rofl laugh ضحك موت"),
            EmojiEntry("😂", "joy tears ضحك دموع"),
            EmojiEntry("🙂", "slight smile لطيف"),
            EmojiEntry("🙃", "upside down مقلوب"),
            EmojiEntry("😉", "wink غمزة"),
            EmojiEntry("😊", "blush خجل سعيد"),
            EmojiEntry("😇", "angel halo ملاك"),
            EmojiEntry("🥰", "hearts love حب"),
            EmojiEntry("😍", "heart eyes عشق"),
            EmojiEntry("🤩", "star struck انبهار"),
            EmojiEntry("😘", "kiss بوسة"),
            EmojiEntry("😋", "yummy لذيذ"),
            EmojiEntry("😛", "tongue لسان"),
            EmojiEntry("😜", "wink tongue مزح"),
            EmojiEntry("🤪", "zany crazy مجنون"),
            EmojiEntry("🤔", "thinking تفكير"),
            EmojiEntry("🤫", "shush هدوء"),
            EmojiEntry("🤭", "oops ضحكة"),
            EmojiEntry("🤗", "hug حضن"),
            EmojiEntry("😐", "neutral محايد"),
            EmojiEntry("😑", "expressionless ملل"),
            EmojiEntry("😒", "unamused انزعاج"),
            EmojiEntry("🙄", "eyeroll استغراب"),
            EmojiEntry("😔", "pensive حزين"),
            EmojiEntry("😴", "sleep نوم"),
            EmojiEntry("😷", "mask كمامة"),
            EmojiEntry("🤒", "sick مريض"),
            EmojiEntry("😎", "cool sunglasses نظارة"),
            EmojiEntry("🥳", "party احتفال"),
            EmojiEntry("🥺", "pleading رجاء"),
            EmojiEntry("😢", "cry بكاء"),
            EmojiEntry("😭", "sob بكاء شديد"),
            EmojiEntry("😤", "triumph غضب"),
            EmojiEntry("😡", "angry غاضب"),
            EmojiEntry("🤯", "mind blown صدمة"),
            EmojiEntry("😱", "scream خوف"),
            EmojiEntry("👍", "thumbs up موافق تمام", supportsSkinTone = true),
            EmojiEntry("👎", "thumbs down غير موافق", supportsSkinTone = true),
            EmojiEntry("👏", "clap تصفيق", supportsSkinTone = true),
            EmojiEntry("🙌", "raise hands فرح", supportsSkinTone = true),
            EmojiEntry("🙏", "pray thanks دعاء شكرا", supportsSkinTone = true),
            EmojiEntry("🤝", "handshake مصافحة", supportsSkinTone = true),
            EmojiEntry("✌️", "peace نصر", supportsSkinTone = true),
            EmojiEntry("👌", "ok ممتاز", supportsSkinTone = true),
            EmojiEntry("👋", "wave سلام باي", supportsSkinTone = true),
            EmojiEntry("💪", "muscle قوة", supportsSkinTone = true),
        ),
        EmojiCategory.ANIMALS to listOf(
            EmojiEntry("🦁", "lion أسد"), EmojiEntry("🐯", "tiger نمر"), EmojiEntry("🐱", "cat قطة"),
            EmojiEntry("🐶", "dog كلب"), EmojiEntry("🐺", "wolf ذئب"), EmojiEntry("🦊", "fox ثعلب"),
            EmojiEntry("🐻", "bear دب"), EmojiEntry("🐼", "panda باندا"), EmojiEntry("🐨", "koala كوالا"),
            EmojiEntry("🐴", "horse حصان"), EmojiEntry("🐪", "camel جمل"), EmojiEntry("🦅", "eagle نسر"),
            EmojiEntry("🕊️", "dove حمامة"), EmojiEntry("🦋", "butterfly فراشة"), EmojiEntry("🐝", "bee نحلة"),
            EmojiEntry("🐬", "dolphin دولفين"), EmojiEntry("🐳", "whale حوت"), EmojiEntry("🦈", "shark قرش"),
            EmojiEntry("🌹", "rose وردة"), EmojiEntry("🌸", "blossom زهرة"), EmojiEntry("🌴", "palm نخلة"),
            EmojiEntry("🌿", "herb نبات"), EmojiEntry("🍂", "leaf خريف"), EmojiEntry("🌙", "crescent هلال قمر"),
            EmojiEntry("⭐", "star نجمة"), EmojiEntry("☀️", "sun شمس"), EmojiEntry("🌈", "rainbow قوس قزح"),
        ),
        EmojiCategory.FOOD to listOf(
            EmojiEntry("☕", "coffee قهوة"), EmojiEntry("🍵", "tea شاي"), EmojiEntry("🧃", "juice عصير"),
            EmojiEntry("🍎", "apple تفاح"), EmojiEntry("🍊", "orange برتقال"), EmojiEntry("🍋", "lemon ليمون"),
            EmojiEntry("🍉", "watermelon بطيخ"), EmojiEntry("🍇", "grapes عنب"), EmojiEntry("🍓", "strawberry فراولة"),
            EmojiEntry("🥭", "mango مانجو"), EmojiEntry("🍌", "banana موز"), EmojiEntry("🥑", "avocado أفوكادو"),
            EmojiEntry("🍕", "pizza بيتزا"), EmojiEntry("🍔", "burger برجر"), EmojiEntry("🍟", "fries بطاطس"),
            EmojiEntry("🌮", "taco تاكو"), EmojiEntry("🍗", "chicken دجاج"), EmojiEntry("🍚", "rice أرز"),
            EmojiEntry("🍰", "cake كيك"), EmojiEntry("🍫", "chocolate شوكولاتة"), EmojiEntry("🍩", "donut دونات"),
            EmojiEntry("🍯", "honey عسل"), EmojiEntry("🥐", "croissant كرواسون"), EmojiEntry("🥖", "bread خبز"),
        ),
        EmojiCategory.ACTIVITIES to listOf(
            EmojiEntry("⚽", "soccer football كرة قدم"), EmojiEntry("🏀", "basketball سلة"),
            EmojiEntry("🎾", "tennis تنس"), EmojiEntry("🏐", "volleyball طائرة"),
            EmojiEntry("🎮", "gaming ألعاب"), EmojiEntry("🕹️", "joystick تحكم"),
            EmojiEntry("🎯", "target هدف"), EmojiEntry("🏆", "trophy كأس فوز"),
            EmojiEntry("🥇", "gold medal ذهب"), EmojiEntry("🎨", "art رسم فن"),
            EmojiEntry("🎬", "movie سينما"), EmojiEntry("🎵", "music موسيقى"),
            EmojiEntry("🎤", "mic ميكروفون"), EmojiEntry("🎧", "headphones سماعات"),
            EmojiEntry("🎉", "party احتفال"), EmojiEntry("🎊", "confetti تهنئة"),
            EmojiEntry("🎁", "gift هدية"), EmojiEntry("🎈", "balloon بالون"),
        ),
        EmojiCategory.TRAVEL to listOf(
            EmojiEntry("✈️", "airplane سفر طائرة"), EmojiEntry("🚀", "rocket صاروخ"),
            EmojiEntry("🚗", "car سيارة"), EmojiEntry("🚕", "taxi تاكسي"),
            EmojiEntry("🚌", "bus باص"), EmojiEntry("🚆", "train قطار"),
            EmojiEntry("🚢", "ship سفينة"), EmojiEntry("🏍️", "motorcycle دراجة"),
            EmojiEntry("🕌", "mosque مسجد"), EmojiEntry("🕋", "kaaba كعبة مكة"),
            EmojiEntry("🏠", "home بيت منزل"), EmojiEntry("🏢", "office مبنى عمل"),
            EmojiEntry("🏖️", "beach شاطئ"), EmojiEntry("⛰️", "mountain جبل"),
            EmojiEntry("🏜️", "desert صحراء"), EmojiEntry("🗺️", "map خريطة"),
        ),
        EmojiCategory.OBJECTS to listOf(
            EmojiEntry("📱", "phone هاتف جوال"), EmojiEntry("💻", "laptop لابتوب كمبيوتر"),
            EmojiEntry("⌨️", "keyboard لوحة مفاتيح"), EmojiEntry("📷", "camera كاميرا"),
            EmojiEntry("💡", "idea bulb فكرة لمبة"), EmojiEntry("🔦", "flashlight كشاف"),
            EmojiEntry("📚", "books كتب دراسة"), EmojiEntry("📝", "note كتابة ملاحظة"),
            EmojiEntry("📌", "pin تثبيت دبوس"), EmojiEntry("📎", "clip مرفق"),
            EmojiEntry("🔒", "lock قفل أمان"), EmojiEntry("🔑", "key مفتاح"),
            EmojiEntry("💰", "money فلوس مال"), EmojiEntry("💳", "card بطاقة دفع"),
            EmojiEntry("⏰", "alarm ساعة منبه"), EmojiEntry("📅", "calendar تقويم"),
            EmojiEntry("⚙️", "settings إعدادات"), EmojiEntry("🔍", "search بحث"),
        ),
        EmojiCategory.SYMBOLS to listOf(
            EmojiEntry("❤️", "red heart قلب حب"), EmojiEntry("🧡", "orange heart برتقالي"),
            EmojiEntry("💛", "yellow heart أصفر"), EmojiEntry("💚", "green heart أخضر"),
            EmojiEntry("💙", "blue heart أزرق"), EmojiEntry("💜", "purple heart بنفسجي"),
            EmojiEntry("🤍", "white heart أبيض"), EmojiEntry("🖤", "black heart أسود"),
            EmojiEntry("💔", "broken heart مكسور"), EmojiEntry("💖", "sparkling heart لامع"),
            EmojiEntry("💯", "100 مئة ممتاز"), EmojiEntry("🔥", "fire نار حماس"),
            EmojiEntry("✨", "sparkles نجوم لمعان"), EmojiEntry("⚡", "lightning برق سرعة"),
            EmojiEntry("✅", "check صح تم"), EmojiEntry("❌", "cross خطأ إلغاء"),
            EmojiEntry("⚠️", "warning تحذير"), EmojiEntry("♾️", "infinity لانهاية"),
            EmojiEntry("♻️", "recycle تدوير"), EmojiEntry("🔔", "bell تنبيه جرس"),
        ),
        EmojiCategory.FLAGS to listOf(
            EmojiEntry("🇸🇦", "saudi arabia السعودية"), EmojiEntry("🇦🇪", "uae الإمارات"),
            EmojiEntry("🇪🇬", "egypt مصر"), EmojiEntry("🇯🇴", "jordan الأردن"),
            EmojiEntry("🇰🇼", "kuwait الكويت"), EmojiEntry("🇶🇦", "qatar قطر"),
            EmojiEntry("🇧🇭", "bahrain البحرين"), EmojiEntry("🇴🇲", "oman عمان"),
            EmojiEntry("🇲🇦", "morocco المغرب"), EmojiEntry("🇹🇳", "tunisia تونس"),
            EmojiEntry("🇩🇿", "algeria الجزائر"), EmojiEntry("🇮🇶", "iraq العراق"),
            EmojiEntry("🇸🇾", "syria سوريا"), EmojiEntry("🇱🇧", "lebanon لبنان"),
            EmojiEntry("🇵🇸", "palestine فلسطين"), EmojiEntry("🇾🇪", "yemen اليمن"),
            EmojiEntry("🇹🇷", "turkey تركيا"), EmojiEntry("🇺🇸", "usa أمريكا"),
            EmojiEntry("🇬🇧", "uk بريطانيا"), EmojiEntry("🇫🇷", "france فرنسا"),
            EmojiEntry("🇪🇸", "spain إسبانيا"), EmojiEntry("🇩🇪", "germany ألمانيا"),
            EmojiEntry("🇷🇺", "russia روسيا"), EmojiEntry("🇯🇵", "japan اليابان"),
        ),
    )

    fun applySkinTone(emoji: String, tone: String): String {
        if (tone.isEmpty()) return emoji
        val base = emoji.replace(Regex("[\uD83C\uDFFB-\uD83C\uDFFF]"), "")
        return "$base$tone"
    }

    fun search(query: String): List<EmojiEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return entriesByCategory.values.flatten().filter {
            it.keywords.lowercase().contains(q) || it.emoji.contains(q)
        }
    }
}
