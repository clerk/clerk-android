# Organization request-routing assertion audit

`OrganizationApiTest.kt` contains three declarations. The source matches baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f` byte for byte. Its Retrofit interface was removed with the native domain implementation. The [proof](evidence/organization-routing/proof.json) retains the file hash, all declaration names and all 17 mutating endpoint mappings.

The shared `organization-resources.test.mjs` suite runs the generated dispatcher and actual TypeScript resources with fixture HTTP. All 47 cases pass. They assert requests emitted by the current owner rather than annotations on the removed interface. This audit adds a selected-session assertion to the three logo-removal outcomes; no production code or packaged bytes change.

## Every legacy declaration

| Old declaration | Current evidence |
| --- | --- |
| `revokeOrganizationInvitation uses invitation revoke route` | The `OrganizationInvitation.revoke` case requires POST to `/v1/organizations/org_contract/invitations/orginv_contract/revoke`, one request, and the returned revoked invitation. |
| `getMembershipRequests uses membership requests route` | Both `Organization.getMembershipRequests` listing cases require GET to `/v1/organizations/org_contract/membership_requests`, with pagination and the optional status filter. |
| `organization mutating endpoints include clerk session id query` | All 17 mappings below assert `_clerk_session_id=sess_native` on emitted requests. Mutation cases check the exact route and effective verb; logo upload checks binary and string data, and removal checks receipt, complete-resource and rejection outcomes. |

## All mutating endpoint mappings

| Removed native method | Generated resource method |
| --- | --- |
| `updateOrganization` | `Organization.update` |
| `deleteOrganization` | `Organization.destroy` |
| `updateOrganizationLogo` | `Organization.setLogo(file)` |
| `deleteOrganizationLogo` | `Organization.setLogo(null)` |
| `createOrganizationDomain` | `Organization.createDomain` |
| `deleteOrganizationDomain` | `OrganizationDomain.delete` |
| `updateEnrollmentMode` | `OrganizationDomain.updateEnrollmentMode` |
| `prepareAffiliationVerification` | `OrganizationDomain.prepareAffiliationVerification` |
| `attemptAffiliationVerification` | `OrganizationDomain.attemptAffiliationVerification` |
| `createMembership` | `Organization.addMember` |
| `updateMembership` | `Organization.updateMember` |
| `removeMember` | `Organization.removeMember` |
| `createInvitation` | `Organization.inviteMember` |
| `bulkCreateInvitations` | `Organization.inviteMembers` |
| `revokeOrganizationInvitation` | `OrganizationInvitation.revoke` |
| `acceptMembershipRequest` | `OrganizationMembershipRequest.accept` |
| `rejectMembershipRequest` | `OrganizationMembershipRequest.reject` |

The old annotation suite is retired after this mapping. The public behavior remains covered through shared execution. Existing packaged organization tests separately exercise logo session-query transport, resource identity and pagination, as recorded in the [organization resource results](organization-resource-results.md); this audit does not claim a new native run of every route. The timestamp difference in [user invitations](organization-public-data.md) remains unresolved, and that separate legacy file remains retained.
