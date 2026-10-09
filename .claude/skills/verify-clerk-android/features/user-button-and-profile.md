# User button and profile

A signed-in user opens their profile from UserButton, sees their own account, and can sign out or delete the account.

## Sub-features

- `sections-and-back` opens UserProfileView for the signed-in user from the home UserButton, opens Manage account and Security from it, and returns to the profile with back from each.
- `add-account` opens AuthView from the profile's `Add account` row.
- `sign-out` ends the session from the home `Sign out` button.
- `sign-out-profile` ends the session from the profile's `Sign out` row.
- `delete-account` deletes the account from the profile's Security screen.

## How to get to it (user POV)

- Tap the avatar UserButton on the signed-in home screen.
- Tap `Sign out` on the home screen, or the `Sign out` row in the profile.
- Open the profile, tap `Security`, tap `Delete account`, type `DELETE`, and tap `Delete account` in the sheet.

## Driving it with verify

Preconditions:

- Each test seeds a `+clerk_test` user on the standard instance and signs in with a ticket on the home (`host.launch({ signedInAs })`). The launch lands when the home shows that user's email and user ID.

- **Sections and back.** Run `e2e-tests/bin/control-clerk-android run user-button-and-profile`. The first test seeds a user with a phone. It taps `clerk.userButton.profile` and expects `Edit profile`. Screenshot `user-button-profile`. It taps `clerk.userProfile.row.manageAccount` and expects the user's email, `PHONE NUMBER`, the user's number as Clerk formats it, and `CONNECTED ACCOUNTS`. It presses back and expects `Edit profile`. It taps `clerk.userProfile.row.security`, expects `ACTIVE DEVICES`, presses back, and expects `Edit profile` with no `ACTIVE DEVICES`. Screenshots `manage-account-sections` and `profile-after-back`.
- **Add account.** The second test opens the profile the same way, taps `clerk.userProfile.row.addAccount`, and expects `clerk.auth.start.continue`. Screenshot `add-account`.
- **Sign out.** The third test taps `e2e.auth.signOut` and waits for the home to show `Signed out` with no user ID and no session ID, then expects `e2e.auth.signIn`. Screenshot `signed-out`.
- **Sign out from the profile.** The fourth test opens the profile, taps `clerk.userProfile.row.signOut`, and waits for the same signed-out home. The profile closes when the user is gone. Screenshot `signed-out-from-profile`.
- **Delete account.** Run `e2e-tests/bin/control-clerk-android run user-button-and-profile/delete-account`. The spec launches with `authMode: 'signIn'`, opens the profile, taps `clerk.userProfile.row.security`, and taps `Delete account`. It expects the sheet's warning, `Are you sure you want to delete your account? This action is permanent and irreversible.`, types `DELETE` into the one text box of the sheet, and taps the sheet's `Delete account` button. It waits for the home to show `Signed out`. It then taps `e2e.auth.signIn`, enters the deleted user's email, taps `clerk.auth.start.continue`, and expects `Couldn't find your account.`. Screenshot `account-deleted`.
- **Proof.** `specs/golden/user-button-and-profile/profile.e2e.ts` and `delete-account.e2e.ts` pass. The video shows the home name the seeded user before each test opens the profile, and `Signed out` after each sign-out.

## Gotchas

- `Edit profile`, the section headers of Manage account, and `ACTIVE DEVICES` have no test tag, so those checks use text.
- The Security row opens a list of active devices with the public IP address and city of the machine that runs the emulator. No spec takes a screenshot while that list is on screen, and the video of a run shows it.
- The Security screen and the delete sheet have no test tags. `Delete account` is the text of the row on the Security screen, and of both the title and the button of the sheet. The screen tree leaves out what a sheet covers, so the text matches one node before the sheet opens and two after. The spec taps the row while it is the only match and the button with `.last()`. The confirmation field is the only `textbox` in the sheet. The button stays disabled until the field holds `DELETE`.
- `Signed out` after a deletion looks the same as after a sign-out. The sign-in attempt with the deleted email is what shows that the account is gone. It needs `authMode: 'signIn'`, because the default mode would start a sign-up for an unknown email.
- `Couldn't find your account.` shows in a message at the bottom of the sign-in screen that closes itself after a few seconds. It is Clerk's wording, so a change of it fails the spec.
- A new application has reverification on, and the Platform API has no setting for it. Clerk accepts the deletion without another verification because the ticket sign-in happened seconds earlier. A spec that deletes the account long after its sign-in can be asked to verify again.
- `clerk.userButton.profile` is a tag on the parent of the clickable node. Tap it with `tapMiddle` from `specs/compose.ts`.
- Ticket sign-in is a shortcut, not the feature. It is fine here because this feature is not about authentication.
- The host shows a spinner while the ticket sign-in runs. `host.launch` returns once the home shows the user, and fails with the reason when the sign-in fails.
