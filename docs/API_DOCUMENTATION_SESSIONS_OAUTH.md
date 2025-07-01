# Clerk Android SDK - Sessions, OAuth & Additional Components

## Session Management

The `Session` object represents an HTTP session and models the period of information exchange between a user and the server.

### Session Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique identifier for the session |
| `status` | `SessionStatus` | Current state of the session |
| `expireAt` | `Long` | Session expiration timestamp |
| `abandonAt` | `Long` | Session abandonment timestamp |
| `lastActiveAt` | `Long` | Last activity timestamp |
| `latestActivity` | `SessionActivity?` | Latest session activity details |
| `lastActiveOrganizationId` | `String?` | Last active organization ID |
| `actor` | `String?` | JWT actor for the session |
| `user` | `User?` | User associated with the session |
| `publicUserData` | `PublicUserData?` | Public user information |
| `createdAt` | `Long` | Session creation timestamp |
| `updatedAt` | `Long` | Last update timestamp |
| `lastActiveToken` | `TokenResource?` | Last active token |

### Session Status

```kotlin
enum class SessionStatus {
    ABANDONED,    // Session abandoned client-side
    ACTIVE,       // Session is valid and active
    ENDED,        // User signed out but session remains in client
    EXPIRED,      // Session period has passed
    REMOVED,      // User signed out and session removed from client
    REPLACED,     // Session replaced by another but remains in client
    REVOKED,      // Application ended session and removed from client
    UNKNOWN       // Unknown session status
}
```

### Session Methods

#### `suspend fun Session.fetchToken(options: SessionGetTokenOptions): TokenResource?`

Fetches a fresh JWT for the session.

**Parameters:**
```kotlin
SessionGetTokenOptions(
    template = "custom_template", // Optional template name
    leewayInSeconds = 60         // Optional leeway for token validation
)
```

**Example:**
```kotlin
lifecycleScope.launch {
    val token = Clerk.session?.fetchToken()
    
    token?.let { tokenResource ->
        // Use the JWT token for API calls
        val jwt = tokenResource.jwt
        makeAuthenticatedApiCall(jwt)
    }
}
```

#### `suspend fun Session.revoke(): ClerkResult<Session, ClerkErrorResponse>`

