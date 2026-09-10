# Compose authentication completion audit

The nine declarations and complete helper/assertion bodies in the old `sdk/ClerkAuthFlowTest.kt` were reviewed alongside the two old `ClerkClientFlowTest.kt` declarations. Both files remain retained: this review found concrete presentation defects, and the old distinction between a plain active-client refresh and a completed authentication still needs a separate disposition.

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

The remaining old assertions about signed-out readiness, active-user readiness, held post-authentication, closing registrations, and restored pending sessions have corresponding checks here. The old global reset API is removed in favor of owner disposal. The old `Client.get` mocks and mutable clientFlow setter are not current implementation seams; generated state/projection tests cover current identity delivery. This audit does not claim one-for-one compatibility with those removed APIs or retire either old file yet.

These are native presentation-state and packaged-resource tests. They do not establish a live provider login, real biometric enrollment prompt, full rendered AuthView journey, or a signed-in released-app upgrade.
