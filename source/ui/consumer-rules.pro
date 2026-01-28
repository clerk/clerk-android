# Clerk Android UI Consumer ProGuard Rules
# These rules are bundled with the SDK and automatically applied to consumer apps

# Keep Clerk UI classes
-keep class com.clerk.ui.** { *; }

# Keep Compose runtime classes used by the SDK
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Material Design 3 components
-keep class androidx.compose.material3.** { *; }

# Keep Coil image loading
-keep class coil3.** { *; }
-dontwarn coil3.**
