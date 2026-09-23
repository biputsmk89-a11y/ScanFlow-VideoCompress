# CompressFlow ProGuard Rules

# Keep Media3 classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Room generated code & entities
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.compressflow.app.data.local.database.** { *; }

# Keep serialization & models
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep class com.compressflow.app.domain.model.** { *; }
-keep class com.compressflow.app.data.preferences.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
