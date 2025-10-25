# Add project specific ProGuard rules here.
-keep class io.github.clashverge.mobile.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

