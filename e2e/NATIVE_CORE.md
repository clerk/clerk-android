# End-to-end app on the generated core

The application retains one core and supplies it to both custom flows and prebuilt
UI. The custom test-user flow still prepares phone verification, creates a test
identity only for `form_identifier_not_found`, and uses the test code for its
`+clerk_test` email when needed. It calls generated `finalize()` only on completed
attempts, then checks the actual active session and remaining tasks. Outstanding
requirements open prebuilt UI against the same owner. Invalid codes retain the
verification form for retry.

The app's callback scheme is `com.clerk.e2e.clerk://oauth/callback`; its activity
provides the native presentation context. The full Android `assembleDebug` build
includes this app. Building it does not run the account-creating or message-sending
test flows; those require a configured development instance and explicit test run.
