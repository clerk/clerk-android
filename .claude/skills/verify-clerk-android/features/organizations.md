# Organizations

A signed-in user creates an organization from OrganizationSwitcher and it becomes their active organization.

## Sub-features

- `create-from-switcher` creates an organization from the switcher sheet and makes it active.

## How to get to it (user POV)

- Tap the OrganizationSwitcher, tap `Create organization`, type a name, tap Continue, then close the invite step.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user on the standard instance and launches `screen: 'orgSwitcher'` with a ticket.

- **Create.** Run `control-clerk-android run organizations`. It checks `orgId` null, taps `clerk.organizationSwitcher.trigger` and `clerk.organization.accountList.createOrganization`, fills `clerk.organization.profileForm.name` with `Verify <runId>`, taps `clerk.organization.profileForm.submit`, closes the invite sheet, waits up to 30 seconds for a non-null `orgId`, and expects the name on the switcher. Screenshot `org-created`.
- **Proof.** `create-from-switcher.e2e.ts` passes, and `state.json` holds the new `orgId` with the seeded `userId`. `control-clerk-android down` deletes the worktree's application, and the organization and its owner go with it.

## Gotchas

- The invite step after creation has no test tag and no Skip button. The spec closes it with `screen.getByLabel('Close')`.
- The footer is not readable while the switcher sheet is open, so the spec waits for `orgId` after the sheet closes.
