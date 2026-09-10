# Android session-task assertion audit

All nine declarations and helper/assertion bodies in baseline `source/api/src/test/java/com/clerk/api/session/SessionTaskTest.kt` were reviewed at `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. The old test constructs native Session DTOs and checks handwritten task helpers. That file is retired; generated Session resources and the TypeScript task order are now authoritative.

## Packaged core and native presentation evidence

Two new `AuthPresentationCoreTest` declarations run the packaged QuickJS core through fixture HTTP and call production Compose presentation/routing state:

- `taskKeysPreserveCanonicalSpellingAndPreventPrematureCompletion` checks all 18 combinations of active/pending status and nine keys: the three canonical keys, five legacy aliases, and an unknown future key. It verifies exact generated enum/raw-key projection, the tasks list/current task, selected-session wrapper identity across reload, native routing, and zero completion callbacks while any task remains. Unknown keys route to help and cannot complete the presentation.
- `orderedTasksDriveRoutingUntilTheLastRequirementClears` checks both reset-password/setup-mfa orders, an unknown first task followed by setup-mfa, and choose-organization. It checks currentTask comes from the first array item even when a conflicting extra `current_task` field is present in the fixture HTTP response. A pending session without tasks still cannot complete. Only the subsequent active, task-free state allows one callback; a repeated completion notification does not deliver it twice.

All nine focused presentation tests pass on Android 16, including the seven prior completion/refresh tests. These are native state and packaged-resource checks; they do not render the full AuthView or complete a live MFA/password/organization task.

## Every retired declaration

| Legacy declaration | Current disposition and evidence |
| --- | --- |
| `parsedKey maps mfa required keys` | The canonical contract recognizes `setup-mfa`. `mfa_required`, `mfa-required`, and `setup_mfa` remain unrecognized output values with their raw spelling; the new matrix checks each and routes to help. The old alias parser is intentionally removed. |
| `parsedKey maps reset password keys` | `reset-password` is generated as ResetPassword; `reset_password` remains Unrecognized. Both are checked in both statuses. |
| `parsedKey maps choose organization keys` | `choose-organization` is generated as ChooseOrganization; `choose_organization` remains Unrecognized. Both are checked in both statuses. |
| `requiresForcedMfa is true for pending session with mfa task` | The old Boolean helper and `mfa_required` alias are removed. A pending canonical setup-mfa task routes to the MFA destination and blocks completion; the alias safely routes to help. |
| `requiresForcedMfa is true for active session with mfa task` | Active status does not bypass a current task. The active setup-mfa and legacy-alias cases both block completion and route according to the canonical key. |
| `pendingTaskKey returns choose organization for active session with choose organization task` | The active choose-organization case reads the core's currentTask and routes to organization selection without a completion callback. |
| `requiresForcedMfa is false when pending session has no mfa task` | Pending choose-organization routes to organization selection; no native MFA Boolean is retained. The separate pending-with-no-tasks step also remains incomplete. |
| `pendingTaskKey preserves current pending task order` | The order test checks reset-password first, then setup-mfa first, and an unknown first task. The generated currentTask and UI destination follow the first task without scanning ahead for a known key. |
| `pendingTaskKey prefers current task when present` | Intentional source change: TypeScript Session.currentTask returns the first tasks entry. There is no independently writable native currentTask override. Conflicting fixture `current_task` data does not override the canonical getter. |

The canonical source is `packages/shared/src/types/session.ts` (SessionTask.key) and `packages/clerk-js/src/core/resources/Session.ts` (tasks hydration/currentTask getter). The fixture's extra current_task property is not a claim that it belongs to the canonical wire schema. No domain implementation, generator output, or packaged core changed in this audit.