Revokes the current session.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.session?.revoke()
    
    result?.onSuccess { session ->
        // Session revoked successfully
        showMessage("Session revoked")
    }?.onFailure { error ->
        // Handle revocation error
        showError("Failed to revoke session: ${error.message}")
    }
}
```

#### `suspend fun Session.delete()`

Deletes the current session.

**Example:**
```kotlin
lifecycleScope.launch {
    Clerk.session?.delete()
    // Session deleted
}
```

### Session Activity

The `SessionActivity` object provides information about the user's location, device, and browser.

#### SessionActivity Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique identifier for the activity record |
| `browserName` | `String?` | Browser name |
| `browserVersion` | `String?` | Browser version |
| `deviceType` | `String?` | Device type |
| `ipAddress` | `String?` | IP address |
| `city` | `String?` | City (resolved by IP geolocation) |
| `country` | `String?` | Country (resolved by IP geolocation) |
| `isMobile` | `Boolean?` | Whether from mobile device |

### Session Utility Properties

#### `Session.isThisDevice: Boolean`

Convenience property to check if the session is from the current device.

**Example:**
```kotlin
Clerk.user?.allSessions()?.onSuccess { sessions ->
    sessions.forEach { session ->
        val deviceLabel = if (session.isThisDevice) "This device" else "Other device"
        displaySession(session, deviceLabel)
    }
}
```

---

## OAuth & SSO

### OAuth Providers

The `OAuthProvider` enum represents supported OAuth providers for authentication.

#### Supported Providers

| Provider | Description |
|----------|-------------|
| `GOOGLE` | Google OAuth authentication |
| `FACEBOOK` | Facebook OAuth authentication |
| `GITHUB` | GitHub OAuth authentication |
| `MICROSOFT` | Microsoft OAuth authentication |
| `APPLE` | Apple OAuth authentication |
| `TWITTER` | Twitter OAuth authentication |
| `DISCORD` | Discord OAuth authentication |
| `LINKEDIN` | LinkedIn OAuth authentication |
| `LINKEDIN_OIDC` | LinkedIn OpenID Connect |
| `DROPBOX` | Dropbox OAuth authentication |
| `SPOTIFY` | Spotify OAuth authentication |
| `SLACK` | Slack OAuth authentication |
| `NOTION` | Notion OAuth authentication |
| `GITLAB` | GitLab OAuth authentication |
| `BITBUCKET` | Bitbucket OAuth authentication |
| `ATLASSIAN` | Atlassian OAuth authentication |
| `HUBSPOT` | HubSpot OAuth authentication |
| `TIKTOK` | TikTok OAuth authentication |
| `TWITCH` | Twitch OAuth authentication |
| `INSTAGRAM` | Instagram OAuth authentication |
| `COINBASE` | Coinbase OAuth authentication |
| `XERO` | Xero OAuth authentication |
| `BOX` | Box OAuth authentication |
| `LINEAR` | Linear OAuth authentication |
| `HUGGING_FACE` | Hugging Face OAuth authentication |
| `LINE` | LINE OAuth authentication |
| `CUSTOM` | Custom OAuth provider |

#### OAuth Provider Methods

##### `OAuthProvider.fromStrategy(strategy: String): OAuthProvider`

Converts a strategy string to the corresponding OAuth provider.

**Example:**
```kotlin
val provider = OAuthProvider.fromStrategy("oauth_google") // Returns OAuthProvider.GOOGLE
```

#### OAuth Provider Properties

##### `OAuthProvider.providerName: String`

Gets the human-readable name of the OAuth provider.

**Example:**
```kotlin
val displayName = OAuthProvider.GOOGLE.providerName // Returns "Google"
```

##### `OAuthProvider.logoUrl: String?`

Gets the logo URL for the OAuth provider from environment configuration.

**Example:**
```kotlin
val logoUrl = OAuthProvider.GOOGLE.logoUrl
logoUrl?.let { url ->
    // Load and display the provider logo
    loadProviderLogo(url)
}
```

### OAuth Authentication

#### Sign-In with OAuth

##### `suspend fun SignIn.authenticateWithRedirect(params: AuthenticateWithRedirectParams): ClerkResult<OAuthResult, ClerkErrorResponse>`

Initiates OAuth sign-in flow.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = SignIn.authenticateWithRedirect(
        SignIn.AuthenticateWithRedirectParams.OAuth(
            provider = OAuthProvider.GOOGLE,
            redirectUrl = "https://yourapp.com/auth/callback"
        )
    )
    
    result.onSuccess { oauthResult ->
        // OAuth flow initiated
        handleOAuthRedirect(oauthResult)
    }.onFailure { error ->
        // Handle OAuth error
        showError("OAuth failed: ${error.message}")
    }
}
```

#### Sign-Up with OAuth

##### `suspend fun SignUp.authenticateWithRedirect(params: AuthenticateWithRedirectParams): ClerkResult<OAuthResult, ClerkErrorResponse>`

Initiates OAuth sign-up flow.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = SignUp.authenticateWithRedirect(
        SignUp.AuthenticateWithRedirectParams.OAuth(
            provider = OAuthProvider.GITHUB,
            emailAddress = "user@example.com",
            legalAccepted = true
        )
    )
    
    result.onSuccess { oauthResult ->
        // OAuth sign-up initiated
        handleOAuthRedirect(oauthResult)
    }.onFailure { error ->
        // Handle OAuth error
        showError("OAuth sign-up failed: ${error.message}")
    }
}
```

### Google-Specific Authentication

#### Google One Tap

##### `suspend fun SignIn.authenticateWithGoogleOneTap(): ClerkResult<OAuthResult, ClerkErrorResponse>`

Authenticates using Google One Tap.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = SignIn.authenticateWithGoogleOneTap()
    
    result.onSuccess { oauthResult ->
        // Google One Tap authentication successful
        handleAuthenticationSuccess()
    }.onFailure { error ->
        // Handle One Tap error
        showError("Google One Tap failed: ${error.message}")
    }
}
```

#### Google Credentials

##### `suspend fun SignIn.authenticateWithGoogleCredentials(context: Context): ClerkResult<OAuthResult, ClerkErrorResponse>`

Authenticates using Google Credential Manager.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = SignIn.authenticateWithGoogleCredentials(this@MainActivity)
    
    result.onSuccess { oauthResult ->
        // Google credentials authentication successful
        handleAuthenticationSuccess()
    }.onFailure { error ->
        // Handle credentials error
        showError("Google credentials failed: ${error.message}")
    }
}
```

---

## Email Address Management

The `EmailAddress` object represents an email address associated with a user.

### EmailAddress Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique identifier for the email address |
| `emailAddress` | `String` | Email address value |
| `verification` | `Verification?` | Verification status |
| `linkedTo` | `List<LinkedEntity>?` | Linked accounts or identifiers |

### EmailAddress Methods

#### `suspend fun EmailAddress.create(email: String): ClerkResult<EmailAddress, ClerkErrorResponse>`

Creates a new email address for the user.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = EmailAddress.create("newemail@example.com")
    
    result.onSuccess { emailAddress ->
        // Email address created (unverified)
        showMessage("Email address added. Please verify.")
    }.onFailure { error ->
        // Handle creation error
        showError("Failed to add email: ${error.message}")
    }
}
```

