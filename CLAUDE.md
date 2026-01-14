# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

The Clerk Android SDK is a modular authentication SDK for Android applications. It provides two main artifacts:
- **`clerk-android-api`** (`source/api`) - Core API and authentication logic
- **`clerk-android-ui`** (`source/ui`) - Prebuilt Jetpack Compose UI components (includes API)

The SDK enables user management with sign-up, sign-in, MFA, passkeys, OAuth/SSO, and profile management.

## Common Development Commands

### Building

```bash
# Build the entire project
./gradlew build

# Build specific modules
./gradlew :source:api:build
./gradlew :source:ui:build

# Assemble without tests
./gradlew assemble
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :source:api:test
./gradlew :source:ui:test

# Run a single test class
./gradlew :source:api:test --tests "com.clerk.sdk.SpecificTest"

# Run UI snapshot tests (Paparazzi)
./gradlew :source:ui:testDebug
./gradlew :source:ui:recordPaparazziDebug  # Update snapshots

# Run Android instrumentation tests
./gradlew connectedAndroidTest
./gradlew :source:api:connectedDebugAndroidTest
```

### Code Quality

```bash
# Format code (must pass before commit)
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck

# Run detekt static analysis
./gradlew detekt

# Run Android lint
./gradlew lint
./gradlew :source:api:lintDebug
```

### Documentation

```bash
# Generate Dokka API documentation (outputs to docs/)
./gradlew dokkaGenerate
./gradlew dokkaGenerateHtml
```

### Running Samples

```bash
# Build and install sample apps
./gradlew :samples:quickstart:installDebug
./gradlew :samples:custom-flows:installDebug
./gradlew :samples:linear-clone:installDebug
./gradlew :samples:prebuilt-ui:installDebug
```

**Note:** Before running samples, update Clerk publishable keys in `gradle.properties`:
- `QUICKSTART_CLERK_PUBLISHABLE_KEY`
- `CUSTOM_FLOWS_CLERK_PUBLISHABLE_KEY`
- `LINEAR_CLONE_CLERK_PUBLISHABLE_KEY`
- `PREBUILT_UI_CLERK_PUBLISHABLE_KEY`

### Publishing (Maintainers Only)

```bash
# Publish to Maven Central
./gradlew :source:api:publishToMavenCentral
./gradlew :source:ui:publishToMavenCentral
```

## Architecture Overview

### Module Structure

```
clerk-android/
├── source/
│   ├── api/              # Core authentication logic (clerk-android-api)
│   ├── ui/               # Jetpack Compose UI components (clerk-android-ui)
│   └── telemetry/        # Kotlin Multiplatform telemetry
├── samples/              # Example implementations
│   ├── quickstart/       # Basic integration
│   ├── custom-flows/     # Advanced custom auth flows
│   ├── linear-clone/     # OAuth/Passkey/Email flows with Compose navigation
│   └── prebuilt-ui/      # Prebuilt UI component examples
└── workbench/            # Internal development tools
```

### API Module (`source/api`)

**Core Architecture:**
- Retrofit 2 for HTTP with OkHttp middleware pipeline
- Kotlinx Serialization with snake_case naming
- Result-based error handling via `ClerkResult<T, E>` sealed interface (no exceptions)
- Reactive state management with Kotlin StateFlows

**Key Components:**

1. **`Clerk` (singleton object)** - Main SDK entry point at `com.clerk.sdk.Clerk`
   - Initialize: `Clerk.initialize(publishableKey, context)`
   - Reactive state: `Clerk.sessionFlow`, `Clerk.userFlow`, `Clerk.isInitialized`
   - Lazy initialization with error recovery

2. **Authentication Flow Classes:**
   - `SignIn` - Sign-in state machine with factor verification
   - `SignUp` - Sign-up state machine with field collection
   - `Session` - Active session management with JWT token handling
   - `User` - User profile and account management

3. **Network Layer:**
   - `ClerkApi` - Retrofit service configuration
   - `network/api/*` - Service interfaces (ClientApi, SignInApi, SignUpApi, SessionApi, UserApi, etc.)
   - `network/middleware/*` - Request/response interceptors (versioning, client syncing, device attestation)
   - `network/model/*` - Serializable data models (Client, User, Session, Factor, Verification, etc.)

4. **Authentication Features:**
   - `passkeys/` - WebAuthn/Passkey via Google Credential Manager
   - `sso/` - OAuth/SSO providers (Google One Tap, enterprise SSO)
   - `organizations/` - Multi-tenant organization support
   - `session/` - JWT token caching and refresh

**Result Handling Pattern:**
```kotlin
when (val result = signIn.attemptFirstFactor(...)) {
    is ClerkResult.Success -> // Handle success
    is ClerkResult.Failure -> // Handle error
}
```

### UI Module (`source/ui`)

**Core Architecture:**
- Jetpack Compose with Material Design 3
- MVVM pattern with ViewModels per screen
- CompositionLocal for dependency injection (`LocalAuthState`, `LocalTelemetryCollector`)
- Custom navigation using NavBackStack (no NavController)
- Paparazzi for snapshot testing

**Key Components:**

