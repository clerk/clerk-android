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

3. **Configure Social Providers** (Optional):
   - Navigate to **User & Authentication** → **Social Connections**
   - Enable desired providers (Google, GitHub, Apple, etc.)

### 3. Configure Local Environment

1. **Add Publishable Key**:
   Create or update `gradle.properties` in the project root:
   ```properties
   CLERK_PUBLISHABLE_KEY=
   ```

   > ⚠️ **Important**: Add your publishable key from the Clerk Dashboard.

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

## Troubleshooting

### Common Issues

1. **"Missing CLERK_PUBLISHABLE_KEY" Error**:
   - Ensure `gradle.properties` contains your publishable key
   - Verify the key format starts with `pk_test_` or `pk_live_`

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
