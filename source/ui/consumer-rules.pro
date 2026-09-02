# Clerk Android UI Consumer ProGuard Rules
# These rules are bundled with the SDK and automatically applied to consumer apps

# Clerk ViewModels are instantiated by the lifecycle ViewModel factory.
-keepclassmembers,allowoptimization,allowobfuscation class com.clerk.ui.** extends androidx.lifecycle.ViewModel {
    <init>(...);
}
