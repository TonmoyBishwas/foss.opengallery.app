# OpenGallery R8 rules.
# Keep line numbers for readable crash reports from users.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# TensorFlow Lite uses JNI; keep its runtime classes.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# osmdroid reflects on tile source/config classes.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
