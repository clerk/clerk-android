# Clerk Android SDK - API Documentation

## Table of Contents

1. [Getting Started](#getting-started)
2. [Core SDK](#core-sdk)
3. [Authentication](#authentication)
4. [User Management](#user-management)
5. [Session Management](#session-management)
6. [Email & Phone Management](#email--phone-management)
7. [Passkeys](#passkeys)
8. [Multi-Factor Authentication](#multi-factor-authentication)
9. [OAuth & SSO](#oauth--sso)
10. [Error Handling](#error-handling)
11. [Constants & Configuration](#constants--configuration)

---

## Getting Started

### Installation

Add the Clerk Android SDK to your project's dependencies:

```gradle
dependencies {
    implementation 'com.clerk:clerk-android:<latest-version>'
}
```

### Basic Setup

Initialize the Clerk SDK in your Application class:

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

### Advanced Setup with Options

```kotlin
import com.clerk.Clerk
import com.clerk.ClerkConfigurationOptions
import com.clerk.DeviceAttestationOptions

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val options = ClerkConfigurationOptions(
            enableDebugMode = true,
            deviceAttestationOptions = DeviceAttestationOptions(
                applicationId = "com.example.app",
                cloudProjectNumber = 123456789L
            )
        )
        
        Clerk.initialize(
            context = this,
            publishableKey = "pk_test_your_publishable_key_here",
            options = options
        )
    }
}
```

---

## Core SDK

### Clerk Object

The main entry point for the Clerk SDK.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `isInitialized` | `StateFlow<Boolean>` | Reactive state indicating SDK initialization status |
| `isSignedIn` | `Boolean` | Whether a user is currently signed in |
| `user` | `User?` | The currently authenticated user |
| `session` | `Session?` | The currently active session |
| `signIn` | `SignIn?` | Current sign-in attempt, if in progress |
| `signUp` | `SignUp?` | Current sign-up attempt, if in progress |
| `client` | `Client` | Client object representing device authentication state |
| `debugMode` | `Boolean` | Whether debug mode is enabled |

#### Configuration Properties

| Property | Type | Description |
|----------|------|-------------|
| `applicationName` | `String?` | Application name from dashboard |
| `logoUrl` | `String?` | Application logo URL for authentication UI |
| `socialProviders` | `Map<String, UserSettings.SocialConfig>` | Available OAuth providers |
| `passkeyIsEnabled` | `Boolean` | Whether passkey authentication is enabled |
| `mfaIsEnabled` | `Boolean` | Whether multi-factor authentication is enabled |
| `passwordIsEnabled` | `Boolean` | Whether password authentication is enabled |
| `usernameIsEnabled` | `Boolean` | Whether username authentication is enabled |

#### Methods

##### `initialize(context: Context, publishableKey: String)`

Initializes the Clerk SDK with basic configuration.

**Parameters:**
- `context`: Application context
- `publishableKey`: Publishable key from Clerk Dashboard

**Throws:**
- `IllegalArgumentException` if publishable key format is invalid

**Example:**
```kotlin
Clerk.initialize(this, "pk_test_your_key_here")
```

##### `initialize(context: Context, publishableKey: String, options: ClerkConfigurationOptions?)`

Initializes the Clerk SDK with advanced configuration options.

**Parameters:**
- `context`: Application context
- `publishableKey`: Publishable key from Clerk Dashboard
- `options`: Additional configuration options

**Example:**
```kotlin
val options = ClerkConfigurationOptions(enableDebugMode = true)
Clerk.initialize(this, "pk_test_your_key_here", options)
```

##### `suspend fun signOut(): ClerkResult<Unit, ClerkErrorResponse>`

Signs out the currently authenticated user.

**Returns:** `ClerkResult<Unit, ClerkErrorResponse>`

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.signOut()
    result.onSuccess {
        // User signed out successfully
    }.onFailure { error ->
        // Handle sign out error
    }
}
```

### Configuration Options

#### ClerkConfigurationOptions

Data class for enabling extra SDK functionality.

```kotlin
data class ClerkConfigurationOptions(
    val enableDebugMode: Boolean = false,
    val deviceAttestationOptions: DeviceAttestationOptions? = null
)
```

#### DeviceAttestationOptions

Configuration for device attestation (used for security features).

```kotlin
data class DeviceAttestationOptions(
    val applicationId: String,
    val cloudProjectNumber: Long
)
```

**Example:**
```kotlin
val attestationOptions = DeviceAttestationOptions(
    applicationId = "com.example.app",
    cloudProjectNumber = 123456789L
)
```

---

## Authentication

### Sign In

The `SignIn` object manages the sign-in process and provides methods to navigate the authentication lifecycle.

#### SignIn Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique identifier for the sign-in |
| `status` | `SignIn.Status` | Current status of the sign-in |
| `identifier` | `String?` | Authentication identifier (email, phone, username) |
| `supportedFirstFactors` | `List<Factor>?` | Available first factor authentication methods |
| `supportedSecondFactors` | `List<Factor>?` | Available second factor authentication methods |
| `firstFactorVerification` | `Verification?` | First factor verification state |
| `secondFactorVerification` | `Verification?` | Second factor verification state |
| `userData` | `SignIn.UserData?` | User information for current sign-in |
| `createdSessionId` | `String?` | Session ID created upon completion |

#### SignIn Status

```kotlin
enum class Status {
    COMPLETE,              // Sign-in process is complete
    NEEDS_FIRST_FACTOR,    // Needs first factor verification
    NEEDS_SECOND_FACTOR,   // Needs second factor verification
    NEEDS_IDENTIFIER,      // Needs identifier
    NEEDS_NEW_PASSWORD,    // User needs to create new password
    UNKNOWN               // Unknown state
}
```

#### Creating a Sign-In

##### `suspend fun SignIn.create(params: CreateParams.Strategy): ClerkResult<SignIn, ClerkErrorResponse>`

Creates a new sign-in attempt.

**Parameters:**
- `params`: Sign-in creation strategy

**Available Strategies:**
```kotlin
// Email code strategy
SignIn.CreateParams.Strategy.EmailCode("user@example.com")

// Phone code strategy  
SignIn.CreateParams.Strategy.PhoneCode("+1234567890")

// Password strategy
SignIn.CreateParams.Strategy.Password("user@example.com", "password123")

// Username strategy
SignIn.CreateParams.Strategy.Username("username", "password123")
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = SignIn.create(
        SignIn.CreateParams.Strategy.EmailCode("user@example.com")
    )
    
    result.onSuccess { signIn ->
        // Sign-in created, check status and proceed
        when (signIn.status) {
            SignIn.Status.NEEDS_FIRST_FACTOR -> {
                // Prepare first factor verification
            }
            SignIn.Status.COMPLETE -> {
                // Sign-in complete
            }
            else -> {
                // Handle other statuses
            }
        }
    }.onFailure { error ->
        // Handle creation error
    }
}
```

#### First Factor Authentication

##### `suspend fun SignIn.prepareFirstFactor(params: PrepareFirstFactorParams): ClerkResult<SignIn, ClerkErrorResponse>`

Prepares the first factor verification.

**Parameters:**
```kotlin
// Email code preparation
SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId = "email_id")

// Phone code preparation
SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId = "phone_id")

// Password reset email code
SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId = "email_id")

// Passkey preparation
SignIn.PrepareFirstFactorParams.Passkey()
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.signIn?.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.EmailCode()
    )
    
    result?.onSuccess { signIn ->
        // Verification code sent, prompt user for code
    }?.onFailure { error ->
        // Handle preparation error
    }
}
```

##### `suspend fun SignIn.attemptFirstFactor(params: AttemptFirstFactorParams): ClerkResult<SignIn, ClerkErrorResponse>`

Attempts to verify the first factor.

**Parameters:**
```kotlin
// Email code verification
SignIn.AttemptFirstFactorParams.EmailCode("123456")

// Phone code verification
SignIn.AttemptFirstFactorParams.PhoneCode("123456")

// Password verification
SignIn.AttemptFirstFactorParams.Password("password123")

// Passkey verification
SignIn.AttemptFirstFactorParams.Passkey("public_key_credential_json")
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.signIn?.attemptFirstFactor(
        SignIn.AttemptFirstFactorParams.EmailCode("123456")
    )
    
    result?.onSuccess { signIn ->
        when (signIn.status) {
            SignIn.Status.NEEDS_SECOND_FACTOR -> {
                // Proceed to second factor
            }
            SignIn.Status.COMPLETE -> {
                // Sign-in complete
            }
            else -> {
                // Handle other statuses
            }
        }
    }?.onFailure { error ->
        // Handle verification error
    }
}
```

#### Second Factor Authentication (MFA)

##### `suspend fun SignIn.prepareSecondFactor(): ClerkResult<SignIn, ClerkErrorResponse>`

Prepares second factor verification for MFA.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.signIn?.prepareSecondFactor()
    
    result?.onSuccess { signIn ->
        // Second factor prepared, prompt for verification
    }?.onFailure { error ->
        // Handle preparation error
    }
}
```

##### `suspend fun SignIn.attemptSecondFactor(params: AttemptSecondFactorParams): ClerkResult<SignIn, ClerkErrorResponse>`

Attempts to verify the second factor.

**Parameters:**
```kotlin
// Phone code for MFA
SignIn.AttemptSecondFactorParams.PhoneCode("123456")

// TOTP code from authenticator app
SignIn.AttemptSecondFactorParams.TOTP("123456")

// Backup code
SignIn.AttemptSecondFactorParams.BackupCode("backup-code-123")
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.signIn?.attemptSecondFactor(
        SignIn.AttemptSecondFactorParams.TOTP("123456")
    )
    
    result?.onSuccess { signIn ->
        if (signIn.status == SignIn.Status.COMPLETE) {
            // Sign-in complete with MFA
        }
    }?.onFailure { error ->
        // Handle MFA verification error
    }
}
```

#### Password Reset

##### `suspend fun SignIn.resetPassword(params: ResetPasswordParams): ClerkResult<SignIn, ClerkErrorResponse>`

Resets user password during sign-in.

**Parameters:**
```kotlin
SignIn.ResetPasswordParams(
    password = "newPassword123",
    signOutOfOtherSessions = true
)
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.signIn?.resetPassword(
        SignIn.ResetPasswordParams(
            password = "newPassword123",
            signOutOfOtherSessions = true
        )
    )
    
    result?.onSuccess { signIn ->
        // Password reset successful
    }?.onFailure { error ->
        // Handle reset error
    }
}
```