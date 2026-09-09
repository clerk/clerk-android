# Clerk Android Quickstart Sample

This sample demonstrates custom email/password sign-in, password sign-up, email-code verification, and sign-out with the generated Kotlin API. The `prebuilt-ui` sample covers additional factors, profile management, and required session tasks.

## Clerk Dashboard Setup

1. **Create a Clerk Application**
   - Sign up for a Clerk account at [dashboard.clerk.com](https://dashboard.clerk.com/sign-up)
   - Create a new application in your Clerk dashboard
   - Give it whatever name you like (e.g., "Quickstart")
   - For Sign in options select **Email**

2. **Configure Authentication Methods**
   - In your Clerk dashboard, go to **User & Authentication** → **Email, Phone, Username**
   - Make sure **Email** authentication is enabled
   - Enable **Email verification code** and **Password**. This sample does not collect phone numbers or additional required profile fields.

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


## Generated API ownership

`MainApplication` retains one `Clerk.connect(...)` result across Activity recreation. `MainViewModel` observes `clerk.changes`; screens receive that same instance explicitly. Enable core library desugaring as shown in this sample's Gradle configuration.

The custom flow calls `clerk.signIn.password(...)` or `clerk.signUp.create(...)`, verifies email through `signUp.verifications`, and calls `finalize()` only after the attempt is complete. Finalization can leave a pending session task. The sample reports that state and keeps authenticated content gated; use the `prebuilt-ui` sample to complete those tasks. API failures are displayed in the form.

Incoming callback URLs are forwarded to `handleAuthCallback()`. That operation itself does not activate a session; a custom email-link flow must inspect the returned resource and finalize it when complete. Email-link and browser sign-in screens are outside this sample's scope.

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
