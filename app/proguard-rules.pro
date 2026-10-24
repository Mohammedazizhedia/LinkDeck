# Ktor / Netty internals
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn org.slf4j.**
-keepclassmembers class kotlinx.serialization.json.** { ***; }
-keepclasseswithmembers class * { kotlinx.serialization.KSerializer *; }
-keep,includedescriptorclasses class com.linkdeck.**$$serializer { *; }
-keepclassmembers class com.linkdeck.** {
    *** Companion;
}
-keepclasseswithmembers class com.linkdeck.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# ZXing core is reflection-free for our usage; keep enums referenced via hints.
-keep class com.google.zxing.** { *; }
# Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
# CameraX
-keep class androidx.camera.** { *; }
# Compose previews
-keepclassmembers class * { @androidx.compose.ui.tooling.preview.Preview *; }

# Vosk / JNA
-keep class org.vosk.** { *; }
-keep class com.alphacephei.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn org.vosk.**
-dontwarn com.sun.jna.**
-dontwarn edu.cmu.sphinx.**
-keep class io.netty.** { *; }
-keep class org.jboss.** { *; }
-dontwarn io.netty.**
-dontwarn org.jboss.**
-keepattributes Signature

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
