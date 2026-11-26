# Native to Web SSO

A signed-in user of the app opens one of the developer's web pages already signed in as the same user, with no second sign-in. The SDK creates a one-time session transfer for the active session and opens a Clerk page that redeems it and then redirects to the destination. `Clerk.auth.loadInWebView(webView, destination)` loads it in the app's own `WebView`. The host has a small screen of its own for this, `NativeToWeb.kt`, which loads the instance's Account Portal and shows in a native text which user the page is signed in as.

## Sub-features

- `web-view` loads the Account Portal's `/user` page in a web view through `Clerk.auth.loadInWebView`, and the page is signed in as the native user. `Done` returns to the home, still signed in as that user. Its second test shows that an instance with session transfers off refuses the transfer with `feature_not_enabled`.

## How to get to it (user POV)

- Launch the app signed in and tap `Native to Web SSO` on the home. The screen shows the web page's user ID or the transfer's error code at the top, a `Done` button, and the web view below.

## Driving it with verify

Preconditions:

- Clerk's `session_transfers` flag must be on for the instance. It's on globally. With it off, the first test cannot turn the feature on.
- Session transfers are off on the standard settings, so the first test calls `host.useSettings({ config: { 'session_transfers.enabled': true } })`. The leaf moves nothing in the public `/v1/environment` that the CLI reads, so the call names no `environment` and goes on after the CLI's 15-second wait.
- The Native API is on in the standard settings, which a session transfer needs.
- The landing page is the instance's own Account Portal, a Clerk web app on the same instance, so no web app needs hosting. The SDK keeps the environment internal, so the screen reads `display_config.home_url` from the Frontend API's `/v1/environment` itself, at the host that the publishable key names. On a development instance that is `https://<slug>.accounts.dev/default-redirect`. The screen keeps its scheme and host and sets the path to `/user`.

- **Signed in on the web.** Run `e2e-tests/bin/control-clerk-android run native-to-web-sso --grep "same user"`. The test turns session transfers on, launches signed in with a ticket, reads the user ID from `e2e.auth.userId`, and taps `e2e.home.nativeToWeb`. It expects `e2e.nativeToWeb.webUserId` to read that ID within 30 seconds, with no `e2e.nativeToWeb.error`. Screenshot `web-view-signed-in`. It then taps `e2e.nativeToWeb.done` and expects the home with the same user.
- **Turned off.** Run `e2e-tests/bin/control-clerk-android run native-to-web-sso --grep "turned off"`. The test keeps the standard settings, launches signed in, taps `e2e.home.nativeToWeb`, and expects `e2e.nativeToWeb.error` to read `feature_not_enabled`. Screenshot `web-view-feature-off`.
- **Proof.** `specs/features/native-to-web-sso/web-view.e2e.ts` passes. The video shows the Account Portal's profile page in the web view under the native user's ID.

## States a spec cannot reach

| State | Where the app decides | What it takes |
| --- | --- | --- |
| Opening the page in the user's browser | `Auth.kt`, `openInBrowser(destination)` | The app hands the URL to a browser only, with `Intent.EXTRA_REFERRER` set to `android-app://<package>`, so the page is in another app that the harness does not read. It fails with `browser_unavailable` when no browser can open the URL. The host does not call it |
| A URL the app presents itself | `Auth.kt`, `prepareSessionTransfer(destination, presentation)` | A screen of the app's own that loads the transfer's URL. `loadInWebView` calls the same code, so the web view test covers the request, but nothing tests a caller that presents the URL itself |
| A destination refused for its scheme or host | Clerk's checks of `destination` | A production instance, which requires `https` and a host on one of its domains. The harness creates development instances only, which allow any `http` or `https` URL |
| A refused sign-in that lands on the destination | `NativeToWeb.kt`, `sessionTransferFailureCode`, from `__clerk_status=session_transfer_failed` | A transfer that Clerk creates and then refuses to redeem, such as an expired or used token, or a WebView that kept the cookies of a user who signed out natively (`Auth.kt`, the doc comment of `loadInWebView`). The refusal rides on the redirect to the destination, which the screen reads in `shouldOverrideUrlLoading`. The spec's refusal comes before loading, from the create request |
| The web session ending at native sign-out | Clerk's revocation of a transferred web session when its native session ends | A web view that outlives the native session. The screen clears cookies and web storage before each transfer, and `Done` drops the web view before a sign-out |

## Gotchas

- The web user ID comes from JavaScript on the page, `window.Clerk.user.id` once `window.Clerk.loaded` is true, not from the web view's accessibility tree. A change to how the Account Portal loads `window.Clerk` changes what the text shows.
- `evaluateJavascript` does not wait for a promise, so the screen evaluates a plain expression every 200 ms after each page load until `window.Clerk.loaded` is true. After 30 seconds without it, the screen shows the `clerk_not_loaded` error instead of `Signed out on the web`.
- `e2e.nativeToWeb.error` shows codes that do not come from Clerk's create request too (`NativeToWeb.kt`): `no_destination` when the screen could not read `home_url` from `/v1/environment`, `load_failed` for a failure with no code, `unknown` for a refusal redirect with no `__clerk_error_code`, and the SDK's own `invalid_arguments`, `signed_out`, and `session_changed` from `loadInWebView`.
- The page can finish loading more than once, with redirects after the transfer, so the screen reads the user again after each load and shows the result for the latest one.
- The screen clears every cookie and all web storage of the app's web views before the transfer, so no web session survives between tests or launches. A signed-in text always comes from this screen's transfer.
- The screen never logs or stores the transfer URL. It works once, for about a minute, and signs the user in.
- The web view draws in software. On the emulator's software GPU, a hardware-accelerated web view with no page left the whole window blank in the screenshot, although the screen tree had the error.
- The screen shows `Signed out on the web` when the page loaded but has no user, and `Loading the Account Portal` until the first answer. Neither has a test tag, so a failing wait shows them only in the screenshot.
