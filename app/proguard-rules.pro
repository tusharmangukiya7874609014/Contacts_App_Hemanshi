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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

#glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

#circulerImageView
-keep class de.hdodenhof.circleimageview.** { *; }

# Room Database
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# AndroidX Lifecycle
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }

-keep class com.contactshandlers.contactinfoall.model.** { *; }
-keep class com.contactshandlers.contactinfoall.helper.Constants { *; }

#room
-dontwarn android.arch.util.paging.CountedDataSource
-dontwarn android.arch.persistence.room.paging.LimitOffsetDataSource

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepattributes *Annotation*

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Database
-keep class com.google.firebase.database.** { *; }

# Allow Reflection for Retrofit, Firebase, Glide, and Room
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Facebook Shimmer
-keep class com.facebook.shimmer.** { *; }

# Keep Ads SDK (AdMob)
-keep class com.google.ads.** { *; }
-keep class com.google.android.gms.ads.** { *; }

#keep Local
-keep class com.contactshandlers.contactinfoall.helper.LocaleHelper { *; }