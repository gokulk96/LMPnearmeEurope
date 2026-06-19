-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.lmpnearme.europe.data.models.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
