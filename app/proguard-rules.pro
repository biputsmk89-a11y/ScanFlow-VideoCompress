# CompressFlow ProGuard Rules

# Keep Media3 classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
