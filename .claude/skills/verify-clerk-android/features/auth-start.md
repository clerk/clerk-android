# Auth start

A signed-out user opens AuthView and sees the start screen, where they enter an email, username, or phone number.

## Sub-features

- `full-screen` opens AuthView as the whole content of the activity from the home's `Sign in full screen` button.
- `initial-identifier` opens AuthView on the phone field, holding the number the app passed as `initialIdentifier`.

## How to get to it (user POV)

- Launch the app signed out and tap `Sign in`.
- Launch the app signed out and tap `Sign in full screen`. The sign-in screen has no close button.
- Open the sign-in screen from an app that already knows the phone number. The screen opens with that number in it.

## Driving it with verify

Preconditions:

- The first test needs no user. The second seeds one with `host.seedUser({ phone: true, password: true })`. The spec declares no settings, so it launches on the standard instance, signed out.

- **Full screen.** Run `e2e-tests/bin/control-clerk-android run auth-start`. The first test launches, taps `e2e.auth.signInFullScreen`, and expects `clerk.auth.start.identifier` and `clerk.auth.start.continue` with no `clerk.dismissButton`. Screenshot `auth-full-screen`, which shows AuthView as the whole content of the activity.
- **Initial identifier.** The second test launches with `authMode: 'signIn'` and the user's phone as `initialIdentifier`, and taps `e2e.auth.signInFullScreen`, and the host shows `AuthView(initialIdentifier = <that phone>)`. AuthView then opens on the phone field and shows no `clerk.auth.start.identifier`. The test expects `clerk.auth.start.phoneNumber` to hold the number as Clerk formats it and `clerk.auth.start.identifier` to be absent, then taps `clerk.auth.start.continue` and expects `clerk.auth.signIn.password`. Screenshots `auth-initial-identifier` and `auth-initial-identifier-accepted`.
- **Proof.** `specs/golden/auth-start/auth-start.e2e.ts` passes. The run directory holds `video.mp4` and the three screenshots.

## Gotchas

- A missing or malformed publishable key shows the host's error screen, `Something went wrong` with the reason, instead of an empty AuthView. `host.launch` fails with that reason.
- AuthView remembers identifiers by default. The host turns that off with `persistIdentifiers = false`, so the field starts empty on every launch.
- The identifier is a launch input, `verifyInitialIdentifier`. The host has no screen of its own for it, and only the AuthView behind `Sign in full screen` gets it.
- `asShownByClerk(phone)` in `specs/phone.ts` gives the formatted text the phone field shows, `+1 201-555-01xx`.