#### `suspend fun EmailAddress.prepareVerification(params: PrepareVerificationParams): ClerkResult<EmailAddress, ClerkErrorResponse>`

Prepares email address verification.

**Parameters:**
```kotlin
// Email code verification
EmailAddress.PrepareVerificationParams.EmailCode()

// Enterprise SSO verification
EmailAddress.PrepareVerificationParams.EnterpriseSSO(
    redirectUrl = "https://yourapp.com/auth/callback"
)
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = emailAddress.prepareVerification(
        EmailAddress.PrepareVerificationParams.EmailCode()
    )
    
    result?.onSuccess { emailAddress ->
        // Verification code sent
        showVerificationDialog()
    }?.onFailure { error ->
        // Handle preparation error
        showError("Failed to send verification: ${error.message}")
    }
}
```

#### `suspend fun EmailAddress.attemptVerification(code: String): ClerkResult<EmailAddress, ClerkErrorResponse>`

Attempts to verify the email address with the provided code.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = emailAddress.attemptVerification("123456")
    
    result?.onSuccess { emailAddress ->
        // Email verified successfully
        showMessage("Email verified successfully")
    }?.onFailure { error ->
        // Handle verification error
        showError("Invalid verification code: ${error.message}")
    }
}
```

#### `suspend fun EmailAddress.delete(): ClerkResult<DeletedObject, ClerkErrorResponse>`

Deletes the email address.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = emailAddress.delete()
    
    result?.onSuccess {
        // Email address deleted
        refreshEmailList()
    }?.onFailure { error ->
        // Handle deletion error
        showError("Failed to delete email: ${error.message}")
    }
}
```

---

## Phone Number Management

The `PhoneNumber` object represents a phone number associated with a user.

### PhoneNumber Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique identifier for the phone number |
| `phoneNumber` | `String` | Phone number value |
| `verification` | `Verification?` | Verification status |
| `reservedForSecondFactor` | `Boolean` | Whether reserved for 2FA |
| `defaultSecondFactor` | `Boolean` | Whether default 2FA method |
| `createdAt` | `Long?` | Creation timestamp |
| `updatedAt` | `Long?` | Last update timestamp |
| `linkedTo` | `List<String>?` | Linked identifiers |
| `backupCodes` | `List<String>?` | Backup codes |

### PhoneNumber Methods

#### `suspend fun PhoneNumber.create(phoneNumber: String): ClerkResult<PhoneNumber, ClerkErrorResponse>`

Creates a new phone number for the user.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = PhoneNumber.create("+1234567890")
    
    result.onSuccess { phoneNumber ->
        // Phone number created (unverified)
        showMessage("Phone number added. Please verify.")
    }.onFailure { error ->
        // Handle creation error
        showError("Failed to add phone: ${error.message}")
    }
}
```

#### `suspend fun PhoneNumber.prepareVerification(): ClerkResult<PhoneNumber, ClerkErrorResponse>`

Prepares phone number verification by sending SMS code.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = phoneNumber.prepareVerification()
    
    result?.onSuccess { phoneNumber ->
        // SMS code sent
        showSMSVerificationDialog()
    }?.onFailure { error ->
        // Handle preparation error
        showError("Failed to send SMS: ${error.message}")
    }
}
```

#### `suspend fun PhoneNumber.attemptVerification(code: String): ClerkResult<PhoneNumber, ClerkErrorResponse>`

Attempts to verify the phone number with the provided SMS code.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = phoneNumber.attemptVerification("123456")
    
    result?.onSuccess { phoneNumber ->
        // Phone verified successfully
        showMessage("Phone number verified successfully")
    }?.onFailure { error ->
        // Handle verification error
        showError("Invalid SMS code: ${error.message}")
    }
}
```

#### `suspend fun PhoneNumber.update(reservedForSecondFactor: Boolean?, defaultSecondFactor: Boolean?): ClerkResult<PhoneNumber, ClerkErrorResponse>`

Updates phone number settings for second factor authentication.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = phoneNumber.update(
        reservedForSecondFactor = true,
        defaultSecondFactor = true
    )
    
    result?.onSuccess { phoneNumber ->
        // Phone number updated for 2FA
        showMessage("Phone number set as default 2FA method")
    }?.onFailure { error ->
        // Handle update error
        showError("Failed to update phone settings: ${error.message}")
    }
}
```

