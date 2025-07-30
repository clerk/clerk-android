# Clerk Android Quickstart Sample

This sample app demonstrates the basic integration of Clerk authentication in an Android application. It provides a simple example of user sign-up, sign-in, and profile management using the Clerk Android SDK.

## Clerk Dashboard Setup

1. **Create a Clerk Application**
   - Sign up for a Clerk account at [dashboard.clerk.com](https://dashboard.clerk.com/sign-up)
   - Create a new application in your Clerk dashboard
   - Give it whatever name you like (e.g., "Quickstart")
   - For Sign in options select **Email**

2. **Configure Authentication Methods**
   - In your Clerk dashboard, go to **User & Authentication** → **Email, Phone, Username**
   - Make sure **Email** authentication is enabled
   - Under Sign in options enable **Email verification code**, **Phone number**, and **Password**

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
   QUICKSTART_CLERK_PUBLISHABLE_KEY=pk_test_your_publishable_key_here
   ```
   
   > ⚠️ **Important**: Replace `pk_test_your_publishable_key_here` with your actual publishable key from the Clerk dashboard.


## How to Run

1. **Using Android Studio**:
   - Open the project in Android Studio
   - Select the `quickstart` run configuration from the dropdown
   - Click the **Run** button

2. **Using Gradle command line**:
   ```bash
   ./gradlew :samples:quickstart:installDebug
   ```

3. **Launch the app** on your device or emulator

## Related Documentation

- [Clerk Android SDK Reference](https://clerk-android.clerkstage.dev)
- [Clerk Documentation](https://clerk.com/docs)
- [Android Integration Guide](https://clerk.com/docs/quickstarts/android)
