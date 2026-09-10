# Clerk singleton assertion audit

All 41 declarations and their complete assertion/setup bodies in the 900-line legacy `sdk/ClerkTest.kt` were reviewed at baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. Its setup directly substitutes mocked client/environment objects and mutates singleton state; it does not establish behavior through HTTP or native bindings.

## Packaged replacement evidence

`NativeCoreTests/Android/ClerkProjectionTest.kt` passes three tests through the actual packaged QuickJS core on the Android 16 emulator:

- Six startup cases cover no sessions, missing selection among multiple active sessions, unmatched selection, a selected second session, a selected pending session, and an expired-only list. Each checks generated session/user state, the complete session list, and valid future-style authentication roots. Pending state preserves its required task.
- Two refresh cases retain selected session/user wrapper identity when the client envelope omits or mismatches its last-active ID. Generated sign-out exposes a null session/user pair before returning and through the generated changes Flow, and invalidates the prior session handle.
- Environment reload preserves its resource identity while changing application name, logo, support email, single-session mode, social providers, and enabled/immutable identifier attributes. Both configured/empty strings and true/false flags are checked.

These are three test declarations with explicit fixture matrices, not a coverage percentage. Fixture HTTP is not live-server or rendered-UI evidence. Core implementation, package bytes, and generated bindings did not change in this audit.

## Every legacy declaration

