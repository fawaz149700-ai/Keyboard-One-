# Keep InputMethodService and Activities
-keep class com.example.ime.KhattInputMethodService { *; }
-keep class com.example.ui.BiometricUnlockActivity { *; }
-keep class com.example.MainActivity { *; }

# Keep kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.model.**$$serializer { *; }
-keepclassmembers class com.example.model.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.example.data.**$$serializer { *; }
-keepclassmembers class com.example.data.** {
    *** Companion;
}
