# Clerk Android SDK - Complete API Documentation

This comprehensive documentation covers all public APIs, functions, and components of the Clerk Android SDK with detailed examples and usage instructions.

## Documentation Structure

The documentation is organized into the following sections:

### 📚 [Core SDK & Authentication](./API_DOCUMENTATION.md)
- **Getting Started**: Installation and basic setup
- **Core SDK**: Main Clerk object and configuration
- **Authentication**: Complete sign-in flow with examples
  - Email/Phone/Username authentication
  - Password authentication
  - First and second factor verification
  - Password reset functionality
  - OAuth and Enterprise SSO integration

### 📝 [Sign Up Management](./API_DOCUMENTATION_SIGNUP.md)
- **Sign-Up Process**: User registration and account creation
- **Verification**: Email and phone number verification
- **OAuth Sign-Up**: Social provider registration
- **Requirements Handling**: Managing required and optional fields
- **Complete examples**: End-to-end sign-up flows

### 👤 [User Management](./API_DOCUMENTATION_USER.md)
- **User Profile**: Getting and updating user information
- **Password Management**: Changing and deleting passwords
- **Profile Images**: Upload and manage profile pictures
- **Session Management**: Active and historical sessions
- **Email & Phone Management**: Adding and verifying contact methods
- **Passkey Management**: Creating and managing passkeys
- **External Accounts**: OAuth account connections
- **Multi-Factor Authentication**: TOTP and backup codes
- **Account Deletion**: Permanent account removal

### 🔐 [Sessions, OAuth & Additional Components](./API_DOCUMENTATION_SESSIONS_OAUTH.md)
- **Session Management**: Session lifecycle and token handling
- **OAuth Providers**: All supported social providers
- **Google Authentication**: One Tap and Credential Manager
- **Email Address Management**: Verification and management
- **Phone Number Management**: SMS verification and 2FA settings
- **Passkeys**: Passwordless authentication
- **Error Handling**: Comprehensive error management

### ⚙️ [Constants, Configuration & Examples](./API_DOCUMENTATION_CONSTANTS.md)
- **Constants**: All SDK constants and configuration options
- **Complete Integration Examples**: Real-world implementation patterns
- **Advanced Session Management**: Multi-session handling
- **Custom Error Handling**: Robust error management strategies
- **Reactive State Management**: State observation patterns
- **Best Practices**: Security, performance, and development guidelines

## Quick Start

### 1. Installation

Add to your `build.gradle(.kts)`:

```gradle
dependencies {
    implementation 'com.clerk:clerk-android:<latest-version>'
}
```

### 2. Initialize

In your Application class:

```kotlin
import com.clerk.Clerk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Clerk.initialize(
            context = this,
            publishableKey = "pk_test_your_publishable_key_here"
        )
    }
}
```

### 3. Check Authentication State

```kotlin
if (Clerk.isSignedIn) {
    // User is signed in
    val user = Clerk.user
} else {
    // Show sign-in UI
}
```

## Key Features Covered

### ✅ Authentication Methods
- **Email/Phone/Username** with password
- **Email/SMS verification codes**
- **OAuth providers** (Google, Facebook, GitHub, etc.)
- **Passkeys** for passwordless authentication
- **Multi-factor authentication** (TOTP, SMS, Backup codes)
- **Enterprise SSO** integration

### ✅ User Management
- **Profile management** (name, email, phone, image)
- **Password management** (update, delete)
- **Contact method verification**
- **Security settings** (MFA, passkeys)
- **Session management** across devices
- **Account deletion**

### ✅ Advanced Features
- **Reactive state management**
- **Comprehensive error handling**
- **Session token management**
- **Device attestation**
- **Multi-session support**
- **Deep linking support**

### ✅ Developer Experience
- **Type-safe APIs** with Kotlin
- **Coroutine support** for async operations
- **Comprehensive examples** for all features
- **Error handling patterns**
- **Best practices** and security guidelines

## API Reference Summary

### Core Objects

| Object | Description | Key Methods |
|--------|-------------|-------------|
| `Clerk` | Main SDK entry point | `initialize()`, `signOut()`, `isSignedIn` |
| `SignIn` | Sign-in flow management | `create()`, `attemptFirstFactor()`, `attemptSecondFactor()` |
| `SignUp` | Sign-up flow management | `create()`, `prepareVerification()`, `attemptVerification()` |
| `User` | User profile and data | `update()`, `updatePassword()`, `createEmailAddress()` |
| `Session` | Session management | `fetchToken()`, `revoke()`, `delete()` |
| `EmailAddress` | Email management | `prepareVerification()`, `attemptVerification()` |
| `PhoneNumber` | Phone management | `prepareVerification()`, `attemptVerification()` |
| `Passkey` | Passkey management | `create()`, `update()`, `delete()` |

### Result Handling

All async operations return `ClerkResult<T, ClerkErrorResponse>`:

```kotlin
result.onSuccess { data ->
    // Handle success
}.onFailure { error ->
    // Handle error
}
```

## Support

- **Documentation**: Complete API reference with examples
- **Error Handling**: Detailed error codes and messages
- **Best Practices**: Security and performance guidelines
- **Examples**: Real-world implementation patterns

For additional support, please refer to the [official Clerk documentation](https://clerk.com/docs) or contact support.

---

*This documentation covers Clerk Android SDK version 0.1.0 and API version 2024-10-01*