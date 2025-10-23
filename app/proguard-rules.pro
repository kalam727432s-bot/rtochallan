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
# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE


##################################################
# ⚙️ General Android / AppCompat / CardView
##################################################

# Keep resource identifiers (R files)
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep core Android components
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment

# AppCompat / CardView / AndroidX UI
-keep class androidx.appcompat.** { *; }
-dontwarn androidx.appcompat.**
-keep class androidx.cardview.** { *; }
-dontwarn androidx.cardview.**

##################################################
# ⚡ Kotlin (stdlib)
##################################################
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.Metadata { *; }

# Avoid removing inline functions and metadata
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

##################################################
# 🌐 OkHttp (HTTP client)
##################################################
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes Signature, *Annotation*

# Needed for TLS/SSL reflection in OkHttp
-keepnames class okhttp3.internal.platform.**

##################################################
# 🔌 Socket.IO Client (io.socket:socket.io-client)
##################################################
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keep interface io.socket.** { *; }

# JSON library (used internally)
-dontwarn org.json.**
-keep class org.json.** { *; }

##################################################
# 🧪 JUnit / Espresso (for testing)
##################################################
-dontwarn org.junit.**
-dontwarn androidx.test.**
-dontwarn androidx.test.espresso.**
-keep class org.junit.** { *; }
-keep class androidx.test.** { *; }
-keep class androidx.test.espresso.** { *; }

##################################################
# 🧩 Misc / Safety
##################################################
# Keep annotations
-keepattributes *Annotation*

# Suppress warnings from generated / system classes
-dontwarn javax.annotation.**
-dontwarn sun.misc.**

# Keep your Application and MainActivity entry points
-keep class **.MainActivity { *; }
-keep class **.MyApplication { *; }

# Remove all Log.* calls in release builds (optional optimization)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
