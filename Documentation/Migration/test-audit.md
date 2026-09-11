# Legacy test audit

The [deletion result contract](deletion-result-contract.md) records the reproduced membership return-value regression and packaged QuickJS checks. It does not retire unaudited legacy tests.

The [user resource results](user-resource-results.md) record shared profile and image-receipt regressions and five generated QuickJS scenarios; Android legacy files remain retained.

The [biometric persistence audit](biometric-persistence.md) records eight packaged QuickJS scenarios and the shared metadata/rollback fixes. Android legacy tests remain retained for their own assertion audit.

Baseline: `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. This is a file-level ownership and migration audit of the old native API suite, with exact test declarations retained in [legacy-tests.json](legacy-tests.json). Parameterized cases are not expanded; declarations and helper functions are not a coverage percentage.

The inventory contains 105 old test/helper files. Three have been migrated, 59 have been retired after assertion review, and 43 remain while platform and coverage migration is incomplete.

| Reviewed group | File disposition | Evidence |
| --- | --- | --- |
| Live integration scenarios and helper | Three migrated to packaged QuickJS instrumentation | [Integration audit](integration-test-audit.md) |
| Private foreground-policy suites | Two retired | [Foreground assertions and reproduced race](configuration-test-audit.md#foreground-policy-and-pending-authentication) |
| Configuration switch and device-token suites | Two retired | [Owner-isolation audit](configuration-ownership-test-audit.md) |
| Clerk singleton suite | One retired | [All 41 declarations and packaged projection checks](clerk-projection-test-audit.md) |
| Auth and client flow suites | Two retired | [All 11 declarations and canonical refresh/adoption checks](auth-presentation-test-audit.md) |
| Session tasks and verification | Two retired | [Nine task declarations](session-task-test-audit.md) and [15 verification declarations](session-verification-test-audit.md#android-assertion-retirement) |
| Storage helper and installation ID | Two retired | [All 14 declarations and Keystore integrity checks](storage-test-audit.md#integrity-and-retirement-follow-up) |
| Outgoing HTTP middleware | Two retired | [All six declarations and native transport checks](http-test-audit.md#outgoing-middleware-retirement) |
| Incoming response middleware | Two retired | [All 13 declarations and packaged response/completion checks](incoming-response-test-audit.md) |
| Private token freshness/cache policy | One retired | [All ten declarations and packaged response ordering](token-freshness-test-audit.md) |
| Token fetching | One retired | [All 25 declarations and the late host-reply fix](token-request-test-audit.md) |
| Authentication entry points | Four retired | [All 23 declarations and 21 new packaged contract cases](auth-entry-test-audit.md) |
| Google service and sign-up parameter mapping | Two retired | [All twelve declarations and native credential/transfer checks](google-service-test-audit.md) |
| Credential sign-in and sign-up presentation | Three retired | [All eleven declarations and generated request/routing checks](signin-credential-test-audit.md) |
| Initial factors and sign-out service | Two retired | [All fifteen declarations and shared sign-out behavior](factor-signout-test-audit.md) |
| Browser redirect wrappers and result converters | Four retired | [All thirteen declarations and generated browser SSO behavior](browser-sso-test-audit.md) |
| OAuth provider identity, choices and parameters | Three retired | [All eleven declarations and generated provider/request checks](oauth-provider-test-audit.md) |
| Browser and external-account services | Three retired | [All seventeen declarations and native host/core callback checks](browser-host-test-audit.md) |
| Publishable-key and callback configuration helpers | Two retired | [All eleven declarations and explicit configuration/route behavior](configuration-test-audit.md) |
| Callback receiver and manifest suites | Two retired | [All five declarations and reproduced task cancellation](callback-receiver-test-audit.md) |
| Browser manager activity | One retired | [All thirteen declarations and current presentation/core ownership](browser-manager-test-audit.md) |
| Connectivity monitor | One retired | [All ten declarations and API 24/25 availability regression](lifecycle-test-audit.md) |
| Passkey manager and helper | Two retired | [All ten declarations and native Credential Manager callback checks](passkey-host-test-audit.md) |
| Passkey registration and service facade | Two retired | [All nineteen declarations and core failure boundaries](passkey-service-test-audit.md) |
| Passkey authentication service | One retired | [All 21 declarations, reproduced UI suppression and generated recovery](passkey-authentication-test-audit.md) |
| Environment configuration and settings | Five retired | [All fifteen declarations, generated projection checks and disabled-passkey regression](environment-settings-test-audit.md) |
| Development-mode warning | One retired | [All three declarations and rendered production/reload regression](development-warning-test-audit.md) |
| Passkey assertion options | One retired | [All five declarations and shared/native request validation](passkey-options-test-audit.md) |
| Sign-in state and preparation | Three retired | [All eight declarations and generated state/reset/SSO checks](signin-state-test-audit.md) |

No retained file is claimed to pass unchanged against the new API. Do not remove tests merely because the generated API compiles. Domain tests that only assert calls into a deleted native service should be retired with that service; retained public behavior must be checked through its current owner. The baseline lists below preserve the full inventory and unresolved gates.

The [packaged contact deletion regression](contact-resource-regression.md) records the shared deletion-receipt projection fix and its Kotlin/QuickJS verification. This does not retire unaudited Android legacy contact tests.

The [Compose completion-gate audit](auth-presentation-test-audit.md) records reproduced and corrected presentation failures. Both old auth-flow and client-flow files are retired after all 11 declarations were mapped, including the intentional change from implicit native selection to explicit core finalization.

Native UI presentation tests are outside this inventory and remain in their existing test target. The migration changes resource calls and owner injection, not the intended screen layout or interaction behavior.

The [configuration helper assertion audit](configuration-test-audit.md) records
the reviewed publishable-key and callback defaults, including intentionally
rejected legacy key formats, all eight retired foreground-policy assertions, and the packaged OAuth/foreground regression proof.

The [storage assertion audit](storage-test-audit.md) records the actual snapshot
encoding and the reproduced first-write Keystore race, with passing replacements.
The [HTTP host audit](http-test-audit.md) records reproduced response-body
cancellation and UTF-8 defects and separates the old header/logging assertions.
The [lifecycle/connectivity audit](lifecycle-test-audit.md) records packaged-core
recovery, connectivity restoration and owner-collection cleanup checks.

## Configuration validation

Replacement owner / evidence: NativeCoreContractTests/ClerkConfigurationTests.swift (iOS); NativeCoreTests/Unit/ClerkConfigurationTest.kt (Android).

Valid test/live keys, whitespace, malformed keys, invalid callback routes, and structured errors are exercised. Old global configuration mutation/proxy behavior is not retained.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/DeviceIdGeneratorTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/DeviceIdGeneratorTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/configuration/ConfigurationManagerAuthRaceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/configuration/ConfigurationManagerAuthRaceTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/sdk/PublishableKeyHelperTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/PublishableKeyHelperTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/sso/RedirectConfigurationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/RedirectConfigurationTest.kt) | 6 |

## Core lifecycle and token policy

The [token cache invalidation regression](token-cache-invalidation.md) records the reproduced cache-clear race and generated QuickJS checks. The [freshness audit](token-freshness-test-audit.md) and [request audit](token-request-test-audit.md) retire both legacy token suites after reviewing all 35 declarations.

Replacement owner / evidence: JavaScript packages/mobile-runtime/test/protocol.test.mjs; clerk-js SessionTokenCache/Session tests; native lifecycle adapters.

Background/foreground notifications delegate reload and token policy to TypeScript. Do not migrate native polling or token-cache algorithms. Process death, native cancellation and all foreground failure paths still require platform evidence.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/configuration/ConfigurationManagerForegroundRefreshTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/configuration/ConfigurationManagerForegroundRefreshTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/configuration/connectivity/NetworkConnectivityMonitorTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/configuration/connectivity/NetworkConnectivityMonitorTest.kt) | 10 |
| [source/api/src/test/java/com/clerk/api/session/SessionTokenFetcherTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/session/SessionTokenFetcherTest.kt) | 25 |
| [source/api/src/test/java/com/clerk/api/session/TokenFreshnessTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/session/TokenFreshnessTest.kt) | 10 |

## Core state, request ownership and races

Replacement owner / evidence: JavaScript packages/mobile-runtime/test/{protocol,attached-core,magic-link,biometrics}.test.mjs and clerk-js resource/request tests; native CoreRuntime and host integration.

The old sequence gates, mutable singleton, middleware pipeline and activation coordinator are not retained as a second state machine. Preserve public race/error outcomes in core tests. Late finalization, reset, sign-out, canceled host effects, shared-owner detach and stale resource handles are tested through the new protocol. This is not a one-for-one replacement of every old race test.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/network/middleware/incoming/ClientSyncingMiddlewareTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/middleware/incoming/ClientSyncingMiddlewareTest.kt) | 9 |
| [source/api/src/test/java/com/clerk/api/network/middleware/incoming/DeviceTokenSavingMiddlewareTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/middleware/incoming/DeviceTokenSavingMiddlewareTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/network/middleware/outgoing/RequestLoggingMiddlewareTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/middleware/outgoing/RequestLoggingMiddlewareTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/network/middleware/outgoing/VersioningUserAgentMiddlewareTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/middleware/outgoing/VersioningUserAgentMiddlewareTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/sdk/ClerkAuthFlowTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ClerkAuthFlowTest.kt) | 9 |
| [source/api/src/test/java/com/clerk/api/sdk/ClerkClientFlowTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ClerkClientFlowTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/sdk/ClerkConfigurationSwitchTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ClerkConfigurationSwitchTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/sdk/ClerkDeviceTokenUpdateTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ClerkDeviceTokenUpdateTest.kt) | 6 |
| [source/api/src/test/java/com/clerk/api/sdk/ClerkTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ClerkTest.kt) | 41 |

## Native diagnostics and package resources

Replacement owner / evidence: Native logging, resource manifests, bundled-core SHA checks and package build tests.

Keep credential redaction and package resource checks. The new core does not retain the old logging callback API. Privacy declarations must describe APIs actually used by each shipped target; copying an obsolete expected manifest is not sufficient.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/log/ClerkLogTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/log/ClerkLogTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/log/SafeUriLogTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/log/SafeUriLogTest.kt) | 2 |

## Live service integration

Replacement owner / evidence: `NativeCoreTests/Android/integration/` and the [integration migration audit](integration-test-audit.md). Compilation and configuration guards are verified; live-service execution and released-app upgrades remain unverified.

Retain as a required live-service proof. Fixture servers do not establish production behavior, real credentials, callback registration or account continuity.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/integration/AuthIntegrationTests.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/integration/AuthIntegrationTests.kt) | 1 |
| [source/api/src/test/java/com/clerk/api/integration/EnvironmentIntegrationTests.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/integration/EnvironmentIntegrationTests.kt) | 1 |
| [source/api/src/test/java/com/clerk/api/integration/IntegrationTestHelpers.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/integration/IntegrationTestHelpers.kt) | 0 |

## Platform prompts and lifecycle

Replacement owner / evidence: iOS NativeCore/AppleAuthentication.swift, AppleBiometricKeyManager.swift; Android NativeCore/Android platform hosts and NativeCoreTests/Android/PasskeyRequestTest.kt.

Keep tests of WebAuthn encoding, callback routes, cancellation, presentation lifetime and key access. Native request encoding is tested, but mocked core/credential results do not prove a successful physical-device browser, passkey, biometric or attestation prompt. These files require assertion-level migration before retirement.

The private passkey manager/helper suites are now retired after their [ten-declaration audit](passkey-host-test-audit.md). The baseline inventory below remains historical; the separate WebAuthn request and service suites still require review.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/attestation/DeviceAttestationHelperTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/attestation/DeviceAttestationHelperTest.kt) | 10 |
| [source/api/src/test/java/com/clerk/api/biometriccredential/BiometricCredentialKeyManagerTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/biometriccredential/BiometricCredentialKeyManagerTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/passkeys/PasskeyCredentialManagerTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/passkeys/PasskeyCredentialManagerTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/passkeys/PasskeyHelperTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/passkeys/PasskeyHelperTest.kt) | 7 |
| [source/api/src/test/java/com/clerk/api/passkeys/PasskeyWebAuthnRequestTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/passkeys/PasskeyWebAuthnRequestTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/sdk/ClerkAttachActivityTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ClerkAttachActivityTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/sso/SSOManagerActivityTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/SSOManagerActivityTest.kt) | 13 |
| [source/api/src/test/java/com/clerk/api/sso/SSOReceiverActivityManifestTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/SSOReceiverActivityManifestTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/sso/SSOReceiverActivityTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/SSOReceiverActivityTest.kt) | 3 |

## Presentation and value helpers

Replacement owner / evidence: iOS Tests/UI; Android source/ui/src/test; generated environment and resource fields.

Existing native presentation tests remain. Factor sorting, provider display values, organization list state and typography belong to UI or canonical source helpers. Audit shared helper assertions before deleting them from the old API test tree.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/sdk/ClerkDevelopmentModeWarningTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ClerkDevelopmentModeWarningTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/sdk/ToOAuthProvidersListTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ToOAuthProvidersListTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/sso/OAuthProviderTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/OAuthProviderTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/ui/ClerkTypographyDefaultsTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/ui/ClerkTypographyDefaultsTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/ui/ClerkTypographyTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/ui/ClerkTypographyTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/user/UserOAuthProviderTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/user/UserOAuthProviderTest.kt) | 2 |

## Native secure storage and migration

Replacement owner / evidence: iOS NativeCoreTests/CredentialUpgradeProof.swift and scripts/test-native-core.sh; Android NativeCoreTests/Android/CredentialUpgradeTest.kt; JavaScript magic-link and biometric suites.

Matching legacy identity, purpose separation, durable clears, scoped metadata and restart recovery have dedicated proofs. Old shared owner-slot conflict resolution is unavailable. Actual released-app upgrades and entitlement/Keystore failure behavior remain release gates.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/biometriccredential/BiometricCredentialLocalStoreTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/biometriccredential/BiometricCredentialLocalStoreTest.kt) | 7 |
| [source/api/src/test/java/com/clerk/api/magiclink/PersistentPendingNativeMagicLinkStoreTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/magiclink/PersistentPendingNativeMagicLinkStoreTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/storage/StorageHelperTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/storage/StorageHelperTest.kt) | 9 |

## Generated codecs and structured errors

Replacement owner / evidence: JavaScript packages/native-bindings/test; packages/mobile-runtime/test/{mapped-records,protocol,selected-code-factors}.test.mjs; native packaged-core suites.

Old Codable/Retrofit enum defaults, DTO constructors and result adapters belong to a removed model. New codecs preserve optional/null distinctions, unknown output enum values, typed unions and error kinds. Native round trips need their own assertions; do not infer them from old DTO tests.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/biometriccredential/BiometricCredentialSerializationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/biometriccredential/BiometricCredentialSerializationTest.kt) | 7 |
| [source/api/src/test/java/com/clerk/api/magiclink/NativeMagicLinkErrorMappingTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/magiclink/NativeMagicLinkErrorMappingTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/magiclink/PkceUtilTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/magiclink/PkceUtilTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/network/model/environment/AuthConfigTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/model/environment/AuthConfigTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/network/model/environment/DisplayConfigSerializationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/model/environment/DisplayConfigSerializationTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/network/model/environment/EnvironmentPasskeyFirstFactorTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/model/environment/EnvironmentPasskeyFirstFactorTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/network/model/environment/OrganizationSettingsSerializationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/model/environment/OrganizationSettingsSerializationTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/network/model/environment/UserSettingsSerializationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/model/environment/UserSettingsSerializationTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/network/serialization/ClerkApiResultConverterFactoryTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/serialization/ClerkApiResultConverterFactoryTest.kt) | 6 |
| [source/api/src/test/java/com/clerk/api/network/serialization/ClerkResultTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/serialization/ClerkResultTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/network/serialization/MergePatchTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/serialization/MergePatchTest.kt) | 12 |
| [source/api/src/test/java/com/clerk/api/signin/SignInPrepareFirstFactorParamsMapTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signin/SignInPrepareFirstFactorParamsMapTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/signin/SignInPrepareFirstFactorTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signin/SignInPrepareFirstFactorTest.kt) | 1 |
| [source/api/src/test/java/com/clerk/api/signin/SignInSerializationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signin/SignInSerializationTest.kt) | 5 |

## TypeScript domain ownership

Replacement owner / evidence: JavaScript packages/clerk-js/src/core/resources/__tests__; packages/mobile-runtime/test/{protocol,sso,selected-code-factors,mobile-sso,mobile-identifier-google,passkey,apple-identity,external-account,magic-link,biometrics}.test.mjs; native packaged-core suites.

Tests expecting a native service mock or request-builder call no longer exercise the implementation. Port their public outcomes to the actual TypeScript owner when the selected contract retains the behavior. Matching method names are not a coverage proof.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/auth/AuthHandleTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/auth/AuthHandleTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/auth/AuthOtpTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/auth/AuthOtpTest.kt) | 1 |
| [source/api/src/test/java/com/clerk/api/auth/AuthTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/auth/AuthTest.kt) | 14 |
| [source/api/src/test/java/com/clerk/api/biometriccredential/BiometricCredentialsTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/biometriccredential/BiometricCredentialsTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/magiclink/MagicLinkDeepLinkParserTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/magiclink/MagicLinkDeepLinkParserTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/magiclink/NativeMagicLinkServiceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/magiclink/NativeMagicLinkServiceTest.kt) | 11 |
| [source/api/src/test/java/com/clerk/api/network/ClerkPaginatedResponseTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/ClerkPaginatedResponseTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/network/api/BiometricCredentialApiTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/api/BiometricCredentialApiTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/network/api/ClientApiTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/api/ClientApiTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/network/api/OrganizationApiTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/api/OrganizationApiTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/network/api/UserApiTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/network/api/UserApiTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/organizations/OrganizationCreationDefaultsTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/organizations/OrganizationCreationDefaultsTest.kt) | 1 |
| [source/api/src/test/java/com/clerk/api/organizations/OrganizationDomainTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/organizations/OrganizationDomainTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/organizations/OrganizationMembershipTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/organizations/OrganizationMembershipTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/organizations/UserOrganizationInvitationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/organizations/UserOrganizationInvitationTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/passkeys/PasskeyAuthenticationServiceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/passkeys/PasskeyAuthenticationServiceTest.kt) | 21 |
| [source/api/src/test/java/com/clerk/api/passkeys/PasskeyCreationServiceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/passkeys/PasskeyCreationServiceTest.kt) | 9 |
| [source/api/src/test/java/com/clerk/api/passkeys/PasskeyServiceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/passkeys/PasskeyServiceTest.kt) | 10 |
| [source/api/src/test/java/com/clerk/api/sdk/ClerkOrganizationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ClerkOrganizationTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/session/SessionDeleteTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/session/SessionDeleteTest.kt) | 1 |
| [source/api/src/test/java/com/clerk/api/session/SessionTaskTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/session/SessionTaskTest.kt) | 9 |
| [source/api/src/test/java/com/clerk/api/session/SessionVerificationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/session/SessionVerificationTest.kt) | 15 |
| [source/api/src/test/java/com/clerk/api/signin/SignInAuthenticateWithRedirectTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signin/SignInAuthenticateWithRedirectTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/signin/SignInCreateTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signin/SignInCreateTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/signin/SignInExtensionsTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signin/SignInExtensionsTest.kt) | 7 |
| [source/api/src/test/java/com/clerk/api/signout/SignOutServiceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signout/SignOutServiceTest.kt) | 8 |
| [source/api/src/test/java/com/clerk/api/signup/SignUpAuthenticateWithRedirectTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signup/SignUpAuthenticateWithRedirectTest.kt) | 5 |
| [source/api/src/test/java/com/clerk/api/signup/SignUpCreateParamsTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signup/SignUpCreateParamsTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/signup/SignUpEmailVerificationStrategyTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signup/SignUpEmailVerificationStrategyTest.kt) | 6 |
| [source/api/src/test/java/com/clerk/api/signup/SignUpFieldCollectionTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/signup/SignUpFieldCollectionTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/sso/ExternalAccountServiceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/ExternalAccountServiceTest.kt) | 6 |
| [source/api/src/test/java/com/clerk/api/sso/GoogleSignInServiceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/GoogleSignInServiceTest.kt) | 10 |
| [source/api/src/test/java/com/clerk/api/sso/SSOExtensionsTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/SSOExtensionsTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/sso/SSOServiceAuthenticateWithRedirectTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/SSOServiceAuthenticateWithRedirectTest.kt) | 1 |
| [source/api/src/test/java/com/clerk/api/sso/SSOServiceCancellationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/SSOServiceCancellationTest.kt) | 4 |
| [source/api/src/test/java/com/clerk/api/sso/SSOServiceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sso/SSOServiceTest.kt) | 7 |
| [source/api/src/test/java/com/clerk/api/user/UpdateMetadataOverloadTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/user/UpdateMetadataOverloadTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/user/UserTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/user/UserTest.kt) | 15 |
| [source/api/src/test/java/com/clerk/api/user/UserUpdateRoutingTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/user/UserUpdateRoutingTest.kt) | 7 |

## Explicitly unavailable legacy feature

Replacement owner / evidence: Migration README: unavailable surfaces.

Hosted portal authentication, live shared-session/watch replication, proxy configuration and offline resource-cache bootstrap have no equivalent in this selected prerelease. These tests are evidence of a product gap, not passing replacement coverage. Apps using these features must not be migrated implicitly.

| Old test file | Source test declarations |
| --- | --- |
| [source/api/src/test/java/com/clerk/api/auth/AuthStartHostedAuthTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/auth/AuthStartHostedAuthTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/hostedauth/HostedAuthCallbackTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/hostedauth/HostedAuthCallbackTest.kt) | 11 |
| [source/api/src/test/java/com/clerk/api/hostedauth/HostedAuthPkceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/hostedauth/HostedAuthPkceTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/hostedauth/HostedAuthRedirectUrlTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/hostedauth/HostedAuthRedirectUrlTest.kt) | 10 |
| [source/api/src/test/java/com/clerk/api/hostedauth/HostedAuthServiceTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/hostedauth/HostedAuthServiceTest.kt) | 14 |
| [source/api/src/test/java/com/clerk/api/sdk/ClerkOfflineCacheTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ClerkOfflineCacheTest.kt) | 8 |
| [source/api/src/test/java/com/clerk/api/sdk/ProxyUrlConfigurationTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/ProxyUrlConfigurationTest.kt) | 3 |
| [source/api/src/test/java/com/clerk/api/sdk/SharedSessionSyncConfigTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sdk/SharedSessionSyncConfigTest.kt) | 1 |
| [source/api/src/test/java/com/clerk/api/sharedsession/SharedSessionSyncCoordinatorTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sharedsession/SharedSessionSyncCoordinatorTest.kt) | 6 |
| [source/api/src/test/java/com/clerk/api/sharedsession/SharedSessionSyncProviderTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sharedsession/SharedSessionSyncProviderTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/sharedsession/SharedSessionSyncPublicApiTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sharedsession/SharedSessionSyncPublicApiTest.kt) | 2 |
| [source/api/src/test/java/com/clerk/api/sharedsession/SharedSessionSyncSnapshotTest.kt](https://github.com/clerk/clerk-android/blob/1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f/source/api/src/test/java/com/clerk/api/sharedsession/SharedSessionSyncSnapshotTest.kt) | 7 |

The [Apple SSO verification errors](apple-auth-errors.md) records generated Apple transfer and restriction behavior, the shared verification-error serializer correction, and packaged-engine evidence.

The [email-link completion audit](email-link-completion.md) records legacy flow-kind migration, incomplete callback results, cleanup/ownership checks, and generated authentication parameter coverage.
