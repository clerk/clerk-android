<p align="center">
  <a href="https://clerk.com?utm_source=github&utm_medium=clerk_android" target="_blank" rel="noopener noreferrer">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://images.clerk.com/static/logo-dark-mode-400x400.png">
      <img src="https://images.clerk.com/static/logo-light-mode-400x400.png" height="260">
    </picture>
  </a>
  <br />
</p>
<h1 align="center">
  Official Clerk Android SDK
</h1>
<p align="center">
  <strong>
    Clerk helps developers build user management. We provide streamlined user experiences for your users to sign up, sign in, and manage their profile.
  </strong>
</p>

[![chat on Discord](https://img.shields.io/discord/856971667393609759.svg?logo=discord)](https://clerk.com/discord)
[![documentation](https://img.shields.io/badge/documentation-clerk-green.svg)](https://clerk.com/docs)
[![twitter](https://img.shields.io/twitter/follow/ClerkDev?style=social)](https://twitter.com/intent/follow?screen_name=ClerkDev)

---

**Clerk is Hiring!**

Would you like to work on Open Source software and help maintain this repository? [Apply today!](https://jobs.ashbyhq.com/clerk)

---

## � Requirements

- **Android API Level 24 (Android 7.0)** or higher
- **Kotlin 2.2.0** or higher
- **Java 17** or higher
- **Android Gradle Plugin 8.11.0** or higher

## �🚀 Get Started with Clerk

### 1. Create a Clerk Application
1. [Sign up for an account](https://dashboard.clerk.com/sign-up?utm_source=github&utm_medium=clerk_android_repo_readme)
2. Create an application in your Clerk dashboard
3. Copy your **Publishable Key** from the API Keys section

### 2. Install the SDK
Add the Clerk Android SDK to your project following the installation instructions below.

### 3. Initialize Clerk
Configure Clerk in your application class with your publishable key.

### 4. Add Authentication
Use Clerk's authentication methods to enable sign-up, sign-in, and user management.

## 🧑‍💻 Installation

The Clerk Android SDK is distributed via Maven Central. 

### Add Repository
First, ensure you have added [mavenCentral](https://docs.gradle.org/current/userguide/declaring_repositories.html) to your project's `build.gradle(.kts)`:

```gradle
repositories {
    mavenCentral()
}
```

### Add Dependency
Add the Clerk SDK to your application's dependencies:

```gradle
dependencies {
    implementation 'com.clerk:clerk-android:0.1.0'
}
```

**Kotlin DSL:**
```kotlin
dependencies {
    implementation("com.clerk:clerk-android:0.1.0")
}
```

> 💡 **Tip:** Check [Maven Central](https://central.sonatype.com/artifact/com.clerk/clerk-android) for the latest version.

## 🛠️ Usage

### Initialization

Before using any part of the SDK, you must call `Clerk.initialize()` in your Application class with your publishable key and application context:

```kotlin
import com.clerk.Clerk

class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Clerk.initialize(
            context = this,
            publishableKey = "pk_test_..." // Your publishable key from Clerk Dashboard
        )
    }
}
```

**Don't forget to register your Application class in `AndroidManifest.xml`:**
```xml
<application
    android:name=".YourApplication"
    android:label="@string/app_name"
    android:theme="@style/AppTheme">
    <!-- ... -->
</application>
```

### Check Authentication Status

Now you can conditionally render content based on the user's session:

```kotlin
import com.clerk.Clerk

if (Clerk.user != null) {
    Text("Hello, ${Clerk.user?.firstName ?: "User"}")
} else {
    Text("You are signed out")
}
```

### Authentication

All authentication functions follow a consistent parameter pattern where function parameters are exposed as data classes named after the function itself (e.g., `SignUp.Create` parameters are `SignUp.SignUpCreateParams`).

#### Sign Up with Email and Verification

```kotlin
import com.clerk.SignUp

// Create a sign up
SignUp.create(SignUp.CreateParams.Standard(emailAddress = "user@example.com"))
    .onSuccess { signUp ->
        // Check if the SignUp needs the email address verified and send an OTP code via email
        if (signUp.unverifiedFields.contains("email_address")) {
            signUp.prepareVerification(SignUp.PrepareVerificationParams.EMAIL_CODE)
                .onSuccess { 
                    // OTP sent successfully
                }
                .onFailure { 
                    // Handle error
                }
        }
    }
    .onFailure { error ->
        // Handle sign-up creation error
        Log.e("Clerk", "Sign-up failed: ${error.message}")
    }

// After collecting the OTP from the user
Clerk.signUp?.attemptVerification(SignUp.AttemptVerificationParams.EmailCode(code = "123456"))
    .onSuccess { 
        // User is signed in successfully
    }
```

#### Passwordless Sign In

```kotlin
import com.clerk.SignIn

var signIn: SignIn? = null

// Create the sign in
SignIn.create(SignIn.CreateParams.Strategy.EmailCode("email@example.com"))
    .onSuccess { 
        signIn = it
        // OTP code sent to email
    }

// After collecting the OTP code from the user, attempt verification
signIn?.attemptFirstFactor(SignIn.AttemptFirstFactorParams.Strategy.EmailCode("123456"))
    .onSuccess {
        // User signed in successfully
    }
```

#### Sign In with OAuth (Google, GitHub, etc.)

Clerk handles the OAuth flow and deep linking automatically:

```kotlin
import com.clerk.SignIn
import com.clerk.network.model.oauth.OAuthProvider

// This will open the OAuth provider's sign-in page
SignIn.authenticateWithRedirect(OAuthProvider.GOOGLE)
    .onSuccess {
        // OAuth flow initiated successfully
    }
```

#### Native Sign In with Google

```kotlin
SignIn.authenticateWithGoogleOneTap()
    .onSuccess {
        // Google One Tap sign-in successful
    }
    .onFailure { error ->
        Log.e("Clerk", "Google One Tap failed: ${error.message}")
    }
```

#### Authenticate with Google Credential Manager

```kotlin
import com.clerk.passkeys.CredentialType

SignIn.authenticateWithGoogleCredentialManager(
    credentialTypes = listOf(
        CredentialType.PASSKEY, 
        CredentialType.PASSWORD, 
        CredentialType.GOOGLE
    )
)
.onSuccess {
    // Authentication successful
}
.onFailure { error ->
    Log.e("Clerk", "Credential Manager auth failed: ${error.message}")
}
```

#### Forgot Password

```kotlin
import com.clerk.SignIn

// Create a sign in and send an OTP code to verify the user owns the email
SignIn.create(SignIn.CreateParams.Strategy.EmailCode("user@example.com"))
    .onSuccess { signIn ->
        // After collecting the OTP code from the user, attempt verification
        signIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.Strategy.ResetPasswordEmailCode("123456"))
            .onSuccess { verifiedSignIn ->
                // Set a new password to complete the process
                verifiedSignIn.resetPassword(
                    password = "newSecurePassword123!", 
                    signOutOfOtherSessions = true
                )
                .onSuccess {
                    // Password reset successful
                }
            }
    }
```

#### Sign Out

```kotlin
Clerk.signOut()
    .onSuccess {
        // User signed out successfully
    }
    .onFailure { error ->
        Log.e("Clerk", "Sign out failed: ${error.message}")
    }
```

### User Management

#### Update User Profile

```kotlin
import com.clerk.User

Clerk.user.update(
    User.UpdateParams(firstName = "Walter", lastName = "Johnson"))
        .onSuccess {
           updatedUser -> // User updated
        }
```

#### Update User Profile Image

```kotlin
// After getting a java.io.File object to upload
val imageFile: File = // ... your image file
Clerk.user.setProfileImage(file = imageFile)
    .onSuccess { 
        // Profile image updated successfully
    }
```

#### Add Phone Number

```kotlin
import com.clerk.PhoneNumber

lateinit var newPhoneNumber: PhoneNumber

// Create a new phone number on the user's account
Clerk.user.createPhoneNumber("+15555550100")
    .onSuccess { phoneNumber ->
        newPhoneNumber = phoneNumber
        // Use the returned resource to send an OTP
        newPhoneNumber.prepareVerification()
    }

// After collecting the OTP code from the user, attempt verification
newPhoneNumber.attemptVerification(code = "123456")
    .onSuccess {
        // Phone number verified and added successfully
    }
    .onFailure { error ->
        Log.e("Clerk", "Phone verification failed: ${error.message}")
    }
```

#### Link an External Account

```kotlin
import com.clerk.User
import com.clerk.network.model.oauth.OAuthProvider

Clerk.user.createExternalAccount(
    User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
).onSuccess { externalAccount ->
    externalAccount.reauthorize()
}
```

#### Session Tokens

```kotlin
// Get the current session token for API calls
Clerk.session.fetchToken().jwt.let { token ->
    // Use the token in your API requests
    val headers = mutableMapOf<String, String>()
    headers["Authorization"] = "Bearer $token"
    
    // Make your authenticated API call
    // ...
}
```

## 📱 Example App

This repository includes a complete example app demonstrating phone authentication with SMS OTP and social sign-in. The example app shows best practices for integrating Clerk into an Android application.

### Quick Start

1. **Setup Clerk Account**:
   ```bash
   # Visit https://dashboard.clerk.com
   # Create a new application
   # Copy your publishable key
   ```

2. **Configure Phone Authentication**:
   - In Clerk Dashboard → **User & Authentication** → **Email, Phone, Username**
   - Enable **Phone number** as a contact method
   - Configure **SMS** verification method
   - For production: Set up SMS provider (Twilio recommended)

3. **Run the Example App**:
   ```bash
   # Clone the repository
   git clone https://github.com/clerk/clerk-android.git
   cd clerk-android

   # Add your publishable key to gradle.properties
   echo "CLERK_PUBLISHABLE_KEY=pk_test_your_key_here" >> gradle.properties

   # Build and run
   ./gradlew :samples:example-app:installDebug
   ```

### Example App Features

| Feature | Implementation | Files |
|---------|---------------|-------|
| **Phone Authentication** | SMS OTP sign-in/up | `SignInOrUpViewModel.kt` |
| **Social Authentication** | OAuth with Google, GitHub, etc. | `SocialProviderRow.kt` |
| **Session Management** | Automatic state management | `MainViewModel.kt` |
| **Phone Formatting** | US phone number formatting | `NanpVisualTransformation.kt` |
| **Modern UI** | Material Design 3 with Compose | `ui/theme/` |

### Phone Authentication Setup

The example app requires specific Clerk Dashboard configuration for phone authentication:

#### 1. Enable Phone Authentication
```
Dashboard → User & Authentication → Email, Phone, Username
✅ Phone number (required)
✅ Used for sign-in
✅ Require verification
```

#### 2. Configure SMS Provider
For **development/testing**:
- Clerk provides test SMS functionality
- Use phone numbers ending in `0000-0999`
- Verification code: `424242`

For **production**:
```
Dashboard → Configure → SMS
- Provider: Twilio (recommended)
- Account SID: [Your Twilio SID]
- Auth Token: [Your Twilio Token]  
- From Phone: [Verified Twilio number]
```

#### 3. Test Phone Numbers

**Development Mode** (no real SMS sent):
```
+1 555-000-0000  # Works with code 424242
+1 555-000-0001  # Works with code 424242
+1 555-000-####  # Any number ending 0000-0999
```

**Production Mode**:
- Use real phone numbers
- Real SMS will be sent
- Standard carrier rates apply

### Key Implementation Files

#### `MainApplication.kt` - Clerk Initialization
```kotlin
Clerk.initialize(
    this,
    BuildConfig.CLERK_PUBLISHABLE_KEY,
    options = ClerkConfigurationOptions(enableDebugMode = true)
)
```

#### `SignInOrUpViewModel.kt` - Phone Authentication
```kotlin
// Create sign-in with phone
SignIn.create(SignIn.CreateParams.Strategy.PhoneCode(phoneNumber))
    .flatMap { it.prepareFirstFactor(SignIn.PrepareFirstFactorParams.PhoneCode()) }

// Verify OTP code  
signIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code))
```

#### `gradle.properties` - Configuration
```properties
CLERK_PUBLISHABLE_KEY=pk_test_your_actual_key_here
```

### Detailed Documentation

For complete setup instructions, troubleshooting, and advanced configuration, see:
**[📖 Example App Documentation](samples/example-app/README.md)**

## 🔧 Troubleshooting

### Common Issues

**"Clerk not initialized" error:**
- Make sure you've called `Clerk.initialize()` in your Application class
- Verify your Application class is registered in `AndroidManifest.xml`
- Check that your publishable key is correct

**"Missing CLERK_PUBLISHABLE_KEY" error:**
- Add your publishable key to `gradle.properties`
- Verify the key format starts with `pk_test_` or `pk_live_`
- Ensure the key is valid and from the correct Clerk application

**SMS not received:**
- Check SMS provider configuration in Clerk Dashboard
- Verify phone number format (include country code: +1...)
- For development, use test numbers ending in 0000-0999
- Check spam/blocked messages on device

**OAuth deep linking not working:**
- Verify your configuration in the Clerk Dashboard
- Check redirect URLs in social provider settings

**ProGuard/R8 issues:**
- The SDK includes ProGuard rules automatically
- If you encounter issues, check the `proguard-rules.pro` file in the SDK

### Getting Help

- 📚 [Documentation](https://clerk.com/docs)
- 💬 [Discord Community](https://clerk.com/discord)
- 📧 [Support](https://clerk.com/support)
- 🐛 [Report Issues](https://github.com/clerk/clerk-android/issues)

## 📚 Documentation

- [Reference Documentation](https://clerk-android.clerkstage.dev)
- [Clerk Docs](https://clerk.com/docs)
- [Android Integration Guide](https://clerk.com/docs/android)

## ✅ Supported Features

| Feature | Android Support | Notes |
| --- | :---: | --- |
| Email/Phone/Username Authentication | ✅ | Full support |
| Email Code Verification | ✅ | OTP via email |
| SMS Code Verification | ✅ | OTP via SMS |
| Multi-Factor Authentication (TOTP / SMS) | ✅ | TOTP and SMS |
| Sign in / Sign up with OAuth | ✅ | Google, GitHub, Apple, etc. |
| Native Sign in with Google | ✅ | Google One Tap |
| Session Management | ✅ | Full session lifecycle |
| Forgot Password | ✅ | Email-based reset |
| User Management | ✅ | Profile, phone, email management |
| Passkeys | ✅ | WebAuthn support |
| Enterprise SSO (SAML) | ✅ | Enterprise authentication |
| Device Attestation | ✅ | Android Play Integrity |
| Multi-Session Applications | ❌ | Coming soon |
| Organizations | ❌ | Coming soon |
| Prebuilt UI Components | ❌ | Coming soon |
| Magic Links | ❌ | Planned |
| Web3 Wallet | ❌ | Planned |

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](https://github.com/clerk/clerk-android/blob/main/CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Run tests: `./gradlew test`

## 📝 License

This project is licensed under the **MIT license**.

See [LICENSE](https://github.com/clerk/clerk-android/blob/main/LICENSE) for more information.

---

<p align="center">
  Made with ❤️ by <a href="https://clerk.com">Clerk</a>
</p>
