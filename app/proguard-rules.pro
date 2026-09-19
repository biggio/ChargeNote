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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Preserve Annotations, Signatures, and InnerClasses
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Data models
-keep class com.example.data.model.** { *; }
-keep class com.example.util.SyncResult { *; }
-keep class com.example.util.UpdateInfo { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Networking & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class kotlinx.coroutines.** { *; }

