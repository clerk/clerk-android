# Clerk Android SDK Consumer ProGuard Rules
# These rules are bundled with the SDK and automatically applied to consumer apps

# Keep serializer accessors for reachable Clerk models. Kotlinx serialization and Retrofit ship
# the remaining reflection rules required by their runtimes.
-keepclassmembers,allowoptimization,allowobfuscation class com.clerk.api.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
