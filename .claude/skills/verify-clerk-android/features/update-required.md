# Update required

An instance sets a minimum version for an app. Clerk rejects most requests from a build below it, and the SDK covers the app with an update screen that shows the build's version and links to Google Play. Users stay signed in, so they continue where they left off once the build is allowed again.

## Sub-features

- `signed-in` covers a signed-in user's app with the update screen on a build below the minimum, showing `Your version` and `Go to Google Play`. Lowering the minimum returns the same user and session.
- `signed-out` covers a signed-out user's app with the update screen too.
- `at-the-minimum` leaves a build at the minimum to open as usual.
- `update-app` shows `Update app` instead of `Go to Google Play` when the update URL is on another host.

## How to get to it (user POV)

- A developer sets a minimum on the Clerk Dashboard's `Native applications` page, then a user opens a build below it.

## Driving it with verify

Preconditions:

- The native app settings are off the standard settings, so each test calls `host.useMinimumAppVersion({ minimumVersion, updateUrl })`. It registers `com.clerk.e2e` with the application, sets the minimum, and waits for the public environment to show it. When the test ends, the CLI removes the minimum and leaves the registration.
- Clerk's `native_app_force_update` flag must be on for the instance. It's on globally, and with it off the call fails.
- The test app's `versionCode` is `1` and its `versionName` is `1.0`, in `e2e/build.gradle.kts`. A minimum is a whole version code, so the spec uses `2` to block it. The screen shows the version name.

- **Signed in.** Run `e2e-tests/bin/control-clerk-android run update-required --grep "signed-in user"`. The spec signs in with a ticket, sets the minimum to `2`, and relaunches with `keepStorage: true`. It expects `1.0` and `Go to Google Play` (screenshot `update-required`). It then lowers the minimum to `1`, relaunches, and expects the same user and session ID on the home (screenshot `signed-in-after-lowering`).
- **Signed out.** Run `e2e-tests/bin/control-clerk-android run update-required --grep "signed-out user"`. The spec sets the minimum to `2` before the first launch, then expects the update screen and `1.0` (screenshot `update-required-signed-out`).
- **At the minimum.** Run `e2e-tests/bin/control-clerk-android run update-required --grep "at the minimum"`. The spec sets the minimum to `1`, signs in with a ticket, and expects the signed-in home with no update screen (screenshot `at-the-minimum`).
- **Update app.** Run `e2e-tests/bin/control-clerk-android run update-required --grep "outside Google Play"`. The spec sets the minimum to `2` with an update URL on `example.com`, then expects `Update app` and no `Go to Google Play` (screenshot `update-app`).
- **Proof.** `specs/features/update-required/update-required.e2e.ts` passes. The video shows the update screen with `1.0`, then the home with the same user and session ID.

## States a spec cannot reach

| State | Where the app decides | What it takes |
| --- | --- | --- |
| `Can't update? Get help` | `UpdateRequiredView.kt`, when `supportEmail` is not null | A support email on the instance. The Platform API config has no leaf for it, so a spec cannot set one |
| The screen with no store button | `UpdateRequiredView.kt`, `status.updateUrl?.let` | A minimum with no update URL. Clerk requires one (`validateNativeAppMinimum` in clerk_go), so only a local patch reaches it |
| An app's own screen instead of Clerk's | `withUpdateRequiredViewDisabled()` | A call to it in the test app, and a home that shows `appVersionSupportStatus` |
| The debug warning that the update screen can't show | `UpdateRequiredViewLauncher.kt`, `warnOnceAppHasMinimum` | A debuggable app that has a minimum and doesn't include `com.clerk:clerk-android-ui`. The `:e2e` host includes it, so only a local patch that removes it reaches the warning. It's a log line with no UI |

## Gotchas

- A build with no `versionCode`, or one below `1`, is never blocked: `AppBuild.from` in `AppVersionSupport.kt` logs a warning and the SDK skips the check. A spec on such a build would pass `at the minimum` and fail the others.
- `Go to Google Play` and `Update app` open an outside app, so the spec checks that the text shows and does not tap it. `storeButtonText` picks the label by the update URL's host.
- The button's label is a text node beside an unnamed button, so `getByRole('button', { name })` does not find it. The spec uses `getByText`.
- The screen has no test tags of its own, so the spec finds it by its text. A change to that wording changes the spec.
- A minimum applies to the next request or launch. The spec relaunches instead of waiting for a running app to foreground.
- A relaunch restores the cached environment first, and `AppVersionSupport.applyEnvironment` runs for it too. So right after the minimum is lowered, the update screen can open for a moment on the old minimum. `UpdateRequiredActivity` closes once `Clerk.isInitialized` is true and the status is supported, and `landsOn: host.app.signedIn` waits through that. Don't assert that the update screen never appears after that relaunch.
- The subtitle names the application, which here is the `verify-throwaway-...` name. Don't assert on it.
- The update screen replaces what the home shows, so the home's locators aren't on screen while it shows. A launch that expects it needs `landsOn`.
