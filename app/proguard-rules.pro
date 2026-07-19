# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard-default.txt file.

# Keep all Activity classes
-keep class com.sidjpd.picturesoundpanels.** { *; }

# Keep Google Play Billing library
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Keep Android framework classes used via reflection
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
