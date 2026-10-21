# Organizations

A signed-in user creates an organization from OrganizationSwitcher and it becomes their active organization. From the switcher, the user moves between the organization and their personal account, and opens the organization's profile.

## Sub-features

- `create-from-switcher` creates an organization from the switcher sheet and makes it active.
- `switch-account` makes the personal account active from the switcher's account list, then the organization again.
- `Manage organization` (`clerk.organizationSwitcher.manageOrganization`) opens the organization's profile, which [Organization profile](./organization-profile.md) covers.

## How to get to it (user POV)

- Tap the OrganizationSwitcher, tap `Create organization`, type a name, tap the `Create organization` button, then close the invite step.
- With an organization active, tap the OrganizationSwitcher. The sheet shows the organization with `Manage organization` and `Switch account`. `Switch account` opens the account list, with the personal account first, then a row for each organization. Tap a row to make it active. With no organization active, the switcher opens the account list directly.

## Driving it with verify

Preconditions:

- Each spec seeds a `+clerk_test` user on the standard instance and signs in with a ticket. The launch lands on the home, where the OrganizationSwitcher is.

- **Create.** Run `e2e-tests/bin/control-clerk-android run organizations`. The launch lands on the home with the user's email and user ID. The spec expects the user's email on `clerk.organizationSwitcher.trigger`, which is what the switcher reads while no organization is active and the user has no name. It taps the switcher and `clerk.organization.accountList.createOrganization`, fills `clerk.organization.profileForm.name` with `host.newOrganizationName()`, such as `Harbor 7c1e`, taps `clerk.organization.profileForm.submit`, and closes the invite step. It waits up to 30 seconds for the home to show the same user again, and expects the organization's name on the switcher and the email gone from it. Screenshot `org-created`.
- **Switch.** The same command runs `switch-account.e2e.ts`. It creates an organization with `signInWithNewOrganization` from `specs/compose.ts`, which ends with the organization's name on the switcher. It taps the switcher and `clerk.organizationSwitcher.switchAccount`, and expects `clerk.organization.accountList.membership`. Screenshot `account-list`. It taps `clerk.organization.accountList.personalAccount`, and expects the user's email on the switcher and the organization's name gone. Screenshot `personal-active`. It taps the switcher, which now opens the account list, taps `clerk.organization.accountList.membership`, and expects the organization's name again. Screenshot `org-active-again`.
- **Proof.** `create-from-switcher.e2e.ts` and `switch-account.e2e.ts` pass. `org-created` shows the switcher with the new organization's name under the user's email and IDs, `personal-active` shows the email on the switcher, and `org-active-again` shows the organization's name on it again. `e2e-tests/bin/control-clerk-android down` deletes the worktree's application, and the organization and its owner go with it.

## Gotchas

- The invite step after creation is a full screen with the title `Invite new members`. It shows only while the organization may have more than one member, which the standard settings allow. It has no test tag and no Skip button. The spec closes it with `host.tap(middleOf(screen.getByLabel('Close')))`.
- The screen tree leaves out what a sheet or a full-screen step covers, so the home cannot be read while the switcher's sheet or the invite step is up. `host.expectSignedInAs` waits for the invite step to close.
- The switcher loads the user's organizations when it first shows and when the session or the user changes. It loads them again when its create flow is dismissed or moves on to the invite step, and when its organization profile completes because the organization is gone, such as after the user leaves or deletes it. Closing the profile any other way does not reload the list. An organization that the app creates some other way, such as through `onCreateOrganization`, is not in the list until one of those happens.
- `Create organization` is the text of the account list row, the title of the create screen, and the label of its button. Tap the row and the button by their tags.
- The slug of an organization comes from its name, and Clerk refuses a slug that another organization in the instance holds. Give each test that creates an organization its own name. [Organization profile](./organization-profile.md) has what the refusal looks like.
- The switcher never says `Personal account` for a seeded user. Its trigger and its sheet show the user's name, then the username, then the email, whichever the user has first. A seeded user has only the email. The spec looks for the email inside the trigger and not on the whole screen, where `Signed in as` also holds it.
