# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Media3 classes
-keep class androidx.media3.** { *; }

# Keep our models
-keep class com.morningenglish.app.data.db.** { *; }