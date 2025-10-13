# HELLDECK ProGuard Rules

# Keep all classes in the main package
-keep class com.helldeck.** { *; }

# Keep Room database entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
   <init>(...);
}
-keep @androidx.room.Dao class *
-keepclassmembers class * {
   @androidx.room.Query <methods>;
}

# Keep Gson models and annotations
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
   @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
   @com.google.gson.annotations.Expose <fields>;
}

# Keep Kotlin metadata and reflection
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
   ** Companion;
}
-keepclasseswithmembernames class * {
   kotlin.jvm.internal.*;
}

# Keep SnakeYAML classes
-keep class org.yaml.snakeyaml.** { *; }

# Keep Android system classes
-keep class android.app.admin.DeviceAdminReceiver
-keep class androidx.core.content.FileProvider

# Keep Compose UI classes
-keep class androidx.compose.** { *; }

# Keep lifecycle classes
-keep class androidx.lifecycle.** { *; }

# Keep navigation classes
-keep class androidx.navigation.** { *; }

# Keep datastore classes
-keep class androidx.datastore.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep parcelable classes
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep serializable classes
-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enum classes and values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep annotation classes
-keep @interface * { *; }

# Keep activity classes
-keep class * extends android.app.Activity {
   public void *(android.view.View);
}

# Keep service classes
-keep class * extends android.app.Service {
   public <init>();
}

# Keep broadcast receiver classes
-keep class * extends android.content.BroadcastReceiver {
   public void *(android.content.Context, android.content.Intent);
}

# Keep content provider classes
-keep class * extends android.content.ContentProvider {
   public void *(android.content.Context);
   public android.database.Cursor *(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String);
   public java.lang.String *(android.net.Uri, java.lang.String);
   public int *(android.net.Uri, android.content.ContentValues, java.lang.String);
   public int *(android.net.Uri, java.lang.String, java.lang.String[]);
   public android.net.Uri *(android.net.Uri, android.content.ContentValues);
   public int *(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[]);
}

# Keep view classes
-keep class * extends android.view.View {
   public <init>(android.content.Context);
   public <init>(android.content.Context, android.util.AttributeSet);
   public <init>(android.content.Context, android.util.AttributeSet, int);
   public void set*(...);
}

# Keep fragment classes
-keep class * extends androidx.fragment.app.Fragment {
   public void *(android.view.View, android.os.Bundle);
   public void *(android.content.Context);
   public void *(android.os.Bundle);
}

# Keep coroutine classes
-keep class kotlinx.coroutines.** { *; }

# Keep data classes
-keepclassmembers class * extends java.lang.Object {
   <fields>;
   <methods>;
}

# Keep sealed classes
-keep class * extends java.lang.Object {
   public static *** valueOf(java.lang.String);
   public static *** values();
}

# Optimization rules
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.compose.**
-dontwarn com.google.android.material.**
-dontwarn com.google.android.play.**

# Remove debug information in release builds
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Keep line number information for debugging (remove in production)
-keepattributes LineNumberTable

# Keep exception information
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*

# Enable obfuscation
-obfuscate

# Optimization passes
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# Remove unused code
-whyareyoukeeping class *
-printusage unused.txt