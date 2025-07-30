# Clerk Android Custom Flows Sample

This sample app demonstrates advanced authentication flows using the Clerk Android SDK. It showcases how to implement custom authentication patterns including multi-factor authentication, OAuth integration, password reset flows, and user profile management.

## 📖 What You'll Learn

- Custom email/password authentication flows
- Multi-factor authentication (MFA) implementation
- OAuth authentication with third-party providers
- Password reset and recovery flows
- Adding email addresses and phone numbers to user profiles
- Advanced error handling and user experience patterns

## 🚀 Prerequisites

- Android Studio Arctic Fox or later
- Android SDK with minimum API level 24
- A Clerk account (sign up at [clerk.com](https://clerk.com))

## 🔧 Clerk Dashboard Setup

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

## ⚙️ Project Setup

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

3. **Sync the project**:
   - Open the project in Android Studio
   - Let Gradle sync complete

## 🏃‍♂️ How to Run

1. **Using Android Studio**:
   - Open the project in Android Studio
   - Select the `custom-flows` run configuration from the dropdown
   - Click the **Run** button or press `Shift + F10`

2. **Using Gradle command line**:
   ```bash
   ./gradlew :samples:custom-flows:installDebug
   ```

3. **Launch the app** on your device or emulator

## 🎯 Key Features Demonstrated

### Authentication Flows
- **Email/Password Sign In**: Custom implementation of email and password authentication
- **Email/Password Sign Up**: Custom user registration with email verification
- **Multi-Factor Authentication**: SMS and TOTP-based MFA flows
- **OAuth Authentication**: Social login with third-party providers

### User Management
- **Add Email Address**: Allow users to add additional email addresses
- **Add Phone Number**: Phone number verification and addition to user profile
- **Password Reset**: Email and SMS-based password recovery flows

### Advanced Patterns
- **Custom Error Handling**: Comprehensive error handling for various authentication scenarios
- **Loading States**: Proper loading and feedback states during authentication
- **Navigation Flow**: Multi-step authentication processes with proper navigation

## 📱 App Flow

### When Signed Out:
1. **Email/Password Sign In**: Direct login with existing credentials
2. **Email/Password Sign Up**: Create new account with email verification
3. **MFA Sign In**: Multi-factor authentication for enhanced security
4. **OAuth Sign In**: Social authentication with third-party providers
5. **Password Reset**: Recover account via email or phone

### When Signed In:
1. **Add Email Address**: Add additional email to user profile
2. **Add Phone Number**: Add and verify phone number
3. **Sign Out**: Terminate current session

## 🔍 Code Structure

```
src/main/java/com/clerk/customflows/
├── CustomFlowsApplication.kt          # Clerk SDK initialization
├── MainActivity.kt                    # Main app entry point
├── MainViewModel.kt                   # Main state management
├── emailpassword/
│   ├── signin/                        # Email/password sign-in flow
│   ├── signup/                        # Email/password sign-up flow
│   └── mfa/                          # Multi-factor authentication
├── oauth/                             # OAuth authentication flows
├── forgotpassword/
│   ├── emailaddress/                  # Email-based password reset
│   └── phone/                         # SMS-based password reset
├── addemail/                          # Add email address flow
├── addphone/                          # Add phone number flow
└── ui/                               # Shared UI components and theme
```

## 🛠 Troubleshooting

**Build fails with "Missing CUSTOM_FLOWS_CLERK_PUBLISHABLE_KEY"**
- Ensure you've added `CUSTOM_FLOWS_CLERK_PUBLISHABLE_KEY` to your `gradle.properties` file
- Verify the key starts with `pk_test_` or `pk_live_`

**MFA not working**
- Verify MFA is enabled in your Clerk dashboard
- Ensure SMS provider is configured if using SMS MFA
- Check that phone numbers are properly formatted

**OAuth authentication fails**
- Verify OAuth providers are configured in Clerk dashboard
- Ensure OAuth app credentials are correctly set up
- Check that redirect URLs are properly configured

**Email/SMS not being sent**
- Verify email/SMS providers are configured in Clerk dashboard
- Check spam/junk folders for emails
- Ensure phone numbers include proper country codes

## 🔒 Security Best Practices

- **Key Management**: Never commit your publishable key to version control in production apps
- **Environment Variables**: Use different keys for development and production environments
- **MFA**: Implement multi-factor authentication for sensitive applications
- **Error Handling**: Avoid exposing sensitive error information to users

## �� Next Steps

- Explore the [Quickstart sample](../quickstart/README.md) for basic authentication
- Check out the [Linear Clone sample](../linear-clone/README.md) for a production-ready implementation
- Read the [Custom Flows documentation](https://clerk.com/docs/custom-flows/overview) for detailed guides
- Review the [Error Handling guide](https://clerk.com/docs/custom-flows/error-handling)

## �� Related Documentation

- [Custom Flows Overview](https://clerk.com/docs/custom-flows/overview)
- [Email/Password Authentication](https://clerk.com/docs/custom-flows/email-password)
- [Multi-factor Authentication](https://clerk.com/docs/custom-flows/mfa)
- [OAuth Flows](https://clerk.com/docs/custom-flows/oauth)
- [Clerk Android SDK Reference](https://clerk-android.clerkstage.dev)
