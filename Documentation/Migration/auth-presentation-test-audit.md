# Compose authentication completion audit

[The rendered authentication journey](rendered-auth-journey.md) adds actual `AuthView` input, invalid-code retry, generated finalization and completion-gate evidence with screenshots. It complements the state-level assertions below.

The nine declarations and complete helper/assertion bodies in the old `sdk/ClerkAuthFlowTest.kt` were reviewed alongside the two old `ClerkClientFlowTest.kt` declarations. Both files are now retired after the assertion mapping below. Their private singleton setters, mocked Client fetches, and AuthEvent completion payloads are removed implementation seams; retained presentation behavior is checked through generated resources.

## Reproduced completion defects

The new `AuthPresentationCoreTest` exercises the current presentation owner and `AuthState` using generated resources. Against the previous UI implementation:

- Registering a non-dismissible presentation for an already active user changed the completion gate from true to false.
- Calling completion while a session was still pending saved its session ID. When the task later cleared, the gate incorrectly opened without a valid completion afterward.
- A generated active-session snapshot with a null Clerk user opened the gate. This case uses a modified source-generated preview projection at the native state boundary, not a live or packaged-core server response. The first attempted HTTP fixture normalized its null user into a User resource, so that attempt was not evidence of the missing-user condition.

The final pre-fix run discovered five tests and three assertion failures. The final six-test suite passes on the Android 16 emulator, including the additional registration-reuse case. The complete current UI instrumentation target also passes all ten tests, including existing Compose view-model lifetime and organization-list checks. The tests distinguish signed-in state, active task-free state, and completion of presentation work.

## Current behavior

`AuthPresentationState` requires an active session, no pending task, and a user. Registration for an already ready user acquires no hold. A completion recorded before those conditions hold is ignored. Starting a new registration after the old registrations have closed clears the prior completion marker, preventing a later pending flow for the same session from reusing it. Closing an older registration is idempotent and cannot remove a remaining registration's hold.

`AuthState.completePresentation` also requires a user before invoking the application's callback. Authentication and session-task state continue to come from TypeScript. These checks decide whether native presentation can finish; they do not select sessions or implement verification rules.

The packaged-core prebuilt-flow test starts with a complete sign-in and no selected session. It calls the production `AuthState.setToStepForStatus`, verifies that it invokes generated finalization, and checks both an active result and a pending choose-organization task. Pending results route to the task without calling completion. After the task clears, presentation can complete. Repeating the status callback neither finalizes again nor delivers the callback twice.

## Refresh and explicit adoption

The seventh packaged-core presentation test, `foregroundRefreshPublishesSessionsWithoutImplicitlyCompletingAuthentication`, verifies that an OS foreground refresh discovers the available session and publishes the same generated resource through `Clerk.changes`. The selected session and user remain null. Calling generated `signIn.finalize()` adopts that same resource before returning, while the registered presentation remains held until `complete()`.

This differs intentionally from the old private `Clerk.updateClient` setter, which implicitly selected a newly available session. The current TypeScript `__internal_reloadInitialResources` refreshes the client/environment through `updateClient`; availability alone does not adopt a session when there was no selection. Startup restoration remains separate, as verified by `ClerkProjectionTest`. Reintroducing the old Kotlin selection behavior would duplicate domain policy and undermine explicit finalization.

The initial exploratory fixtures did not prove a presentation regression: attaching a client envelope during initial environment loading failed bootstrap, later environment reload did not select a session, and waiting for foreground refresh to select one timed out. The final test waits for session availability and then checks the canonical selection and presentation contracts directly. All seven focused presentation tests pass on the Android 16 emulator. No production code or packaged bundle changed in this follow-up.

## Every retired declaration

| Legacy declaration | Disposition and current evidence |
| --- | --- |
| `isAuthFlowComplete is false when signed out` | The foreground-refresh and prebuilt-completion cases start with null selection and a false presentation gate. The old separate singleton StateFlow is removed; the UI gate uses Compose state. |
| `isAuthFlowComplete requires an active session with a user` | The pending-task test checks pending then active readiness; the generated snapshot boundary test checks an active session with no root user. The latter is explicitly a projection fixture, not an HTTP reproduction. |
| `completed authentication holds registered auth flow until post-auth completes` | The new foreground-refresh test awaits explicit finalization and checks the gate stays false until presentation completion. The prebuilt-flow test checks production AuthState invokes finalization and delivers completion once. The old pending AuthEvent payload property is removed. |
| `active client refresh does not hold registered auth flow` | Intentional domain change: a refresh from signed-out state exposes available sessions without selecting one. The new test verifies this and subsequent explicit adoption. Existing active-user registration remains unheld. |
| `registerAuthFlow ignores an existing active user session` | `registrationDoesNotHoldAnExistingActiveUser` checks readiness before/after registration and after a pending notification. Registration now returns a no-op closer instead of null. |
| `closing older registration does not clear newer auth flow` | `releasingAnOlderRegistrationDoesNotReleaseTheRemainingHold` closes the older registration twice, then activates a restored pending session and checks the remaining hold. Multiple current registrations use a count, not the old latest-registration token. |
| `restored pending session keeps auth flow held after activation` | `pendingSessionCannotBeMarkedCompleteBeforeItsTaskFinishes` checks pending, ignored premature completion, active refresh still held, then valid completion. The registration-reuse test prevents reuse of a prior completion marker. |
| `closing auth flow registration clears pending hold` | The older-registration test closes the last registration after activation and checks the gate opens. The old pending AuthEvent property has no generated equivalent. |
| `reset clears registered auth flow before a subsequent completed sign in` | The mutable global Clerk reset API is removed. Owner disposal and a new ClerkProvider establish independent runtime/presentation ownership; `ConfigurationOwnershipTest` checks one owner's closure leaves another usable, and `ClerkModelLifetimeTest` checks Compose owner replacement/removal. This is an ownership change, not a promise to reset a reusable global instance. |
| `updateClient updates clientFlow with latest client` | The private setter and public legacy Client DTO/flow are removed. The new refresh test checks current sessions and the generated `Clerk.changes` snapshot share resource identity. `ClerkProjectionTest` separately checks root session/user delivery. |
| `refreshClient fetches and updates clientFlow` | The old mocked `Client.get`, native result wrapper, and `refreshClient` method are removed. The packaged test exercises real core HTTP on foreground refresh and observes generated state; no mocked native fetch is retained. |

The setup/teardown setters and the helper constructors for old Client, Session, and SignInCompleted values belong to the same removed model. The tests do not claim compatibility with these private seams or the old AuthEvent ordering.

These are native presentation-state and packaged-resource tests. They do not establish a live provider login, real biometric enrollment prompt, full rendered AuthView journey, or a signed-in released-app upgrade.


## Sign-up completion follow-up

The prebuilt-completion matrix now exercises both sign-in and sign-up with active and pending outcomes. Its first exploratory sign-up fixture omitted the available created session and therefore did not adopt one; the UI routed to help and correctly withheld completion. The final test asserts that path, then supplies the session through actual core foreground refresh and verifies it remains unselected until generated finalization. Once adopted, pending organization tasks still block presentation completion. Repeated status processing does not touch the session again or deliver another completion callback. All nine focused presentation tests pass, including the task-key/order follow-up. This is native state/fixture-HTTP evidence, not a rendered or live-server sign-up journey.
