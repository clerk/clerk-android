# Clerk Android Linear Clone Sample

This sample app is a native Android recreation of Linear's authentication flow, demonstrating Clerk authentication with modern Android development practices. It showcases advanced features including Passkey authentication, Google Sign-In, email code authentication, and seamless integration with Jetpack Compose Navigation.

## Prerequisites
- A Clerk account (sign up at [clerk.com](https://clerk.com))
- Google account for OAuth setup (optional, for Google Sign-In)

## Clerk Dashboard Setup

1. **Create a Clerk Application**
   - Sign up for a Clerk account at [dashboard.clerk.com](https://dashboard.clerk.com/sign-up)
   - Create a new application in your Clerk dashboard
   - Give it whatever name you like (e.g., "Linear Clone")
   - For Sign in options select **Email**, and **Google** and click Create Application

2. **Configure Authentication Methods**
   
   **Email Code Authentication:**
   - Under the **Configure** tab, go to **User & Authentication** → **Email, Phone, Username**
   - Enable **Email** authentication
   - Disable **Require email during sign-up**
   - Enable **Verify at sign-up** 
   - Enable **Email verification code**

   **Google OAuth (Optional):**
   - Nothing to do! If you selected Google during application creation, Clerk automatically configures it for you.


   **Passkey Authentication:**
   - For Passkey authentication, follow [this guide](https://www.clerk.com/docs/references/android/passkeys) in the Clerk Docs

3. **Get Your Publishable Key**
   - Navigate to **Developers** → **API Keys**
   - Copy your **Publishable Key** (starts with `pk_test_` or `pk_live_`)

## Project Setup

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

## How to Run

1. **Using Android Studio**:
   - Open the project in Android Studio
   - Select the `linear-clone` run configuration from the dropdown
   - Click the **Run** button

2. **Using Gradle command line**:
   ```bash
   ./gradlew :samples:linear-clone:installDebug
   ```

3. **Launch the app** on your device or emulator

> **Note**: Passkeys are not compatible with emulators. You must test Passkey authentication on a physical device.

## Related Documentation

- [Passkeys Documentation](https://clerk.com/docs/references/android/passkeys)
- [Social Connections (OAuth)](https://clerk.com/docs/authentication/social-connections)
- [Email & SMS](https://clerk.com/docs/authentication/email-sms)
- [Jetpack Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- [Clerk Android SDK Reference](https://clerk-android.clerkstage.dev)
