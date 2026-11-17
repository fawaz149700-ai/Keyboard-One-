package com.example.data.layout

import android.text.InputType
import android.view.inputmethod.EditorInfo

enum class NumericContextType(val arabicLabel: String) {
    NONE(""),
    PHONE("حقل رقم هاتف"),
    CURRENCY_DECIMAL("حقل مبلغ مالي / رقمي"),
    PIN_PASSWORD("حقل رمز سري / تحقق"),
    DATETIME("حقل تاريخ / وقت"),
    NUMBER("حقل أرقام"),
}

object NumericFieldDetector {

    private val phoneKeywords = listOf(
        "phone", "mobile", "cell", "tel", "whatsapp", "contact",
        "هاتف", "جوال", "موبايل", "تلفون", "واتساب", "رقم الاتصال",
    )

    private val currencyKeywords = listOf(
        "amount", "price", "cost", "salary", "total", "balance", "payment", "pay",
        "iban", "card", "account", "transfer", "budget", "quantity", "qty", "zip", "postal",
        "مبلغ", "سعر", "رصيد", "راتب", "إجمالي", "دفع", "سداد", "حساب", "آيبان", "ايبان",
        "بطاقة", "حوالة", "كمية", "ريال", "دولار", "دينار", "درهم", "الرمز البريدي",
    )

    private val pinKeywords = listOf(
        "otp", "pin", "cvv", "cvc", "passcode", "verification", "verify", "2fa", "mfa",
        "digit", "secret code", "security code", "postal", "year",
        "رمز التحقق", "كود التفعيل", "كود التحقق", "الرقم السري", "رقم سري", "رمز سري",
        "كلمة مرور رقمية", "أرقام فقط", "رمز الأمان", "رمز التفعيل",
    )

    fun detect(info: EditorInfo?): NumericContextType {
        if (info == null) return NumericContextType.NONE
        val metaText = buildString {
            info.hintText?.let { append(it).append(' ') }
            info.label?.let { append(it).append(' ') }
            info.fieldName?.let { append(it).append(' ') }
            info.extras?.keySet()?.forEach { key ->
                val v = info.extras?.get(key)?.toString()
                if (!v.isNullOrBlank()) append(v).append(' ')
            }
        }
        return detectFromMetadata(info.inputType, metaText)
    }

    fun detectFromMetadata(inputType: Int, metadataText: String = ""): NumericContextType {
        val cls = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val flags = inputType and InputType.TYPE_MASK_FLAGS
        val lower = metadataText.lowercase()

        when (cls) {
            InputType.TYPE_CLASS_PHONE -> return NumericContextType.PHONE
            InputType.TYPE_CLASS_DATETIME -> return NumericContextType.DATETIME
            InputType.TYPE_CLASS_NUMBER -> {
                if (variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                    return NumericContextType.PIN_PASSWORD
                }
                if ((flags and InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0 ||
                    currencyKeywords.any { it in lower }
                ) {
                    return NumericContextType.CURRENCY_DECIMAL
                }
                if (phoneKeywords.any { it in lower }) {
                    return NumericContextType.PHONE
                }
                if (pinKeywords.any { it in lower }) {
                    return NumericContextType.PIN_PASSWORD
                }
                return NumericContextType.NUMBER
            }
            InputType.TYPE_CLASS_TEXT -> {
                val isPasswordVar = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                if (pinKeywords.any { it in lower }) {
                    return NumericContextType.PIN_PASSWORD
                }
                if (phoneKeywords.any { it in lower }) {
                    return NumericContextType.PHONE
                }
                if (currencyKeywords.any { it in lower }) {
                    return NumericContextType.CURRENCY_DECIMAL
                }
                if (isPasswordVar && (
                        "رقم" in lower || "أرقام" in lower || "pin" in lower || "digit" in lower
                    )
                ) {
                    return NumericContextType.PIN_PASSWORD
                }
            }
        }
        return NumericContextType.NONE
    }
}
