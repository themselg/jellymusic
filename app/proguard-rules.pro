# Jellyfin SDK relies on kotlinx.serialization; keep serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Media3 / ExoPlayer
-dontwarn androidx.media3.**

# Ktor (used by the Jellyfin SDK)
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
