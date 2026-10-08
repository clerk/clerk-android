# Organizations

A signed-in user creates an organization from OrganizationSwitcher and it becomes their active organization.

## Sub-features

- `create-from-switcher` creates an organization from the switcher sheet and makes it active.

## How to get to it (user POV)

- Tap the OrganizationSwitcher, tap `Create organization`, type a name, tap the `Create organization` button, then close the invite step.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user on the standard instance and signs in with a ticket. The launch lands on the home, where the OrganizationSwitcher is.

- **Create.** Run `e2e-tests/bin/control-clerk-android run organizations`. The launch lands on the home with the user's email and user ID. The spec expects the user's email on `clerk.organizationSwitcher.trigger`, which is what the switcher reads while no organization is active and the user has no name. It taps the switcher and `clerk.organization.accountList.createOrganization`, fills `clerk.organization.profileForm.name` with `Verify <runId>`, taps `clerk.organization.profileForm.submit`, and closes the invite step. It waits up to 30 seconds for the home to show the same user again, and expects the organization's name on the switcher and the email gone from it. Screenshot `org-created`.
- **Proof.** `create-from-switcher.e2e.ts` passes, and the screenshot shows the switcher with the new organization's name under the user's email and IDs. `e2e-tests/bin/control-clerk-android down` deletes the worktree's application, and the organization and its owner go with it.

## Gotchas

- The invite step after creation is a full screen with the title `Invite new members`. It shows only while the organization may have more than one member, which the standard settings allow. It has no test tag and no Skip button. The spec closes it with `tapMiddle(screen.getByLabel('Close'))`.
- The screen tree leaves out what a sheet or a full-screen step covers, so the home cannot be read while the switcher's sheet or the invite step is up. `host.expectSignedInAs` waits for the invite step to close.
- The switcher never says `Personal account` for a seeded user. Its trigger and its sheet show the user's name, then the username, then the email, whichever the user has first. A seeded user has only the email. The spec looks for the email inside the trigger and not on the whole screen, where `Signed in as` also holds it.
