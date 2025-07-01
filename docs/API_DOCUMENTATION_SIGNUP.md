# Clerk Android SDK - Sign Up Documentation

## Sign Up

The `SignUp` object manages the user registration process and provides methods to navigate the sign-up lifecycle.

### SignUp Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique identifier for the sign-up |
| `status` | `SignUp.Status` | Current status of the sign-up |
| `requiredFields` | `List<String>` | Fields required for completion |
| `optionalFields` | `List<String>` | Optional fields that can be provided |
| `missingFields` | `List<String>` | Required fields not yet provided |
| `unverifiedFields` | `List<String>` | Fields that need verification |
| `verifications` | `Map<String, Verification?>` | In-flight verification states |
| `username` | `String?` | Username if provided |
| `emailAddress` | `String?` | Email address if provided |
| `phoneNumber` | `String?` | Phone number if provided |
| `passwordEnabled` | `Boolean` | Whether password was provided |
| `firstName` | `String?` | First name if provided |
| `lastName` | `String?` | Last name if provided |
| `createdSessionId` | `String?` | Session ID when complete |
| `createdUserId` | `String?` | User ID when complete |

### SignUp Status

```kotlin
enum class Status {
    ABANDONED,              // Sign-up abandoned (inactive >24h)
    MISSING_REQUIREMENTS,   // Missing required fields or verification
    COMPLETE,              // Sign-up complete, user created
    UNKNOWN               // Unknown state
}
```

### Creating a Sign-Up

#### `suspend fun SignUp.create(params: CreateParams): ClerkResult<SignUp, ClerkErrorResponse>`

Creates a new sign-up attempt.

**Available Creation Strategies:**

##### Standard Sign-Up
```kotlin
SignUp.CreateParams.Standard(
    emailAddress = "user@example.com",
    password = "password123",
    firstName = "John",
    lastName = "Doe",
    username = "johndoe",
    phoneNumber = "+1234567890"
)
```

##### Empty Sign-Up (Inspect Requirements)
```kotlin
SignUp.CreateParams.None
```

##### Transfer from Sign-In
```kotlin
SignUp.CreateParams.Transfer
```

##### Google One Tap
```kotlin
SignUp.CreateParams.GoogleOneTap(token = "one_tap_token")
```

**Examples:**

```kotlin
// Standard sign-up with email and password
lifecycleScope.launch {
    val result = SignUp.create(
        SignUp.CreateParams.Standard(
            emailAddress = "user@example.com",
            password = "securePassword123",
            firstName = "John",
            lastName = "Doe"
        )
    )
    
    result.onSuccess { signUp ->
        when (signUp.status) {
            SignUp.Status.MISSING_REQUIREMENTS -> {
                // Check what's missing and proceed
                handleMissingRequirements(signUp)
            }
            SignUp.Status.COMPLETE -> {
                // Sign-up complete, user created
            }
            else -> {
                // Handle other statuses
            }
        }
    }.onFailure { error ->
        // Handle creation error
    }
}

// Create empty sign-up to inspect requirements
lifecycleScope.launch {
    val result = SignUp.create(SignUp.CreateParams.None)
    
    result.onSuccess { signUp ->
        // Inspect required and optional fields
        val requiredFields = signUp.requiredFields
        val optionalFields = signUp.optionalFields
        
        // Build UI based on requirements
        buildSignUpForm(requiredFields, optionalFields)
    }
}
```

### Updating a Sign-Up

#### `suspend fun SignUp.update(params: SignUpUpdateParams): ClerkResult<SignUp, ClerkErrorResponse>`

Updates an existing sign-up with additional information.

**Parameters:**
```kotlin
SignUp.SignUpUpdateParams.Standard(
    emailAddress = "newemail@example.com",
    password = "newPassword123",
    firstName = "Jane",
    lastName = "Smith",
    username = "janesmith",
    phoneNumber = "+0987654321"
)
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.signUp?.update(
        SignUp.SignUpUpdateParams.Standard(
            firstName = "Jane",
            lastName = "Smith"
        )
    )
    
    result?.onSuccess { signUp ->
        // Sign-up updated successfully
        checkIfComplete(signUp)
    }?.onFailure { error ->
        // Handle update error
    }
}
```

### Verification Process

#### `suspend fun SignUp.prepareVerification(strategy: PrepareVerificationParams.Strategy): ClerkResult<SignUp, ClerkErrorResponse>`

Prepares verification for email or phone number.

**Available Strategies:**
```kotlin
// Email verification
SignUp.PrepareVerificationParams.Strategy.EMAIL_CODE

// Phone verification  
SignUp.PrepareVerificationParams.Strategy.PHONE_CODE
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.signUp?.prepareVerification(
        SignUp.PrepareVerificationParams.Strategy.EMAIL_CODE
    )
    
    result?.onSuccess { signUp ->
        // Verification code sent to email
        promptForVerificationCode()
    }?.onFailure { error ->
        // Handle preparation error
    }
}
```

#### `suspend fun SignUp.attemptVerification(params: AttemptVerificationParams): ClerkResult<SignUp, ClerkErrorResponse>`

Attempts to complete verification with provided code.

