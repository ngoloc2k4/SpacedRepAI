# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers and source file names for crash reporting (Crashlytics)
-keepattributes SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

# Room database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.data.local.dao.** { *; }
-keep class com.example.data.local.AppDatabase_Impl { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public void <init>();
}

# Moshi & JSON models
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { *; }

# Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**

# Firebase Crashlytics
-keepattributes *Annotation*,SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room entities and SRS domain models
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.domain.srs.** { *; }

# Moshi / AI / Backup DTOs
-keep class com.example.domain.io.** { *; }
-keep class com.example.domain.ai.** { *; }

# UI & Views for Robolectric Native graphics in Release unit tests
-keep class com.example.ui.** { *; }
-keep class ** extends android.view.View { *; }
