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

# German Learning Widget - Production ProGuard Rules
# Comprehensive configuration for release builds

# Basic Android optimizations
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Preserve line numbers for debugging stack traces in release
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep exception handling
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod

# Keep reflection-based APIs
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations

# ==== Android Framework ====
# Keep all Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Keep AppWidget providers
-keep public class * extends android.appwidget.AppWidgetProvider
-keep class **.*Widget { *; }
-keep class **.*WidgetProvider { *; }

# ==== Kotlin Specific ====
# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.germanleraningwidget.**$$serializer { *; }
-keepclassmembers class com.germanleraningwidget.** {
    *** Companion;
}
-keepclasseswithmembers class com.germanleraningwidget.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==== Jetpack Compose ====
# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose compiler generated classes
-keep class androidx.compose.compiler.** { *; }

# Keep @Composable functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ==== Data Classes and Models ====
# Keep all data classes and their constructors
-keep @kotlinx.serialization.Serializable class ** { *; }
-keep class com.germanleraningwidget.data.model.** { *; }
-keepclassmembers class com.germanleraningwidget.data.model.** { *; }

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==== Repository Pattern ====
# Keep repository classes and their singleton instances
-keep class com.germanleraningwidget.data.repository.** { *; }
-keepclassmembers class com.germanleraningwidget.data.repository.** {
    public static ** getInstance(...);
    public static ** instance;
}

# ==== WorkManager ====
# Keep Worker classes
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class com.germanleraningwidget.worker.** { *; }

# Keep WorkManager internals
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ==== DataStore ====
# Keep DataStore preferences
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ==== ViewModels ====
# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.germanleraningwidget.ui.viewmodel.** { *; }

# ==== Dependency Injection ====
# Keep DI module
-keep class com.germanleraningwidget.di.** { *; }

# ==== Widgets ====
# Keep widget classes and their methods
-keep class com.germanleraningwidget.widget.** { *; }
-keepclassmembers class com.germanleraningwidget.widget.** { *; }

# Keep widget helper classes
-keep class com.germanleraningwidget.widget.WidgetCustomizationHelper { *; }

# ==== Logging and Debugging ====
# Keep logging classes for crash reporting
-keep class com.germanleraningwidget.util.AppLogger { *; }
-keep class com.germanleraningwidget.util.PerformanceMonitor { *; }

# ==== Native Methods ====
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ==== WebView (if used) ====
# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ==== Parcelable ====
# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ==== Optimizations ====
# Enable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-mergeinterfacesaggressively

# Remove logging in release builds (except errors)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Remove debug logging from custom logger
-assumenosideeffects class com.germanleraningwidget.util.AppLogger {
    public static void v(...);
    public static void d(...);
    public static void i(...);
}

# ==== Keep Application Class ====
-keep class com.germanleraningwidget.GermanLearningApplication { *; }

# ==== Keep MainActivity ====
-keep class com.germanleraningwidget.MainActivity { *; }

# ==== R8 Compatibility ====
# Disable R8 warnings for known issues
-dontwarn java.lang.invoke.StringConcatFactory

# ==== Testing ====
# Keep test classes when testing
-keep class androidx.test.** { *; }
-dontwarn androidx.test.**

# ==== Third-party Libraries ====
# Add rules for any third-party libraries as needed

# ==== Final Optimizations ====
# Optimize dex files  
-dontpreverify

# Additional R8 optimizations
-repackageclasses ''
-allowaccessmodification