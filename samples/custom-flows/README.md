# Clerk Android Custom Flows Sample

This sample app demonstrates advanced authentication flows using the Clerk Android SDK. It showcases how to implement custom authentication patterns including multi-factor authentication, OAuth integration, password reset flows, and user profile management.

## Clerk Dashboard Setup

1. **Create a Clerk Application**
   - Sign up for a Clerk account at [dashboard.clerk.com](https://dashboard.clerk.com/sign-up)
   - Create a new application in your Clerk dashboard
   - Choose "Android" as your platform

2. **Configure Authentication Methods**
   
   **Email/Password Authentication:**
   - Go to **User & Authentication** → **Email, Phone, Username**
   - Enable **Email** authentication
   - Enable **Password** authentication
   - Configure **Sign-up** and **Sign-in** options

   **Multi-Factor Authentication:**
   - Navigate to **User & Authentication** → **Multi-factor**
   - Enable **SMS** and/or **TOTP** (Time-based One-Time Password)
   - Configure backup codes if desired

   **OAuth Providers:**
   - Go to **User & Authentication** → **Social Connections**
   - Enable desired OAuth providers (Google, GitHub, etc.)
   - Configure OAuth app credentials for each provider

3. **Configure Additional Settings**
   - **User & Authentication** → **Email, Phone, Username**: Enable email and phone number as identifiers
   - **User & Authentication** → **Personal Information**: Configure which fields are required/optional

4. **Get Your Publishable Key**
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
   CUSTOM_FLOWS_CLERK_PUBLISHABLE_KEY=pk_test_your_publishable_key_here
   ```
   
   > ⚠️ **Important**: Replace `pk_test_your_publishable_key_here` with your actual publishable key from the Clerk dashboard.

## How to Run

1. **Using Android Studio**:
   - Open the project in Android Studio
   - Select the `custom-flows` run configuration from the dropdown
   - Click the **Run** button

2. **Using Gradle command line**:
   ```bash
   ./gradlew :samples:custom-flows:installDebug
   ```

3. **Launch the app** on your device or emulator


## Related Documentation

- [Custom Flows Overview](https://clerk.com/docs/custom-flows/overview)
- [Email/Password Authentication](https://clerk.com/docs/custom-flows/email-password)
- [Multi-factor Authentication](https://clerk.com/docs/custom-flows/mfa)
- [OAuth Flows](https://clerk.com/docs/custom-flows/oauth)
- [Clerk Android SDK Reference](https://clerk-android.clerkstage.dev)
