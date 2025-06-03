<p align="center">
  <a href="https://clerk.com?utm_source=github&utm_medium=clerk_ios" target="_blank" rel="noopener noreferrer">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://images.clerk.com/static/logo-dark-mode-400x400.png">
      <img src="https://images.clerk.com/static/logo-light-mode-400x400.png" height="260">
    </picture>
  </a>
  <br />
</p>
<h1 align="center">
  Official Clerk Android SDK
</h1>
<p align="center">
  <strong>
    Clerk helps developers build user management. We provide streamlined user experiences for your users to sign up, sign in, and manage their profile.
  </strong>
</p>

[![chat on Discord](https://img.shields.io/discord/856971667393609759.svg?logo=discord)](https://clerk.com/discord)
[![documentation](https://img.shields.io/badge/documentation-clerk-green.svg)](https://clerk.com/docs)
[![twitter](https://img.shields.io/twitter/follow/ClerkDev?style=social)](https://twitter.com/intent/follow?screen_name=ClerkDev)

---

**Clerk is Hiring!**

Would you like to work on Open Source software and help maintain this repository? [Apply today!](https://jobs.ashbyhq.com/clerk)

---

## 🚀 Get Started with Clerk

1. [Sign up for an account](https://dashboard.clerk.com/sign-up?utm_source=github&utm_medium=clerk_ios_repo_readme)
2. Create an application in your Clerk dashboard

## 🧑‍💻 Installation
The Clerk Android SDK is distributed via Maven Central. To add the Clerk SDK to your project, first ensure you have added [mavenCentral](https://docs.gradle.org/current/userguide/declaring_repositories.html) to your project's `build.gradle(.kts)`, then add the Clerk SDK to your application's dependencies:
```gradle
dependencies {
    ...
    implementation(com.clerk:clerk-android:<latest-version>)
    ...
}
```

## 🛠️ Usage
Before using any part of the SDK, you must, call `.initialize` and pass in your publishable key and an application context. The publishable key is used to associate your application with Clerk's backend and the application context is used for saving the long-lived token and registering activities/receivers/deeplinks.

```kotlin
import com.clerk.Clerk

class ClerkPlaygroundApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    ...
    Clerk.initialize(
      context = this,
      publishableKey = your_publishable_key,
    )
    ...
  }
}
```
Now you can conditionally render content based on the user's session:
```kotlin
import com.clerk.Clerk

if (Clerk.user != null) {
  Text("Hello, ${Clerk.user.id}"
} else {
  Text("You are signed out")
}
```

### Authentication
If a function takes any parameters those parameters are generally exposed as a data class or enum named after the function itself. i.e. parameters for `SignUp.Create` are exposed as `SignUp.SignUpCreateParams` this follows the same pattern as other Clerk SDKs.

#### Sign Up with Email and Perform Verification
```kotlin
// Create a sign up
SignUp.create(SignUpCreateParams.Standard(emailAddress))
  .onSuccess { signUp ->
    // Check if the SignUp needs the email address verified and send an OTP code via email.
      if (signUp.unverifiedFields.contains("email_address")) {
        signUp.prepareVerification(SignUp.PrepareVerificationParams.EMAIL_CODE)
      }
    }
    .onFailure {
        // Log the error
    }
  }

// After collecting the OTP from the user
Clerk.signUp
  .attemptVerification(SignUp.AttemptVerificationParams.EmailCode(code))
  .onSuccess {
      // User is signed in.
  }
```

#### Sign In with OAuth(e.g. Google, GitHub, etc.)
Clerk will handle the OAuth flow and deep linking for you, you just need to pass it the context of your application.
```kotlin
SignIn.authenticateWithRedirect(context, OAuthProvider.GOOGLE)
```

#### Forgot Password
```kotlin
// Create a sign in and send an OTP code to verify the user owns the email
SignIn.create(Strategy.EmailCode("user@example.com"))

// After collecting the OTP code from the user, attempt verification
Clerk.signIn.attemptFirstFactor(Strategy.ResetPasswordEmailCode("123456"))
  .onSuccess { signIn ->
    // Set a new password to complete the process
    signIn.resetPassword(password = "********", signOutOfOtherSessions = true)
  }
```

#### Sign Out
```kotlin
Clerk.signOut()
```

## 🚢 Release Notes

Curious what we shipped recently? Check out our [changelog](https://clerk.com/changelog)!

<!---
## 🤝 How to Contribute

We're open to all community contributions! If you'd like to contribute in any way, please read [our contribution guidelines](https://github.com/clerk/javascript/blob/main/docs/CONTRIBUTING.md). We'd love to have you as part of the Clerk community!
-->

## 📝 License

This project is licensed under the **MIT license**.

See [LICENSE](https://github.com/clerk/javascript/blob/main/LICENSE) for more information.
