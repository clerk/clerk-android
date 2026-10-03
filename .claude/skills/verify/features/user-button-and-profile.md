# User button and profile

A signed-in user opens their profile from UserButton or from a profile screen, sees their own account, and can sign out.

## Sub-features

- `profile-direct` shows UserProfileView for the signed-in user.
- `add-account` opens AuthView from the profile's `Add account` row.
- `user-button` opens the profile from the home UserButton.
- `sign-out` ends the session from the home `Sign Out` button.
- `sign-out-profile` ends the session from the profile's `Sign out` row.

## How to get to it (user POV)

- Tap the avatar UserButton on the signed-in home screen.
- Open a screen that shows UserProfileView as its root (`verifyScreen userProfile`).
- Tap `Sign Out` on the home screen, or the `Sign out` row in the profile.

## Driving it with verify

Preconditions:

- Each test seeds a `+clerk_test` user on `with-email-codes` and signs in with a ticket (`host.launch({ signedInAs })`).

- **Profile.** Run `bin/verify run user-button-and-profile`. The first test launches `screen: 'userProfile'`, checks `userId` and `sessionStatus` `active`, expects `Edit profile`, taps `clerk.userProfile.row.manageAccount`, and expects the user's email. Screenshot `profile`.
- **Add account.** The second test taps `clerk.userProfile.row.addAccount` and expects `clerk.auth.start.continue`. Screenshot `add-account`.
- **User button.** The third test launches `screen: 'home'`, taps `clerk.userButton.profile`, and expects `clerk.userProfile.row.manageAccount`. Screenshot `user-button-profile`.
- **Sign out.** The fourth test taps `verify.signOut` and waits up to 10 seconds for `signedIn` false, then expects `Prebuilt UI Sign In`.
- **Sign out from the profile.** The fifth test taps `clerk.userProfile.row.signOut` and waits for `signedIn` false and `sessionId` null.
- **Proof.** `specs/golden/user-button-and-profile/profile.e2e.ts` passes. `states.jsonl` holds each test's states in order, with the seeded user's id for the profile and user-button tests. `state.json` holds only the run's last state, which is signed out after the sign-out tests.

## Gotchas

- `Edit profile` and the home `Prebuilt UI Sign In` button have no test tags, so those two checks use text.
- The Security row opens a list of active devices with this Mac's public IP address and city. Do not screenshot it, and keep it out of videos you attach to a public PR.
- `verify.signOut` is a tagged Compose Material `Button`. Tap it with `host.tap`; `locator.tap()` can refuse because the tagged node's `android.widget.Button` child is not clickable.
- Ticket sign-in is a shortcut, not the feature. It is fine here because this feature is not about authentication.
