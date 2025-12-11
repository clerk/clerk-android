# Clerk Android Prebuilt Ui

This sample app is integrates the Clerk prebuilt UI components

## Prerequisites
- A Clerk account (sign up at [clerk.com](https://clerk.com))
- Google account for OAuth setup (optional, for Google Sign-In)

## Clerk Dashboard Setup

1. **Create a Clerk Application**
    - Sign up for a Clerk account at [dashboard.clerk.com](https://dashboard.clerk.com/sign-up)
    - Create a new application in your Clerk dashboard
    - Give it whatever name you like (e.g., "Prebuilt UI")
    - You can select whichever authentication methods you prefer, the Prebuilt components will handle them automatically.

2. **Get Your Publishable Key**
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
   PREBUILT_UI_CLERK_PUBLISHABLE_KEY=pk_test_your_publishable_key_here
   ```

   > ⚠️ **Important**: Replace `pk_test_your_publishable_key_here` with your actual publishable key from the Clerk dashboard.

## How to Run

1. **Using Android Studio**:
    - Open the project in Android Studio
    - Select the `prebuilt-ui` run configuration from the dropdown
    - Click the **Run** button

2. **Using Gradle command line**:
   ```bash
   ./gradlew :samples:prebuilt-ui:installDebug
   ```

## Related Documentation

- [Clerk Android SDK Reference](https://clerk-android.clerkstage.dev)
