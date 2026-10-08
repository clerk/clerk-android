# User button and profile

A signed-in user opens their profile from UserButton, sees their own account, and can sign out.

## Sub-features

- `sections-and-back` opens UserProfileView for the signed-in user from the home UserButton, opens Manage account and Security from it, and returns to the profile with back from each.
- `add-account` opens AuthView from the profile's `Add account` row.
- `sign-out` ends the session from the home `Sign out` button.
- `sign-out-profile` ends the session from the profile's `Sign out` row.

## How to get to it (user POV)

- Tap the avatar UserButton on the signed-in home screen.
- Tap `Sign out` on the home screen, or the `Sign out` row in the profile.

## Driving it with verify

Preconditions:

- Each test seeds a `+clerk_test` user on the standard instance and signs in with a ticket on the home (`host.launch({ signedInAs })`). The launch lands when the home shows that user's email and user ID.

- **Sections and back.** Run `control-clerk-android run user-button-and-profile`. The first test seeds a user with a phone. It taps `clerk.userButton.profile` and expects `Edit profile`. Screenshot `user-button-profile`. It taps `clerk.userProfile.row.manageAccount` and expects the user's email, `PHONE NUMBER`, the user's number as Clerk formats it, and `CONNECTED ACCOUNTS`. It presses back and expects `Edit profile`. It taps `clerk.userProfile.row.security`, expects `ACTIVE DEVICES`, presses back, and expects `Edit profile` with no `ACTIVE DEVICES`. Screenshots `manage-account-sections` and `profile-after-back`.
- **Add account.** The second test opens the profile the same way, taps `clerk.userProfile.row.addAccount`, and expects `clerk.auth.start.continue`. Screenshot `add-account`.
- **Sign out.** The third test taps `e2e.auth.signOut` and waits for the home to show `Signed out` with no user ID and no session ID, then expects `e2e.auth.signIn`. Screenshot `signed-out`.
- **Sign out from the profile.** The fourth test opens the profile, taps `clerk.userProfile.row.signOut`, and waits for the same signed-out home. The profile closes when the user is gone. Screenshot `signed-out-from-profile`.
- **Proof.** `specs/golden/user-button-and-profile/profile.e2e.ts` passes. The video shows the home name the seeded user before each test opens the profile, and `Signed out` after each sign-out.

## Gotchas

- `Edit profile`, the section headers of Manage account, and `ACTIVE DEVICES` have no test tag, so those checks use text.
- The Security row opens a list of active devices with the public IP address and city of the machine that runs the emulator.
- `clerk.userButton.profile` is a tag on the parent of the clickable node. Tap it with `tapMiddle` from `specs/compose.ts`.
- Ticket sign-in is a shortcut, not the feature. It is fine here because this feature is not about authentication.
- The host shows a spinner while the ticket sign-in runs. `host.launch` returns once the home shows the user, and fails with the reason when the sign-in fails.