#### `suspend fun PhoneNumber.delete(): ClerkResult<DeletedObject, ClerkErrorResponse>`

Deletes the phone number.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = phoneNumber.delete()
    
    result?.onSuccess {
        // Phone number deleted
        refreshPhoneList()
    }?.onFailure { error ->
        // Handle deletion error
        showError("Failed to delete phone: ${error.message}")
    }
}
```

---

## Passkeys

The `Passkey` object represents a passkey associated with a user for passwordless authentication.

### Passkey Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique identifier for the passkey |
| `name` | `String` | Passkey name |
| `verification` | `Verification?` | Verification details |
| `createdAt` | `Long` | Creation timestamp |
| `updatedAt` | `Long` | Last update timestamp |
| `lastUsedAt` | `Long?` | Last usage timestamp |

### Passkey Methods

#### `suspend fun Passkey.create(): ClerkResult<Passkey, ClerkErrorResponse>`

Creates a new passkey for the user.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Passkey.create()
    
    result.onSuccess { passkey ->
        // Passkey created successfully
        showMessage("Passkey created successfully")
        refreshPasskeysList()
    }.onFailure { error ->
        // Handle creation error
        showError("Failed to create passkey: ${error.message}")
    }
}
```

#### `suspend fun Passkey.update(name: String?): ClerkResult<Passkey, ClerkErrorResponse>`

Updates the passkey's display name.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = passkey.update(name = "My iPhone Passkey")
    
    result?.onSuccess { passkey ->
        // Passkey updated successfully
        showMessage("Passkey name updated")
    }?.onFailure { error ->
        // Handle update error
        showError("Failed to update passkey: ${error.message}")
    }
}
```

#### `suspend fun Passkey.delete(): ClerkResult<DeletedObject, ClerkErrorResponse>`

Deletes the passkey.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = passkey.delete()
    
    result?.onSuccess {
        // Passkey deleted successfully
        showMessage("Passkey deleted")
        refreshPasskeysList()
    }?.onFailure { error ->
        // Handle deletion error
        showError("Failed to delete passkey: ${error.message}")
    }
}
```

#### `suspend fun Passkey.attemptVerification(publicKeyCredential: String): ClerkResult<Passkey, ClerkErrorResponse>`

Attempts to verify the passkey with the provided credential.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = passkey.attemptVerification(publicKeyCredentialJson)
    
    result?.onSuccess { passkey ->
        // Passkey verified successfully
        showMessage("Passkey verified")
    }?.onFailure { error ->
        // Handle verification error
        showError("Passkey verification failed: ${error.message}")
    }
}
```

---

## Error Handling

### ClerkResult

All asynchronous operations return a `ClerkResult<T, ClerkErrorResponse>` which provides type-safe error handling.

#### Using ClerkResult

```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.get()
    
    result?.onSuccess { user ->
        // Operation successful
        displayUser(user)
    }?.onFailure { error ->
        // Operation failed
        handleError(error)
    }
}
```

#### Pattern Matching

```kotlin
lifecycleScope.launch {
    val result = SignIn.create(SignIn.CreateParams.Strategy.EmailCode("user@example.com"))
    
    when (result) {
        is ClerkResult.Success -> {
            // Handle success
            val signIn = result.value
            processSignIn(signIn)
        }
        is ClerkResult.Failure -> {
            // Handle failure
            val error = result.error
            showError(error.message)
        }
    }
}
```

### ClerkErrorResponse

The `ClerkErrorResponse` object contains detailed error information.

**Common Error Properties:**
- `message`: Human-readable error message
- `code`: Error code for programmatic handling
- `longMessage`: Detailed error description
- `meta`: Additional error metadata

**Example Error Handling:**
```kotlin
private fun handleError(error: ClerkErrorResponse) {
    when (error.code) {
        "form_password_pwned" -> {
            showError("This password has been found in a data breach. Please choose a different password.")
        }
        "form_identifier_exists" -> {
            showError("An account with this email already exists.")
        }
        "verification_failed" -> {
            showError("Verification failed. Please check your code and try again.")
        }
        else -> {
            showError(error.message ?: "An unexpected error occurred")
        }
    }
}
```