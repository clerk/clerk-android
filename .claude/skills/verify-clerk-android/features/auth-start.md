# Auth start

A signed-out user opens AuthView and sees the start screen, where they enter an email, username, or phone number.

## Sub-features

- `full-screen` opens AuthView as the whole content of the activity from the home's `Sign in full screen` button.
- `initial-identifier` opens AuthView on the phone field, holding the number the app passed as `initialIdentifier`.
- `dismiss` closes the AuthView that the home's `Sign in` button opens with its close button, shows the home again, and opens AuthView again.

## How to get to it (user POV)

- Launch the app signed out and tap `Sign in`.
- Launch the app signed out and tap `Sign in full screen`. The sign-in screen has no close button.
- Open the sign-in screen from an app that already knows the phone number. The screen opens with that number in it.
- After `Sign in`, tap the close button at the top of the sign-in screen to go back to the home.

## Driving it with verify

Preconditions:

- The first and third tests need no user. The second seeds one with `host.seedUser({ phone: true, password: true })`. The spec declares no settings, so it launches on the standard instance, signed out.

- **Full screen.** Run `e2e-tests/bin/control-clerk-android run auth-start`. The first test launches, taps `e2e.auth.signInFullScreen`, and expects `clerk.auth.start.identifier` and `clerk.auth.start.continue` with no `clerk.dismissButton`. Screenshot `auth-full-screen`, which shows AuthView as the whole content of the activity.
- **Initial identifier.** The second test launches with `authMode: 'signIn'` and the user's phone as `initialIdentifier`, and taps `e2e.auth.signInFullScreen`, and the host shows `AuthView(initialIdentifier = <that phone>)`. AuthView then opens on the phone field and shows no `clerk.auth.start.identifier`. The test expects `clerk.auth.start.phoneNumber` to hold the number as Clerk formats it and `clerk.auth.start.identifier` to be absent, then taps `clerk.auth.start.continue` and expects `clerk.auth.signIn.password`. Screenshots `auth-initial-identifier` and `auth-initial-identifier-accepted`.
- **Home button and dismiss.** The third test launches, expects `Signed out` on the home, taps `e2e.auth.signIn`, and expects `clerk.auth.start.identifier` and `clerk.auth.start.continue`. Screenshot `auth-from-home`. It then taps `clerk.dismissButton` with `tapMiddle`, waits for the home to show `Signed out`, and expects no `clerk.auth.start.identifier`. Screenshot `auth-dismissed`. It then taps `e2e.auth.signIn` again and expects the identifier field.
- **Proof.** `specs/features/auth-start/auth-start.e2e.ts` passes. The run directory holds `video.mp4` and the five screenshots.

## Gotchas

- A missing or malformed publishable key shows the host's error screen, `Something went wrong` with the reason, instead of an empty AuthView. `host.launch` fails with that reason.
- The home and the AuthView behind `Sign in` are never on screen together, so `Signed out` on the home is how the dismiss test knows that AuthView has closed.
- `clerk.dismissButton` is an icon button, and its whole box belongs to its icon. A plain tap fails with "no parent-owned touch point outside its interactive descendants", so the test taps it with `tapMiddle` from `specs/compose.ts`.
- AuthView remembers identifiers by default. The host turns that off with `persistIdentifiers = false`, so the field starts empty on every launch.
- The identifier is a launch input, `verifyInitialIdentifier`. The host has no screen of its own for it, and only the AuthView behind `Sign in full screen` gets it.
- `asShownByClerk(phone)` in `specs/phone.ts` gives the formatted text the phone field shows, `+1 201-555-01xx`.
