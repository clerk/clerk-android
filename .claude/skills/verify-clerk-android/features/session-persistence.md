# Session persistence

A signed-in user closes the app and opens it again. The app starts signed in, with the same session, and asks for nothing.

## Sub-features

- `relaunch` restores the session from the app's storage after the app process is stopped and started again.

## How to get to it (user POV)

- Sign in, close the app, and open it again.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user and signs in with a ticket on the home. It declares no settings, so it launches on the standard instance.

- **Relaunch.** Run `e2e-tests/bin/control-clerk-android run session-persistence`. The spec reads the session ID on the home (`e2e.auth.sessionId`), then launches again with `keepStorage: true` and no `signedInAs`. That launch stops the process and starts it with the same storage scope and no sign-in ticket. The spec waits for the home to show the same user, and expects the same session ID. Screenshot `relaunched`.
- **Proof.** `specs/features/session-persistence/relaunch.e2e.ts` passes. The video shows the home with the user, the app closing, and the home with the same user and session ID.

## Gotchas

- Only the second launch tests persistence. It carries no ticket, so the only way the home can show a user is the session that the SDK stored on the device.
- A launch with `keepStorage` and no `signedInAs` needs `landsOn`, because the fixture cannot know what stored state should show. The spec names `host.app.signedIn`.
- The SDK keeps its state in the shared preferences file `clerk_preferences`. The host deletes that file when a launch names a storage scope other than the one of the launch before it, which is how every other launch starts signed out. A launch with `keepStorage: true` names the same scope, so the file stays.
