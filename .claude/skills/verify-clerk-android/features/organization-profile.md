# Organization profile

A member of the active organization opens its profile from OrganizationSwitcher's `Manage organization`, renames the organization, sees its members, invites people, and reaches its verified domains. Which rows and tabs show depends on the member's permissions in the organization.

## Sub-features

- `profile` shows the creator every row of the profile: `Update profile` under the organization's name, `Members`, `Verified domains`, `Leave organization`, and `Delete organization`. `Verified domains` shows the `Add domain` row. `Members` shows the creator, marked `You`, on the `Members` tab, `No invitations sent` on `Invitations`, and `No membership requests` on `Requests`.
- `invitation-sent` sends an invitation to a new `+clerk_test` address from `Invite` on the Members screen, and the address shows on the `Invitations` tab.
- `rename` changes the organization's name on `Update profile`, and the profile shows the new name.
- `org-domains-empty` shows `No verified domains.` and no `Add domain` to a member whose role can read domains but not manage them.
- `org-requests` shows a request to join on the `Requests` tab, with `Accept` and `Reject`, from a user who signed up with an address at the organization's verified domain.

## How to get to it (user POV)

- With an organization active, tap the OrganizationSwitcher, then `Manage organization`. The profile opens full screen with the title `Organization`.
- From the profile, tap `Update profile`, `Members`, or `Verified domains`. On the Members screen, the three tabs are at the top and `Invite` is at the top right.

## Driving it with verify

Preconditions:

- The first three tests seed a `+clerk_test` user on the standard instance, sign in with a ticket, and create an organization from the switcher with `signInWithNewOrganization` from `specs/compose.ts`. The last two seed the organization with `host.seedOrganization`, and the seeded organization is active when a member signs in with a ticket. The user who creates an organization holds the `org:admin` role (`creator_role` in `src/core/instances/base.json`), which has every permission. The standard settings turn on domains, allow three members, and let an admin delete the organization.

- **Profile.** Run `e2e-tests/bin/control-clerk-android run organization-profile`. The first test opens `Manage organization`, expects the organization's name and the five rows, and takes screenshot `org-profile`. It opens `Verified domains`, expects `Add domain` and no `No verified domains.`, takes screenshot `verified-domains`, and goes back. It opens `Members`, expects `You`, takes screenshot `members`, then opens `Invitations` and expects `No invitations sent`, and opens `Requests` and expects `No membership requests`.
- **Invitation.** The second test opens `Members`, taps `Invite`, fills `Enter email addresses` with `host.newEmail()`, taps `Send invitations`, opens `Invitations`, and expects the address. Screenshot `invitation-sent`.
- **Rename.** The third test taps `Update profile`, replaces the text in `clerk.organization.profileForm.name` with `Renamed <runId>`, and taps `clerk.organization.profileForm.submit`. It expects the form gone and the new name on the profile. Screenshot `org-renamed`.
- **Domains reader.** The fourth test seeds an admin and a reader, and an organization with the reader as `org:e2e_domains_reader`. It signs the reader in, expects the organization's name on the switcher, taps it, taps `Manage organization`, opens `Verified domains`, and expects `No verified domains.` and no `Add domain`. Screenshot `org-domains-empty`.
- **Request to join.** The fifth test seeds an admin and an organization with `suggestDomain: true`. It signs up a new user through the full-screen AuthView with `host.newEmail({ domain: org.domain })`, the run password, and the test code. That user taps the switcher, taps `Request to join` on the suggestion, and expects `Pending approval`. Then it signs the admin in, opens `Manage organization`, `Members`, and `Requests`, and expects the requester's email, `Accept`, `Reject`, and no `No membership requests`. Screenshot `org-requests`.
- **Proof.** `profile.e2e.ts` passes, and the screenshots show each screen with the organization's name or the creator's email. `down` deletes the application, and every organization and invitation goes with it.

## States a spec cannot reach

`host.seedOrganization` adds other members and the `org:e2e_domains_reader` role, so the tests above reach `No verified domains.` and a non-empty `Requests` tab. These states are still out of reach:

| State | Where the app decides | Why |
| --- | --- | --- |
| `No members sections are available.` on Members | `OrganizationMembersView.kt`: the viewer has no tab | Unreachable by design. The viewer has no tab when they can neither read nor manage memberships, and the same check hides the `Members` row in `root/OrganizationProfileRows.kt`, so the profile never opens this screen in that state. Only a role revoked while the screen is open shows it, for a moment. |
| `No members found.` on the `Members` tab | `OrganizationMembersView.kt`: the list is empty and the search field is empty | Nothing a role gives: the viewer is always in the list. It shows only after the members request fails. A search that matches no one shows `No members found` without the period. |

Seeding also reaches these states, and no spec drives them yet: the `Members` row with no `Requests` tab, and no `Update profile` or `Delete organization`, for the `org:e2e_domains_reader` member; another member's row, with changing their role and removing them, for an admin with a second member.

A change to a state no spec reaches is shown on a build with a local change that is not part of the pull request, and the pull request says so. Name the change and the screens it affects, and keep the spec in `specs/explored/`.

## Gotchas

- No row, tab, or button of the profile has a test tag except the name, slug, and submit of `Update profile`, which share the `clerk.organization.profileForm.*` tags with the create form. The spec finds rows and tabs by text with `{ exact: true }` and taps them with `host.tap(middleOf(...))`. `Members` is both a row and a tab label, and the Members screen's title.
- `Verified domains` shows only when the instance has domains on and the member can read or manage them. On the standard instance the creator always sees it.
- The `Requests` tab shows only when the instance has domains on and the member can manage memberships (`OrganizationMembersState.kt`). On the standard instance the creator always sees it.
- Back from `Verified domains` returns to the profile. The spec taps the top bar's back arrow with `host.back()`.
- The Members row of the creator shows the email twice, as the name and the identifier, because a seeded user has no name. The email can be cut off on screen, but the screen tree holds all of it, so `getByText(email)` finds it. A row on the `Requests` tab shows the requester's email twice too, so the spec takes the first match with `getByText(email).first()`.
- The slug of an organization comes from its name, and Clerk refuses a slug that another organization in the instance holds with "That slug is taken. Please try another." The message shows in a snackbar that closes itself, and the create form stays. Every test in a run shares the run id, and so does every attempt of a test that `--retries` runs again. Name each organization with `newOrganizationName(host, prefix)` from `specs/compose.ts`, which adds a random suffix to `<prefix> <runId>`, so a retry does not create the slug its first attempt took.
