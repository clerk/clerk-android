<p align="center">
  <a href="https://clerk.com?utm_source=github&utm_medium=clerk_android" target="_blank" rel="noopener noreferrer">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://images.clerk.com/static/logo-dark-mode-400x400.png">
      <img src="https://images.clerk.com/static/logo-light-mode-400x400.png" height="64">
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

## Usage

### Creating a Clerk Application

1. [Sign up for an account](https://dashboard.clerk.com/sign-up?utm_source=github&utm_medium=clerk_android_repo_readme)
2. Create an application in your Clerk dashboard
3. Copy your **Publishable Key** from the API Keys section

### Installation

Add the Clerk Android SDK to your app's `build.gradle(.kts)`


```kotlin
dependencies {
    implementation("com.clerk:clerk-android:0.1.4")
}
```

> 💡 **Tip:** Check [Maven Central](https://central.sonatype.com/artifact/com.clerk/clerk-android)
> for the latest version.


### Initialization

Before using any part of the SDK, you must call `Clerk.initialize()` in your Application class with
your publishable key and application context:

```kotlin
import com.clerk.Clerk

class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Clerk.initialize(
            context = this,
            publishableKey = "pk_test_..." // Your publishable key from Clerk Dashboard
        )
    }
}
```

**Don't forget to register your Application class in `AndroidManifest.xml`:**

```xml

<application
    android:name=".YourApplication" 
    android:theme="@style/AppTheme">
    <!-- ... -->
</application>
```

### Initialize with custom options
```kotlin
import com.clerk.Clerk

class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Clerk.initialize(
            context = this,
            publishableKey = "pk_test_..." // Your publishable key from Clerk Dashboard,
            options = ClerkConfigurationOptions(enableDebugMode = true),
        )
    }
}
```

## Samples

#### Quickstart
`samples/quickstart`: This is a paired repo with the [Android Quickstart guide](https://clerk.com/docs/quickstarts/android). It provides a simple
example of how to integrate Clerk into an Android application, demonstrating user sign-up, sign-in,
and profile management. See the [README](samples/quickstart/README.md) for more info

#### Custom flows
`samples/custom-flows`: This is a paired repo with the [Custom Flows guide](https://clerk.com/docs/custom-flows/overview). It showcases how to
implement custom authentication flows using Clerk, including advanced scenarios like multi-factor
authentication and reset password. See the [README](samples/custom-flows/README.md) for more info

#### Linear Clone
`samples/linear-clone`: This is an example that shows how you might integrate with compose navigation, it is a native recreation of the Linear auth flow (which is web based)
and includes Sign in with Google, Passkey authentication, Sign out, and Email Code Authentication. See the [README](samples/linear-clone/README.md) for more info. 

## Documentation

- [Reference Documentation](https://clerk-android.clerkstage.dev)
- [Clerk Docs](https://clerk.com/docs)
- [Android Integration Guide](https://clerk.com/docs/quickstarts/android)

## 📝 License

This project is licensed under the **MIT license**.

See [LICENSE](https://github.com/clerk/clerk-android/blob/main/LICENSE) for more information.
