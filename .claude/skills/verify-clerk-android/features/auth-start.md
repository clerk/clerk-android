# Auth start

A signed-out user opens AuthView and sees the start screen, where they enter an email, username, or phone number.

## Sub-features

- `home-button` opens AuthView from the host home's `Prebuilt UI Sign In` button.
- `direct-launch` opens AuthView as the root screen with `verifyScreen auth`.

## How to get to it (user POV)

- Launch the app signed out and tap `Prebuilt UI Sign In`.
- Launch straight into the sign-in screen.

## Driving it with verify

Preconditions:

- No user. The spec launches `with-email-codes` signed out.

- **Home button.** Run `.claude/skills/verify-clerk-android/bin/control-clerk-android run auth-start`. The first test launches `screen: 'home'`, checks `environmentLoaded` and `signedIn` false, taps `Prebuilt UI Sign In`, and expects `clerk.auth.start.continue`. Screenshot `auth-start`.
- **Direct launch.** The second test launches `screen: 'auth'`, checks `state.screen` is `auth` with no `lastError`, and expects `clerk.auth.start.identifier` and `clerk.auth.start.continue`. Screenshot `auth-start-identifier`, which shows the identifier field.
- **Proof.** `specs/golden/auth-start/auth-start.e2e.ts` passes, and `states.jsonl` shows `environmentLoaded` true for both launches.

## Gotchas

- The host's `Prebuilt UI Sign In` button has no test tag, so the spec finds it by text and taps it with `host.tap`. Tapping the text with `locator.tap()` fails with "no parent-owned touch point", because the label is a sibling of the button node.
- The home button's AuthView is seeded with the test phone `+15555550100`, so it opens on the phone field (`clerk.auth.start.phoneNumber`), not the identifier field. A direct `auth` launch opens on the identifier field.