| Legacy declaration | Current disposition and evidence |
| --- | --- |
| `session returns null when client is not initialized` | No partially initialized global client exists. `connect` returns a ready owner; the empty-client startup case exposes null session/user. |
| `session returns null when client has multiple sessions and no last active session ID` | Intentional canonical change: at startup the core selects the first signed-in session when no last-active ID is supplied. The two-active-session case verifies this; no Kotlin selection algorithm is retained. |
| `session returns null when no sessions match active session ID` | Intentional canonical change: unmatched startup selection falls back to the first signed-in session. The packaged test checks the exact missing-ID case. |
| `session returns matching session when client has session` | The selected active session is exposed through `clerk.session`; the multi-session selected-ID case checks the actual generated resource ID. |
| `sessionsFlow exposes all sessions from current client` | `clerk.sessions` and `ClerkState.sessions` expose generated resource wrappers. The six-case startup matrix checks the complete ordered list, including an expired session that is not selected. The old separate singleton StateFlow is removed. |
| `updateClient preserves previous active session when refreshed client omits active id` | `Session.reload` with a client envelope lacking last-active ID retains the same selected session and user wrappers. Checked by the packaged refresh test. |
| `updateClient preserves previous active session when refreshed client references missing active id` | `Session.reload` with an unmatched last-active ID also retains the same selected session and user wrappers. Checked separately in the packaged refresh test. |
| `updateClient activates sole session when refreshed client omits active id` | The private `updateClient` activation rule is removed. Startup may restore a session, but a later available session does not imply adoption. `LifecycleContractTest` verifies refresh leaves selection null; `PackagedCoreTest.completedSignInSurvivesClientRefreshUntilExplicitFinalization` verifies explicit finalization of a completed attempt. |
| `multiSessionModeIsEnabled mirrors inverse of single session environment flag` | Use `!clerk.environment.authConfig.singleSessionMode`. Both Boolean values are checked through generated environment reload; the old convenience property/flow is removed. |
| `user returns null when no active session exists` | The empty-client and expired-session startup cases expose null `clerk.user`. |
| `user returns user when session exists` | Active startup cases expose the session user through the generated root. |
| `isSignedIn returns false when no session exists` | The old `isSignedIn` convenience is removed; the empty-client case checks null session/user. Applications must distinguish a pending session from an active one. |
| `isSignedIn returns true when session exists` | The old Boolean is not generated; active startup checks the session and user themselves. |
| `currentSignIn returns null when client is not initialized` | The nullable `auth.currentSignIn` API is removed. After successful connection an initially empty future-style `clerk.signIn` always exists; its handle is checked in every startup case. |
| `currentSignIn returns sign in when client is initialized` | `clerk.signIn` is the source-selected future facade, not the mocked legacy SignIn DTO. Startup handle validity and existing generated SSO/reset tests establish its current lifetime. |
| `currentSignUp returns null when client is not initialized` | The nullable `auth.currentSignUp` API is removed. After connection the empty future-style `clerk.signUp` always exists and has a valid handle. |
| `currentSignUp returns sign up when client is initialized` | `clerk.signUp` is the source-selected future facade. Startup handle validity and existing generated signup/reset coverage replace the old DTO identity check. |
| `logoUrl returns null when environment is not initialized` | The assertion body expected an empty string despite the test name saying null. No uninitialized environment accessor remains; the new environment test verifies an explicitly empty server logo field. |
| `logoUrl returns logo URL when environment is initialized` | `clerk.environment.displayConfig.logoImageUrl` retains the configured URL and updates on reload. |
| `applicationName returns null when environment is not initialized` | The assertion body expected an empty string. There is no provisional environment singleton; an explicitly empty server application-name field is preserved. |
| `applicationName returns name when environment is initialized` | `clerk.environment.displayConfig.applicationName` retains the configured name and updates on reload. |
| `supportEmail returns configured address when environment is initialized` | `clerk.environment.displayConfig.supportEmail` preserves the configured address through QuickJS and generated Kotlin. |
| `supportEmail ignores blank configured address` | Intentional representation change: the canonical DisplayConfig string preserves an empty address instead of converting it to null. The packaged test checks the empty string; SignInGetHelpView uses `isNotBlank` before selecting its fallback support address. |
| `socialProviders returns empty map when environment is not initialized` | No uninitialized environment is exposed. An explicitly empty social-provider map is verified on environment reload. |
| `socialProviders returns providers when environment is initialized` | `clerk.environment.userSettings.social` preserves provider entries; the packaged test checks Google presence and removal, while the shared custom-provider protocol case checks strategy/name/enabled values. |
| `identifier flags return false when environment is not initialized` | No pre-initialization default flags exist. The current test explicitly reloads disabled attributes; callers read the typed environment attributes instead of global convenience Booleans. |
| `identifier flags return environment values when initialized` | Email, phone, and username `enabled`/`immutable` values are verified for both Boolean states through `environment.userSettings.attributes`. The old convenience names are removed. |
| `debugMode is false by default` | The mutable global `debugMode` compatibility property is not part of this generated major. No fake replacement flag or default is introduced. |
| `clearSessionAndUserState nulls both sessionFlow and userFlow` | Private direct state clearing is removed. Generated `signOut` is tested to expose null session and user before completion, invalidate the old session handle, and deliver a coherent null pair through `clerk.changes`. |
| `updateClient emits SessionChanged and SignedIn when session appears` | The ordered AuthEvent.SessionChanged/SignedIn event pair is removed. Generated snapshots are the observation contract and can coalesce intermediate revisions. Existing packaged finalization checks session/user visibility before return; this audit does not claim the old event ordering. |
| `clearSessionAndUserState emits SessionChanged and SignedOut when session is cleared` | The ordered AuthEvent.SessionChanged/SignedOut pair is removed. The new packaged test checks the coherent post-sign-out snapshot and Flow delivery, not two ordered legacy events. |
| `session property returns sessionFlow value` | Properties read the same generated `ClerkState`; the old independent `sessionFlow` is removed. The refresh test checks wrapper identity and post-sign-out null projection/observation. |
| `user property returns userFlow value` | The old independent `userFlow` is removed. The generated snapshot exposes the same selected user, checked at startup and across reload, then null after sign-out. |
| `updateSessionAndUserState updates both flows correctly` | The private updater is removed. Session/user projection is owned by TypeScript and one native state revision; startup, reload, and sign-out checks cover the retained state relationship. |
| `session returns matching session even when status is pending` | The selected pending session remains visible with `status: pending` and its `choose-organization` task. Checked by the packaged startup matrix. |
| `user returns user even when session is pending` | The pending session still exposes its user; verified through the generated user root. |
| `isSignedIn returns true for pending session` | The old isSignedIn Boolean is removed. Pending session presence and task state are verified explicitly; presence must not be treated as authorization to skip session tasks. |
| `activeSession returns null when session is pending` | The activeSession convenience is removed. A pending session is visible as `clerk.session` with pending status and its required task; consumers needing an active session must inspect that status. |
| `activeSession returns session when status is active` | The activeSession convenience is removed. The selected active session is exposed through `clerk.session` and its status. |
| `activeUser returns null when session is pending` | The activeUser convenience is removed. The generated root retains the pending session user; active-only presentation must use the session status rather than assuming null user during tasks. |
| `activeUser returns user when session is active` | The activeUser convenience is removed. Active startup verifies the generated user ID associated with the selected session. |

The old file is retired after this assertion review and packaged verification. Its immutable baseline path/hash/declarations remain in `legacy-tests.json`. Separate auth-flow, client-response ordering, integration, and UI suites retain their own scope; this audit does not retire them or restore a parallel native session-selection implementation.
