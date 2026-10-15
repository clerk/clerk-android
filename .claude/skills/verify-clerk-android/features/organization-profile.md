# Organization profile

A member of the active organization opens its profile from OrganizationSwitcher's `Manage organization`, renames the organization, sees its members, invites people, and reaches its verified domains. Which rows and tabs show depends on the member's permissions in the organization.

## Sub-features

- `profile` shows the creator every row of the profile: `Update profile` under the organization's name, `Members`, `Verified domains`, `Leave organization`, and `Delete organization`. `Verified domains` shows the `Add domain` row. `Members` shows the creator, marked `You`, on the `Members` tab, `No invitations sent` on `Invitations`, and `No membership requests` on `Requests`.
- `invitation-sent` sends an invitation to a new `+clerk_test` address from `Invite` on the Members screen, and the address shows on the `Invitations` tab.
- `rename` changes the organization's name on `Update profile`, and the profile shows the new name.

## How to get to it (user POV)

- With an organization active, tap the OrganizationSwitcher, then `Manage organization`. The profile opens full screen with the title `Organization`.
- From the profile, tap `Update profile`, `Members`, or `Verified domains`. On the Members screen, the three tabs are at the top and `Invite` is at the top right.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user on the standard instance, signs in with a ticket, and creates an organization from the switcher with `signInWithNewOrganization` from `specs/compose.ts`. The user who creates an organization holds the `org:admin` role (`creator_role` in `src/core/instances/base.json`), which has every permission. The standard settings turn on domains, allow three members, and let an admin delete the organization.

- **Profile.** Run `e2e-tests/bin/control-clerk-android run organization-profile`. The first test opens `Manage organization`, expects the organization's name and the five rows, and takes screenshot `org-profile`. It opens `Verified domains`, expects `Add domain` and no `No verified domains.`, takes screenshot `verified-domains`, and goes back. It opens `Members`, expects `You`, takes screenshot `members`, then opens `Invitations` and expects `No invitations sent`, and opens `Requests` and expects `No membership requests`.
- **Invitation.** The second test opens `Members`, taps `Invite`, fills `Enter email addresses` with `host.newEmail()`, taps `Send invitations`, opens `Invitations`, and expects the address. Screenshot `invitation-sent`.
- **Rename.** The third test taps `Update profile`, replaces the text in `clerk.organization.profileForm.name` with `Renamed <runId>`, and taps `clerk.organization.profileForm.submit`. It expects the form gone and the new name on the profile. Screenshot `org-renamed`.
- **Proof.** `profile.e2e.ts` passes, and the screenshots show each screen with the organization's name or the creator's email. `down` deletes the application, and every organization and invitation goes with it.

## States a spec cannot reach

The harness seeds users only. It cannot seed an organization, a membership, a role, or a custom role, and a spec signs in one user per launch. The creator of an organization is its only member and an admin, so these states are out of reach:

| State | Where the app decides | What it takes |
| --- | --- | --- |
| `No verified domains.` on Verified domains | `OrganizationVerifiedDomainsView.kt`: domains empty and the viewer cannot manage them | A member who has `org:sys_domains:read` without `org:sys_domains:manage`, such as a custom role. An admin sees `Add domain` instead. |
| `No members sections are available.` on Members | `OrganizationMembersView.kt`: the viewer has no tab | A member who can neither read nor manage memberships. The same check hides the `Members` row, so the profile never opens this screen. |
| `No members found.` on the `Members` tab | `OrganizationMembersView.kt`: the list is empty and the search field is empty | Nothing a role gives: the viewer is always in the list. It shows only after the members request fails. A search that matches no one shows `No members found` without the period. |
| A non-empty `Requests` tab | `OrganizationMembersState.kt` | A verified domain with automatic suggestions and another user who requests to join. |
| The `Members` row or `Verified domains` row hidden, `Delete organization` or `Update profile` hidden | `root/OrganizationProfileRows.kt`, `root/OrganizationProfileRootView.kt` | A member without the read or manage permission for that row, without `org:sys_profile:delete`, or without `org:sys_profile:manage`. |
| Changing another member's role, removing another member | `OrganizationMembersView.kt` | A second member in the organization. The creator's own row has the menu, but only for the creator. |

Today a change to one of these states is shown on a build with a local-only change that is not part of the pull request, and the pull request says so. For example, `canManageDomains = false` in `OrganizationVerifiedDomainsViewModel` shows `No verified domains.` to the creator. Name the change and the screens it affects in the pull request, and keep the spec in `specs/explored/`.

## Gotchas

- No row, tab, or button of the profile has a test tag except the name, slug, and submit of `Update profile`, which share the `clerk.organization.profileForm.*` tags with the create form. The spec finds rows and tabs by text with `{ exact: true }` and taps them with `tapMiddle`. `Members` is both a row and a tab label, and the Members screen's title.
- `Verified domains` shows only when the instance has domains on and the member can read or manage them. On the standard instance the creator always sees it.
- The `Requests` tab shows only when the instance has domains on and the member can manage memberships (`OrganizationMembersState.kt`). On the standard instance the creator always sees it.
- Back from `Verified domains` returns to the profile. The spec presses the system back button with `device.back()`.
- The Members row of the creator shows the email twice, as the name and the identifier, because a seeded user has no name. The email can be cut off on screen, but the screen tree holds all of it, so `getByText(email)` finds it.
- The slug of an organization comes from its name, and Clerk refuses a slug that another organization in the instance holds with "That slug is taken. Please try another." The message shows in a snackbar that closes itself, and the create form stays. Every test in a run shares the run id, and so does every attempt of a test that `--retries` runs again. Name each organization with `newOrganizationName(host, prefix)` from `specs/compose.ts`, which adds a random suffix to `<prefix> <runId>`, so a retry does not create the slug its first attempt took.
