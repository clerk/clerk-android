# Clerk Android SDK Example App

This example app demonstrates how to integrate Clerk authentication into an Android application with phone/SMS OTP authentication and social sign-in.

## Features

- **Phone Authentication**: Sign in/up using phone number with SMS OTP verification
- **Social Authentication**: Sign in with Google, GitHub, Apple, and other OAuth providers
- **Session Management**: Automatic session handling and user state management
- **Material Design 3**: Modern UI with Compose and Material Design 3
- **Hilt Dependency Injection**: Clean architecture with dependency injection

## Prerequisites

- **Android Studio** (Arctic Fox or newer)
- **Android API Level 28+** (Android 9.0)
- **Java 17+** 
- **Kotlin 2.2.0+**
- **Active Clerk Account** with phone authentication enabled

## Setup Instructions

### 1. Clone and Setup Project

```bash
git clone https://github.com/clerk/clerk-android.git
cd clerk-android/samples/example-app
```

### 2. Configure Clerk Dashboard

1. **Create a Clerk Application**:
   - Visit [Clerk Dashboard](https://dashboard.clerk.com)
   - Create a new application or use an existing one
   - Copy your **Publishable Key** from the API Keys section

2. **Enable Phone Authentication**:
   - Navigate to **User & Authentication** → **Email, Phone, Username**
   - Enable **Phone number** as a contact method
   - Configure **SMS** as the verification method
   - Choose your SMS provider (Twilio recommended for production)

3. **Configure Social Providers** (Optional):
   - Navigate to **User & Authentication** → **Social Connections**
   - Enable desired providers (Google, GitHub, Apple, etc.)
   - Configure OAuth redirect URLs and API keys
   - See detailed social provider setup below

4. **SMS Provider Setup**:
   - For **development**: Clerk provides test SMS functionality
   - For **production**: Configure a real SMS provider
     - Go to **Configure** → **SMS** 
     - Add your Twilio credentials or other SMS provider
     - Verify your sender phone number

5. **Social Provider Setup** (Optional but Recommended):
   Social authentication provides a seamless sign-in experience for users.

   **In Clerk Dashboard:**
   - Navigate to **User & Authentication** → **Social Connections**
   - Enable providers you want to support
   - Configure each provider with OAuth credentials

   **Supported Providers:**
   - **Google**: Most popular, recommended for all apps
   - **GitHub**: Great for developer-focused applications
   - **Apple**: Required for iOS App Store submission
   - **Facebook/Meta**: Wide user base
   - **Microsoft**: Enterprise applications
   - **Discord**: Gaming and community apps
   - **Twitter/X**: Social media integration
   - **LinkedIn**: Professional applications

   **Google Setup Example:**
   1. Go to [Google Cloud Console](https://console.cloud.google.com)
   2. Create/select a project
   3. Enable Google+ API
   4. Create OAuth 2.0 credentials
   5. Add authorized redirect URIs:
      - `https://your-clerk-app.clerk.accounts.dev/v1/oauth_callback`
      - Replace `your-clerk-app` with your actual Clerk application name
   6. Copy Client ID and Client Secret to Clerk Dashboard

   **GitHub Setup Example:**
   1. Go to GitHub → Settings → Developer settings → OAuth Apps
   2. Create new OAuth App
   3. Set Authorization callback URL:
      - `https://your-clerk-app.clerk.accounts.dev/v1/oauth_callback`
   4. Copy Client ID and Client Secret to Clerk Dashboard

   **Apple Setup** (iOS apps):
   1. Go to [Apple Developer Portal](https://developer.apple.com)
   2. Create Sign in with Apple service
   3. Configure bundle ID and redirect URLs
   4. Generate private key and team ID
   5. Add credentials to Clerk Dashboard

### 3. Configure Local Environment

1. **Add Publishable Key**:
   Create or update `gradle.properties` in the project root:
   ```properties
   CLERK_PUBLISHABLE_KEY=pk_test_your_key_here_replace_with_actual_key
   ```

   > ⚠️ **Important**: Replace `pk_test_your_key_here_replace_with_actual_key` with your actual publishable key from the Clerk Dashboard.

2. **Verify Configuration**:
   The app's `build.gradle.kts` will automatically inject this key as a BuildConfig field.

### 4. Build and Run

1. **Open in Android Studio**:
   ```bash
   # From the project root
   ./gradlew :samples:example-app:assembleDebug
   ```

2. **Install on Device/Emulator**:
   ```bash
   ./gradlew :samples:example-app:installDebug
   ```

3. **Run from Android Studio**:
   - Open the project in Android Studio
   - Select `example-app` configuration
   - Click Run

## App Structure

```
src/main/kotlin/com/clerk/exampleapp/
├── MainActivity.kt              # Main entry point
├── MainApplication.kt           # Application class with Clerk initialization
├── MainViewModel.kt             # Authentication state management
├── navigation/
│   └── Route.kt                # Navigation routes
├── ui/
│   ├── common/                 # Reusable UI components
│   │   ├── CodeInput.kt        # OTP code input field
│   │   ├── PhoneNumberInput.kt # Phone number input with formatting
│   │   ├── SocialButton.kt     # Social provider buttons
│   │   └── ...
│   ├── screens/
│   │   ├── signin/             # Sign-in/up flow
│   │   │   ├── SignInOrUpScreen.kt
│   │   │   └── SignInOrUpViewModel.kt
│   │   └── profile/            # User profile screen
│   │       ├── ProfileScreen.kt
│   │       └── ProfileViewModel.kt
│   └── theme/                  # App theming
└── utils/
    └── NanpVisualTransformation.kt # Phone number formatting
```

## How It Works

### Authentication Flow

1. **Phone Number Entry**: User enters their phone number
2. **SMS OTP**: Clerk sends a verification code via SMS
3. **Code Verification**: User enters the OTP code
4. **Session Creation**: Successful verification creates a user session
5. **Profile Access**: User can access authenticated content

### Key Components

- **MainViewModel**: Manages global authentication state
- **SignInOrUpViewModel**: Handles phone authentication and social sign-in
- **SocialProviderRow**: Displays available social providers as buttons
- **SocialButton**: Individual social provider button component
- **Clerk Integration**: Automatic session management and state updates

### Phone Authentication Implementation

```kotlin
// Create sign-in with phone number
SignIn.create(SignIn.CreateParams.Strategy.PhoneCode(phoneNumber))
    .flatMap { it.prepareFirstFactor(SignIn.PrepareFirstFactorParams.PhoneCode()) }
    .onSuccess { /* SMS sent */ }

// Verify OTP code
signIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code))
    .onSuccess { /* User signed in */ }
```

### Social Authentication Implementation

```kotlin
// Authenticate with social provider (Google, GitHub, Apple, etc.)
fun authenticateWithRedirect(socialConfig: UserSettings.SocialConfig) {
    SignIn.authenticateWithRedirect(
        SignIn.AuthenticateWithRedirectParams.OAuth(
            OAuthProvider.fromStrategy(socialConfig.strategy)
        )
    )
    .onSuccess { /* User signed in successfully */ }
    .onFailure { /* Handle authentication error */ }
}
```

**Available Social Providers:**
- Google, GitHub, Apple, Facebook/Meta
- Microsoft, Discord, Twitter/X, LinkedIn
- Automatically loaded from Clerk Dashboard configuration
- One-tap authentication with provider-specific UI

## Configuration Options

### Clerk Initialization

```kotlin
// In MainApplication.kt
Clerk.initialize(
    this,
    BuildConfig.CLERK_PUBLISHABLE_KEY,
    options = ClerkConfigurationOptions(enableDebugMode = true)
)
```

### Phone Number Formatting

The app includes NANP (North American Numbering Plan) formatting for US phone numbers:
- Automatically formats as: `(XXX) XXX-XXXX`
- Handles international numbers
- Visual transformation only (doesn't affect actual value)

## Testing

### Test Accounts

During development, you can use Clerk's test mode:
- Any phone number ending in `0000-0999` will work in test mode
- Use code `424242` for verification
- No actual SMS will be sent

### Production Testing

For production testing:
1. Use real phone numbers
2. Configure actual SMS provider
3. Test with international numbers if supported
4. Verify rate limiting and error handling

### Social Authentication Testing

**Development Mode**:
- Social providers work immediately after configuration
- Test with real social accounts (Google, GitHub, etc.)
- No special development credentials needed

**Production Testing**:
1. Verify OAuth redirect URLs are correct
2. Test each enabled social provider
3. Ensure proper error handling for declined permissions
4. Test account linking when user has multiple sign-in methods

## Troubleshooting

### Common Issues

1. **"Missing CLERK_PUBLISHABLE_KEY" Error**:
   - Ensure `gradle.properties` contains your publishable key
   - Verify the key format starts with `pk_test_` or `pk_live_`

2. **SMS Not Received**:
   - Check SMS provider configuration in Clerk Dashboard
   - Verify phone number format (include country code)
   - Check spam/blocked messages
   - Ensure SMS provider has sufficient credits

3. **Social Authentication Not Working**:
   - Verify OAuth app setup in provider dashboard (Google Cloud, GitHub, etc.)
   - Check redirect URLs match exactly: `https://your-clerk-app.clerk.accounts.dev/v1/oauth_callback`
   - Ensure Client ID and Client Secret are correctly entered in Clerk Dashboard
   - For Google: Verify Google+ API is enabled and OAuth consent screen is configured
   - For GitHub: Check OAuth app permissions and callback URL
   - For Apple: Ensure Sign in with Apple is enabled and bundle ID matches
   - Clear browser cache and cookies if authentication seems stuck

4. **Build Errors**:
   - Clean and rebuild: `./gradlew clean build`
   - Sync project with Gradle files
   - Check Android Studio and SDK versions

### Debug Mode

Enable debug logging by setting debug mode in Clerk initialization:
```kotlin
ClerkConfigurationOptions(enableDebugMode = true)
```

## Additional Resources

- [Clerk Android Documentation](https://clerk.com/docs/android)
- [Phone Authentication Guide](https://clerk.com/docs/authentication/configuration/sign-up-sign-in-options#phone-number)
- [SMS Configuration](https://clerk.com/docs/authentication/configuration/sms)
- [OAuth Configuration](https://clerk.com/docs/authentication/configuration/oauth)

## Support

- 📚 [Clerk Documentation](https://clerk.com/docs)
- 💬 [Discord Community](https://clerk.com/discord)
- 🐛 [Report Issues](https://github.com/clerk/clerk-android/issues)