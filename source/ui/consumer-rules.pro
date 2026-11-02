# Clerk ViewModels are instantiated by the lifecycle ViewModel factory.
-keepclassmembers,allowoptimization,allowobfuscation class com.clerk.ui.** extends androidx.lifecycle.ViewModel {
    <init>(...);
}
