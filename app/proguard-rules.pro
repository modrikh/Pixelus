# Keep data classes for serialization
-keepclassmembers class com.pixelus.music.data.** { *; }

# Keep Ktor classes
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.pixelus.music.**$$serializer { *; }
-keepclassmembers class com.pixelus.music.** {
    *** Companion;
}
-keepclasseswithmembers class com.pixelus.music.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Compose
-dontwarn androidx.compose.**
