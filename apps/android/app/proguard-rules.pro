# Proguard / R8 rules for Centwise Android

# Keep JNA classes and methods for UniFFI FFI bindings
-keepclassmembers class * extends com.sun.jna.Structure {
    <fields>;
}
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# Keep UniFFI generated classes and interfaces
-keep class com.centwise.core.uniffi.** { *; }
-keep interface com.centwise.core.uniffi.** { *; }

# Kotlin Coroutines and Compose rules
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.Signal
-dontwarn sun.misc.SignalHandler
