# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    volatile <fields>;
}

# --- ViewModels ---
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# --- Firebase & Google Play Services ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# --- Room & WorkManager ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.introspection.DatabaseVerificationImpl
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.background.systemjob.SystemJobService { *; }
-keep class androidx.work.impl.background.systemalarm.SystemAlarmService { *; }
-keep class androidx.room.** { *; }
-keep class androidx.work.** { *; }

# --- PRESERVAR MODELOS DE DADOS (Importante para Firebase) ---
-keepclassmembers class com.jack.friend.** {
    <fields>;
    <init>(...);
}
-keep class com.jack.friend.** { *; }

# --- WebRTC Native Libs ---
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# --- Coil & Lottie ---
-keep class coil.** { *; }
-keep class com.airbnb.lottie.** { *; }

# --- Cloudinary & Image Loaders (Ignore missing Glide/Picasso) ---
-keep class com.cloudinary.** { *; }
-dontwarn com.cloudinary.android.download.glide.**
-dontwarn com.cloudinary.android.download.picasso.**
-dontwarn com.bumptech.glide.**
-dontwarn com.squareup.picasso.**

# --- Outros avisos comuns ---
-dontwarn java.lang.instrument.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
-dontwarn sun.reflect.**
