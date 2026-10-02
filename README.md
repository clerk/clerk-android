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
[![Follow on X](https://img.shields.io/twitter/follow/clerk?style=social)](https://x.com/intent/follow?screen_name=clerk)

---

## Usage

### Creating a Clerk Application

1. [Sign up for an account](https://dashboard.clerk.com/sign-up?utm_source=github&utm_medium=clerk_android_repo_readme)
2. Create an application in your Clerk dashboard
3. Copy your **Publishable Key** from the API Keys section

### Installation

The Clerk Android SDK is available as two separate artifacts:

- **`clerk-android-api`** – Core API and authentication logic (required)
- **`clerk-android-ui`** – Prebuilt Jetpack Compose UI components (optional, includes API)

Add the desired artifact to your app's `build.gradle(.kts)`:

#### API Only

Use this if you want to build your own custom UI:

```kotlin
dependencies {
    implementation("com.clerk:clerk-android-api:<latest-version>")
}
```

#### With Prebuilt UI Components

Use this if you want to use the prebuilt Jetpack Compose UI components:

```kotlin
dependencies {
    implementation("com.clerk:clerk-android-ui:<latest-version>")
}
```

> 💡 **Tip:** Check Maven Central for the latest versions:
> [clerk-android-api](https://central.sonatype.com/artifact/com.clerk/clerk-android-api) |
> [clerk-android-ui](https://central.sonatype.com/artifact/com.clerk/clerk-android-ui)

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

#### Prebuilt UI
`samples/prebuilt-ui`: This is an example that shows how to integrate the Clerk prebuilt UI components. See the [README](samples/prebuilt-ui/README.md) for more info.

## Documentation

- [Reference Documentation](https://clerk-android.clerkstage.dev)
- [Clerk Docs](https://clerk.com/docs)
- [Android Integration Guide](https://clerk.com/docs/quickstarts/android)

## Developing this SDK

The build runs on **JDK 21** (Robolectric, Paparazzi and the Paparazzi Gradle plugin require it).
`gradle/gradle-daemon-jvm.properties` makes Gradle and Android Studio pick a local JDK 21 even when
`JAVA_HOME` points elsewhere. Published bytecode still targets Java 17 (`jvmTarget` in
`gradle/libs.versions.toml`).

Run `./bootstrap.sh` once (it also works from a git worktree) to install the pre-commit hook that
formats and checks staged files. Re-run it when `config/git/hooks/pre-commit` changes.

CI runs these on every pull request, and the release workflow runs them before publishing:

```bash
./gradlew :source:api:testDebugUnitTest :source:ui:testDebugUnitTest :source:telemetry:testAndroidHostTest
./gradlew spotlessCheck   # ./gradlew spotlessApply to fix
./gradlew detekt :source:api:detektDebug :source:ui:detektDebug :source:telemetry:detektMainAndroid
./gradlew :source:api:lint :source:ui:lint :source:telemetry:lint
./gradlew verifyPublishedArtifacts
```

## License

This project is licensed under the **MIT license**.

See [LICENSE](https://github.com/clerk/clerk-android/blob/main/LICENSE) for more information.