1. **Authentication Screens:**
   - `AuthView` - Main navigation container with slide animations
   - `AuthStartView` - Initial identifier entry (email/username/phone)
   - `signin/*` - Sign-in factor screens (password, email code, phone code, passkey, OTP, backup code)
   - `signup/*` - Sign-up flow (collect fields, verify email/phone, complete profile)

2. **User Management Screens:**
   - `userprofile/*` - Profile management (account, email, phone, security, MFA)
   - `userbutton/*` - User menu dropdown component

3. **Reusable Components:**
   - `core/` - Input fields, buttons, avatars, navigation components
   - `theme/` - Material Design theming with dynamic color support

4. **State Management:**
   - `AuthState` - Local UI state (form inputs, navigation, back stack)
   - `AuthStartViewModel` - Main authentication orchestration
   - ViewModels map API `ClerkResult` to UI states (Idle, Loading, Error, Success)

### Authentication Flow Architecture

**Sign-In Flow:**
```
AuthStartView → SignIn.create(identifier)
             → First Factor (password/code/passkey)
             → Optional Second Factor (MFA)
             → Session Created
```

**Sign-Up Flow:**
```
AuthStartView → SignUp.create(fields)
             → Collect Required Fields
             → Verify Email/Phone
             → Complete Profile
             → Session Created
```

**Key Patterns:**
- **Multi-factor authentication** - First factor required, second factor optional based on configuration
- **Extensible verification** - Supports password, email_code, phone_code, passkey, totp, backup_code
- **OAuth/SSO** - Web-based flows via RedirectConfiguration, native Google One Tap
- **Error recovery** - SignUp fallback when SignIn fails with "identifier_not_found"

### Network Middleware Pipeline

**Outgoing (Request):**
- `VersioningUserAgentMiddleware` - Adds SDK version to User-Agent
- `UrlAppendingMiddleware` - Appends proxy URL if configured
- `DeviceAssertionMiddleware` - Adds device attestation headers

**Incoming (Response):**
- `ClientSyncingMiddleware` - Extracts and syncs client state from API responses
- `DeviceTokenSavingMiddleware` - Persists device attestation tokens

### Testing Structure

**Unit Tests (JVM with Robolectric):**
- `source/api/src/test/` - Network serialization, passkeys, SSO, storage, configuration
- `source/ui/src/test/` - ViewModels, theme, components, navigation

**UI Snapshot Tests (Paparazzi):**
- `source/ui/src/test/` - Visual regression testing of Compose components
- Update snapshots: `./gradlew :source:ui:recordPaparazziDebug`

**Instrumentation Tests:**
- `source/*/src/androidTest/` - Android-specific integration tests

## Development Practices

### Code Style
- **Kotlin code style:** Official Kotlin style guide
- **Formatting:** ktfmt Google Style (enforced via Spotless)
- **Linting:** Detekt with all rules enabled
- Must pass `./gradlew spotlessCheck detekt` before committing

### Adding New API Endpoints

1. Define data models in `source/api/src/main/java/com/clerk/sdk/network/model/`
2. Add Retrofit service interface in `source/api/src/main/java/com/clerk/sdk/network/api/`
3. Add serialization tests in `source/api/src/test/.../network/`
4. Expose through `Clerk` object or extension functions on domain models

### Adding New UI Components

1. Create Composable in appropriate package (`signin/`, `signup/`, `core/`, etc.)
2. Add ViewModel if stateful, inheriting from `androidx.lifecycle.ViewModel`
3. Use `LocalAuthState` for accessing authentication state
4. Add Paparazzi snapshot tests in `source/ui/src/test/`
5. Update snapshots with `./gradlew :source:ui:recordPaparazziDebug`

### Working with ClerkResult

Always use pattern matching instead of exceptions:
```kotlin
when (val result = apiCall()) {
    is ClerkResult.Success -> result.data // Access successful result
    is ClerkResult.Failure -> result.error // Handle error
}
```

### Key Design Patterns in Use

- **Singleton** - `Clerk`, `ClerkApi` for global SDK state
- **Sealed Classes** - `ClerkResult<T, E>` for type-safe error handling
- **Extension Functions** - `SignIn.create()`, `SignUp.create()` for idiomatic Kotlin
- **StateFlow** - Reactive session, user state throughout SDK
- **CompositionLocal** - Dependency injection in Compose tree
- **Middleware/Interceptor** - OkHttp interceptors for cross-cutting concerns
- **Result Type** - No checked exceptions, explicit error handling

## Important Dependencies

**API Module:**
- Retrofit 2 + OkHttp (HTTP)
- Kotlinx Serialization (JSON with snake_case)
- Google Play Services (OAuth, Credentials, Play Integrity)
- Android Credentials Manager (Passkeys)
- JWT Decode

**UI Module:**
- Jetpack Compose + Material 3
- Androidx Navigation 3
- Coil (image loading)
- libphonenumber (phone validation)
- MaterialKolor (dynamic theming)
- Paparazzi (UI testing)

## References

- [Clerk Android Documentation](https://clerk.com/docs/quickstarts/android)
- [API Reference](https://clerk-android.clerkstage.dev)
- [Clerk Docs](https://clerk.com/docs)
- [GitHub Repository](https://github.com/clerk/clerk-android)
