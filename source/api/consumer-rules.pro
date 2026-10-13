# Clerk Android SDK Consumer ProGuard Rules
# These rules are bundled with the SDK and automatically applied to consumer apps

# Keep serializer accessors for reachable Clerk models. Kotlinx serialization and Retrofit ship
# the remaining reflection rules required by their runtimes.
-keepclassmembers,allowoptimization,allowobfuscation class com.clerk.api.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit creates the service interfaces through java.lang.reflect.Proxy, so R8 full mode sees no instantiation of a
# service whose methods the consuming app never calls and rewrites the cast in ClerkApi.configure into an unconditional
# ClassCastException. Retrofit's own conditional keep only fires for interfaces with live annotated methods.
-keep,allowobfuscation interface com.clerk.api.network.api.*

# ClerkApiResultCallAdapterFactory and ClerkApiResultConverterFactory read these as ParameterizedType; R8 full mode strips
# the generic signature of any class that is not kept, which reduces them to raw types and breaks converter lookup.
-keep,allowobfuscation,allowshrinking class com.clerk.api.network.serialization.ClerkResult
-keep,allowobfuscation,allowshrinking class com.clerk.api.network.ClerkPaginatedResponse
-keep,allowobfuscation,allowshrinking class com.clerk.api.network.model.response.ClientPiggybackedResponse