**Parameters:**
```kotlin
// Email code verification
SignUp.AttemptVerificationParams.EmailCode("123456")

// Phone code verification
SignUp.AttemptVerificationParams.PhoneCode("123456")
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.signUp?.attemptVerification(
        SignUp.AttemptVerificationParams.EmailCode("123456")
    )
    
    result?.onSuccess { signUp ->
        when (signUp.status) {
            SignUp.Status.COMPLETE -> {
                // Sign-up complete, user created
                val userId = signUp.createdUserId
                val sessionId = signUp.createdSessionId
            }
            SignUp.Status.MISSING_REQUIREMENTS -> {
                // Still missing some requirements
                handleMissingRequirements(signUp)
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

### OAuth Sign-Up

#### `suspend fun SignUp.authenticateWithRedirect(params: AuthenticateWithRedirectParams): ClerkResult<OAuthResult, ClerkErrorResponse>`

Initiates OAuth sign-up flow.

**Parameters:**
```kotlin
// OAuth sign-up
SignUp.AuthenticateWithRedirectParams.OAuth(
    provider = OAuthProvider.GOOGLE,
    redirectUrl = "https://yourapp.com/auth/callback",
    emailAddress = "user@example.com",
    legalAccepted = true
)

// Enterprise SSO sign-up
SignUp.AuthenticateWithRedirectParams.EnterpriseSSO(
    strategy = "enterprise_sso",
    redirectUrl = "https://yourapp.com/auth/callback",
    emailAddress = "user@company.com"
)
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = SignUp.authenticateWithRedirect(
        SignUp.AuthenticateWithRedirectParams.OAuth(
            provider = OAuthProvider.GOOGLE,
            emailAddress = "user@example.com",
            legalAccepted = true
        )
    )
    
    result.onSuccess { oauthResult ->
        // OAuth flow initiated
        // Handle redirect to OAuth provider
    }.onFailure { error ->
        // Handle OAuth initiation error
    }
}
```

### Complete Sign-Up Flow Example

```kotlin
class SignUpActivity : AppCompatActivity() {
    
    private fun startSignUp() {
        lifecycleScope.launch {
            // Step 1: Create sign-up
            val createResult = SignUp.create(
                SignUp.CreateParams.Standard(
                    emailAddress = "user@example.com",
                    password = "securePassword123",
                    firstName = "John",
                    lastName = "Doe"
                )
            )
            
            createResult.onSuccess { signUp ->
                handleSignUpCreated(signUp)
            }.onFailure { error ->
                showError("Failed to create sign-up: ${error.message}")
            }
        }
    }
    
    private suspend fun handleSignUpCreated(signUp: SignUp) {
        when (signUp.status) {
            SignUp.Status.MISSING_REQUIREMENTS -> {
                // Check what verification is needed
                if (signUp.unverifiedFields.contains("email_address")) {
                    prepareEmailVerification()
                }
            }
            SignUp.Status.COMPLETE -> {
                // Sign-up complete
                navigateToMainActivity()
            }
            else -> {
                showError("Unexpected sign-up status: ${signUp.status}")
            }
        }
    }
    
    private suspend fun prepareEmailVerification() {
        val result = Clerk.signUp?.prepareVerification(
            SignUp.PrepareVerificationParams.Strategy.EMAIL_CODE
        )
        
        result?.onSuccess {
            // Show verification code input
            showVerificationCodeDialog()
        }?.onFailure { error ->
            showError("Failed to send verification code: ${error.message}")
        }
    }
    
    private suspend fun verifyEmailCode(code: String) {
        val result = Clerk.signUp?.attemptVerification(
            SignUp.AttemptVerificationParams.EmailCode(code)
        )
        
        result?.onSuccess { signUp ->
            if (signUp.status == SignUp.Status.COMPLETE) {
                navigateToMainActivity()
            } else {
                // Handle remaining requirements
                handleSignUpCreated(signUp)
            }
        }?.onFailure { error ->
            showError("Invalid verification code: ${error.message}")
        }
    }
    
    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showVerificationCodeDialog() {
        // Show dialog to collect verification code
        // Call verifyEmailCode(code) when user submits
    }
}
```

### Handling Missing Requirements

```kotlin
private fun handleMissingRequirements(signUp: SignUp) {
    val missing = signUp.missingFields
    val unverified = signUp.unverifiedFields
    
    when {
        missing.contains("email_address") -> {
            // Need to collect email address
            showEmailInput()
        }
        missing.contains("password") -> {
            // Need to collect password
            showPasswordInput()
        }
        unverified.contains("email_address") -> {
            // Need to verify email
            prepareEmailVerification()
        }
        unverified.contains("phone_number") -> {
            // Need to verify phone
            preparePhoneVerification()
        }
        else -> {
            // Check for other requirements
            handleOtherRequirements(missing, unverified)
        }
    }
}

private suspend fun preparePhoneVerification() {
    val result = Clerk.signUp?.prepareVerification(
        SignUp.PrepareVerificationParams.Strategy.PHONE_CODE
    )
    
    result?.onSuccess {
        showPhoneVerificationDialog()
    }?.onFailure { error ->
        showError("Failed to send SMS code: ${error.message}")
    }
}
```