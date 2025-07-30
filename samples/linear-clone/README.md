# Clerk Android Linear Clone Sample

This sample app is a native Android recreation of Linear's authentication flow, demonstrating a production-ready implementation of Clerk authentication with modern Android development practices. It showcases advanced features including Passkey authentication, Google Sign-In, email code authentication, and seamless integration with Jetpack Compose Navigation.

## 📖 What You'll Learn

- Production-ready authentication UI/UX patterns
- Passkey (WebAuthn) authentication implementation
- Google Sign-In integration
- Email code authentication (passwordless)
- Jetpack Compose Navigation with authentication flows
- Modern Android architecture patterns
- Advanced UI components and theming

## 🚀 Prerequisites

- Android Studio Arctic Fox or later
- Android SDK with minimum API level 24
- A Clerk account (sign up at [clerk.com](https://clerk.com))
- Google account for OAuth setup (optional, for Google Sign-In)

## 🔧 Clerk Dashboard Setup

1. **Create a Clerk Application**
   - Sign up for a Clerk account at [dashboard.clerk.com](https://dashboard.clerk.com/sign-up)
   - Create a new application in your Clerk dashboard
   - Choose "Android" as your platform

2. **Configure Authentication Methods**
   
   **Email Code Authentication:**
   - Go to **User & Authentication** → **Email, Phone, Username**
   - Enable **Email** authentication
   - Under **Authentication strategies**, enable **Email verification code**
   - Disable password if you want passwordless authentication only

   **Passkey Authentication:**
   - Navigate to **User & Authentication** → **Passkeys**
   - Enable **Passkeys** authentication
   - Configure settings for passkey creation and authentication

   **Google OAuth (Optional):**
   - Go to **User & Authentication** → **Social Connections**
   - Enable **Google** OAuth provider
   - Add your Google OAuth client credentials:
     - Go to [Google Cloud Console](https://console.cloud.google.com/)
     - Create/select a project
     - Enable Google+ API
     - Create OAuth 2.0 credentials
     - Add your Android app's SHA-1 fingerprint
     - Copy the client ID to Clerk dashboard

3. **Configure App Settings**
   - **User & Authentication** → **Restrictions**: Configure any domain or other restrictions
   - **User & Authentication** → **Personal Information**: Set required/optional user fields

4. **Get Your Publishable Key**
   - Navigate to **Developers** → **API Keys**
   - Copy your **Publishable Key** (starts with `pk_test_` or `pk_live_`)

## ⚙️ Project Setup

1. **Clone the repository** (if you haven't already):
   ```bash
   git clone https://github.com/clerk/clerk-android.git
   cd clerk-android
   ```

2. **Add your Clerk Publishable Key**:
   
   Open the `gradle.properties` file in the project root and add your publishable key:
   ```properties
   LINEAR_CLONE_CLERK_PUBLISHABLE_KEY=pk_test_your_publishable_key_here
   ```
   
   > ⚠️ **Important**: Replace `pk_test_your_publishable_key_here` with your actual publishable key from the Clerk dashboard.

3. **Configure Google Sign-In (Optional)**:
   
   If you want to test Google Sign-In:
   - Ensure you've configured Google OAuth in Clerk dashboard
   - Add your app's SHA-1 fingerprint to Google Cloud Console
   - The app will handle the OAuth flow automatically

4. **Sync the project**:
   - Open the project in Android Studio
   - Let Gradle sync complete

## 🏃‍♂️ How to Run

1. **Using Android Studio**:
   - Open the project in Android Studio
   - Select the `linear-clone` run configuration from the dropdown
   - Click the **Run** button or press `Shift + F10`

2. **Using Gradle command line**:
   ```bash
   ./gradlew :samples:linear-clone:installDebug
   ```

3. **Launch the app** on your device or emulator

> 📱 **Note**: For the best experience with Passkey authentication, test on a physical device with biometric authentication capabilities.

## 🎯 Key Features Demonstrated

### Authentication Methods
- **Passkey Authentication**: Modern biometric authentication using WebAuthn
- **Email Code Authentication**: Passwordless login with email verification codes
- **Google Sign-In**: OAuth authentication with Google accounts
- **Multi-step Flows**: Guided authentication process with proper navigation

### User Experience
- **Modern UI Design**: Follows contemporary design patterns inspired by Linear
- **Smooth Navigation**: Jetpack Compose Navigation with proper state management
- **Loading States**: Elegant loading indicators and transition states
- **Error Handling**: User-friendly error messages and recovery flows

### Technical Implementation
- **Jetpack Compose**: Fully built with modern declarative UI
- **Navigation Component**: Type-safe navigation with Compose Navigation
- **MVVM Architecture**: Clean separation of concerns with ViewModels
- **State Management**: Reactive state handling with Compose and Flows

## 📱 App Flow

### Authentication Journey:
1. **Get Started Screen**: Welcome screen with app branding
2. **Choose Login Method**: Select between Passkey, Email, or Google
3. **Email Entry**: Enter email address for verification
4. **Email Verification**: Enter verification code sent to email
5. **Home Screen**: Authenticated user dashboard

### Navigation States:
- **Unauthenticated**: Authentication flow navigation
- **Authenticated**: Main app content
- **Loading**: Initialization and transition states

## 🔍 Code Structure

```
src/main/java/com/clerk/linearclone/
├── LinearCloneApp.kt                  # Application class with Clerk initialization
├── MainActivity.kt                    # Main activity with navigation setup
├── MainViewModel.kt                   # Main authentication state management
├── navigation/                        # Navigation routes and setup
├── ui/
│   ├── getstarted/                   # Welcome/get started screen
│   ├── chooseloginmethod/            # Authentication method selection
│   ├── enteremail/                   # Email entry flow
│   ├── emailverification/            # Email code verification
│   ├── home/                         # Authenticated home screen
│   ├── button/                       # Reusable UI components
│   └── theme/                        # App theming and design system
└── res/                              # Resources (strings, colors, etc.)
```

## 🔧 Advanced Features

### Passkey Implementation
- Biometric authentication setup
- WebAuthn credential management
- Fallback authentication methods
- Cross-device synchronization support

### Email Code Flow
- Real-time email verification
- Code input validation
- Resend functionality
- Error state handling

### Google OAuth
- Seamless Google account integration
- Profile information retrieval
- Account linking capabilities
- Secure token management

## 🛠 Troubleshooting

**Build fails with "Missing LINEAR_CLONE_CLERK_PUBLISHABLE_KEY"**
- Ensure you've added `LINEAR_CLONE_CLERK_PUBLISHABLE_KEY` to your `gradle.properties` file
- Verify the key starts with `pk_test_` or `pk_live_`

**Passkey authentication not working**
- Verify Passkeys are enabled in Clerk dashboard
- Test on a physical device with biometric capabilities
- Ensure device has screen lock set up (PIN, pattern, fingerprint, etc.)
- Check that the device supports WebAuthn

**Google Sign-In fails**
- Verify Google OAuth is configured in Clerk dashboard
- Ensure SHA-1 fingerprint is added to Google Cloud Console
- Check that Google Play Services is installed on the device
- Verify OAuth client credentials are correct

**Email codes not being received**
- Check spam/junk folders
- Verify email provider settings in Clerk dashboard
- Ensure email address is correctly formatted
- Try with a different email provider

**Navigation issues**
- Clear app data and restart
- Check for proper initialization of Clerk SDK
- Verify authentication state is properly managed

## 🔒 Security Considerations

- **Biometric Security**: Passkeys provide enhanced security through biometric authentication
- **Passwordless**: Email code authentication eliminates password-related vulnerabilities
- **Token Management**: Secure handling of authentication tokens and session data
- **Network Security**: All communications are encrypted and secured

## 🎨 Design System

The app implements a cohesive design system including:
- **Color Palette**: Modern purple/gray color scheme
- **Typography**: Clean, readable font hierarchy
- **Component Library**: Reusable UI components
- **Responsive Layout**: Adaptive layouts for different screen sizes

## 📚 Next Steps

- Explore the [Quickstart sample](../quickstart/README.md) for basic authentication concepts
- Check out the [Custom Flows sample](../custom-flows/README.md) for advanced authentication patterns
- Read about [Passkey authentication](https://clerk.com/docs/authentication/passkeys) in detail
- Learn more about [OAuth integration](https://clerk.com/docs/authentication/social-connections)

## 📖 Related Documentation

- [Passkeys Documentation](https://clerk.com/docs/authentication/passkeys)
- [Social Connections (OAuth)](https://clerk.com/docs/authentication/social-connections)
- [Email & SMS](https://clerk.com/docs/authentication/email-sms)
- [Jetpack Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- [Clerk Android SDK Reference](https://clerk-android.clerkstage.dev)
