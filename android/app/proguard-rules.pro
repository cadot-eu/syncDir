# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.jcraft.jsch.** { *; }
-keep class org.bouncycastle.** { *; }
