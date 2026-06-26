# ── TransPilot ProGuard Rules ──

# Keep Room entities (reflection-based)
-keep class com.exsatsukirin.transpilot.data.TranslationRecord { *; }

# Keep data classes used with DataStore serialization
-keep class com.exsatsukirin.transpilot.data.ApiConfig { *; }

# Keep OkHttp internal classes
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep org.json (used by LlmClient)
-keep class org.json.** { *; }

# Keep annotation information
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep coroutines debug info in debug, strip in release
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# AndroidX
-keep class androidx.lifecycle.** { *; }
-keep class androidx.room.** { *; }
-keep class androidx.paging.** { *; }
-keep class androidx.datastore.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
