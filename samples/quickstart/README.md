# Clerk Android Quickstart Sample

This sample app demonstrates the basic integration of Clerk authentication in an Android application. It provides a simple example of user sign-up, sign-in, and profile management using the Clerk Android SDK.

## 📖 What You'll Learn

- How to initialize the Clerk Android SDK
- Basic user authentication flows (sign-in/sign-up)
- User session management
- Simple sign-out functionality

## 🚀 Prerequisites

- Android Studio Arctic Fox or later
- Android SDK with minimum API level 28
- A Clerk account (sign up at [clerk.com](https://clerk.com))

## 🔧 Clerk Dashboard Setup

1. **Create a Clerk Application**
   - Sign up for a Clerk account at [dashboard.clerk.com](https://dashboard.clerk.com/sign-up)
   - Create a new application in your Clerk dashboard
   - Choose "Android" as your platform

2. **Configure Authentication Methods**
   - In your Clerk dashboard, go to **User & Authentication** → **Email, Phone, Username**
   - Enable **Email** authentication
   - Configure **Sign-up** and **Sign-in** options as needed
   - For this quickstart, email/password authentication is recommended

3. **Get Your Publishable Key**
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
   QUICKSTART_CLERK_PUBLISHABLE_KEY=pk_test_your_publishable_key_here
   ```
   
   > ⚠️ **Important**: Replace `pk_test_your_publishable_key_here` with your actual publishable key from the Clerk dashboard.

3. **Sync the project**:
   - Open the project in Android Studio
   - Let Gradle sync complete

## 🏃‍♂️ How to Run

1. **Using Android Studio**:
   - Open the project in Android Studio
   - Select the `quickstart` run configuration from the dropdown
   - Click the **Run** button or press `Shift + F10`

2. **Using Gradle command line**:
   ```bash
   ./gradlew :samples:quickstart:installDebug
   ```

3. **Launch the app** on your device or emulator

## 🎯 Key Features Demonstrated

- **SDK Initialization**: Shows how to initialize Clerk in your Application class
- **Authentication State Management**: Demonstrates reactive state management with user authentication status
- **Sign In/Sign Up**: Basic authentication flows using Clerk's prebuilt components
- **User Session**: Access to current user information and session management
- **Sign Out**: Proper user session termination

## 📱 App Flow

1. **Launch**: App checks authentication state
2. **Signed Out**: Shows sign-in/sign-up options
3. **Authentication**: Users can sign in or create a new account
4. **Signed In**: Displays a simple authenticated state with sign-out option

## 🔍 Code Structure

```
src/main/kotlin/com/clerk/quickstart/
├── MainApplication.kt      # Clerk SDK initialization
├── MainActivity.kt         # Main app entry point
├── MainViewModel.kt        # State management for auth flows
├── SignInOrUpView.kt      # Authentication UI component
├── signin/                 # Sign-in specific components
└── signup/                # Sign-up specific components
```

## 🛠 Troubleshooting

**Build fails with "Missing CLERK_PUBLISHABLE_KEY"**
- Ensure you've added `QUICKSTART_CLERK_PUBLISHABLE_KEY` to your `gradle.properties` file
- Verify the key starts with `pk_test_` or `pk_live_`

**App crashes on startup**
- Check that your publishable key is valid
- Ensure you have internet connectivity
- Verify your Clerk application is properly configured

**Authentication not working**
- Verify your Clerk dashboard has email authentication enabled
- Check that your app's package name matches any restrictions in Clerk dashboard
- Ensure your API keys are for the correct Clerk environment (test vs production)

## 📚 Next Steps

- Explore the [Custom Flows sample](../custom-flows/README.md) for advanced authentication patterns
- Check out the [Linear Clone sample](../linear-clone/README.md) for a production-ready implementation
- Read the [Clerk Android documentation](https://clerk.com/docs/quickstarts/android) for detailed guides

## 📖 Related Documentation

- [Clerk Android SDK Reference](https://clerk-android.clerkstage.dev)
- [Clerk Documentation](https://clerk.com/docs)
- [Android Integration Guide](https://clerk.com/docs/quickstarts/android)
