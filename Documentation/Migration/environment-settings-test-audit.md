# Environment settings assertion audit

All fifteen declarations in the five old environment test files were reviewed against baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. Their bytes matched the baseline before retirement. [The proof](evidence/environment-settings/proof.json) records the original hashes and declarations, current source hashes, and replacement results.

## Reproduced presentation defect

The migrated automatic-passkey gate read `usedForFirstFactor` and `allowAutofill` but omitted `enabled`. A disabled passkey attribute with first-factor use still set therefore started authentication. The new `disabledPasskeyDoesNotStartAuthentication` unit case failed before the fix; all five availability cases pass after requiring both attribute flags and autofill. These cases hydrate generated resources from the TypeScript preview projection and call the actual presentation helper, without its Boolean test overrides.

The production Compose journey uses the packaged QuickJS core and fixture HTTP. With passkeys supported by the host but disabled in the environment, it submits no passkey preparation request. Email sign-in still displays an incorrect-code error, accepts a corrected code, and invokes completion and session activation exactly once. Three captured screens show identifier entry, the verification error, and completion. The existing ordinary-email and passkey-preparation-error journeys also pass.

## Current settings owner

The TypeScript environment resources own hydration and defaults. Fourteen new packaged `EnvironmentSettingsTest` cases check generated Kotlin values after both initial connection and `environment.reload()`, retaining the environment resource identity. They cover session-minter flags, development warning/support email, missing/null/complete organization settings, immutable attributes with their other factor fields, and enabled/registration-only/disabled passkey configurations.

The shared runtime settings suite now has 27 passing cases. Added cases cover null organization children; missing, null and populated support email; immutable true/false/omitted with all factor fields; and passkey flags. The selected TypeScript `Attributes` contract requires every named attribute key. An environment response missing `passkey` fails with `invalid_projection:EnvironmentResource.userSettings`; the old native DTO's arbitrary missing-map-entry acceptance is not the generated wire contract. A separate UI availability case still verifies defensive behavior if an already-hydrated native map lacks that entry.

No native DTO hydration or environment domain helper is restored. The generated attribute properties retain TypeScript meanings. The one production change is the Compose presentation gate. The shared core bundle and generated public API are unchanged.

## CI verification

Commit `c2d19b7a58ca5e5184f6ffd80b7fb903c1bad505` passes all four jobs in [Android test CI](https://github.com/clerk/clerk-android/actions/runs/34543351500). Downloaded XML verifies 566 unit cases (39 API and 527 UI), two API 24 emulator cases, and 140 API 36 emulator cases without failures or skips. The API 36 results include all fourteen environment cases and all three rendered authentication journeys. The five availability unit cases match the local regression suite by name. [CI evidence](evidence/environment-settings/ci/proof.json) records 25 canonical instrumentation reports, unit-report hashes and the three inspected disabled-passkey journey screenshots; identical backup XML is not counted twice.

All three jobs in [build and packaging CI](https://github.com/clerk/clerk-android/actions/runs/34543353377) also pass on that commit. Independent inspection of both downloaded release AARs confirms identical bytes, SHA-256 `833e066b3c76432e180621bff174241bee0d98caf23e6e6e3749df42f184623e`, and the expected core bundle `406af90fdb32effc6fb3c1b35b52ceea639a68ceb3874b4d1390fd958a339f7d`.

## Every legacy declaration

| Old file / declaration | Current disposition |
| --- | --- |
| AuthConfig: `decodes session minter flag` | `sessionMinterEnabled` checks true after connection/reload; shared auth configuration also covers native flags. |
| AuthConfig: `defaults session minter to false when omitted` | `sessionMinterOmittedDefaultsFalse` preserves the false default with single-session mode false. |
| DisplayConfig: `show dev mode warning deserializes correctly` | `developmentWarningEnabled` checks the projected true value. |
| DisplayConfig: `show dev mode warning defaults to false` | `developmentWarningOmittedDefaultsFalse` checks omission; shared cases separately check explicit false. |
| DisplayConfig: `support email deserializes correctly` | `supportEmailPreserved` checks `help@example.com`; shared cases also check missing/null values default to an empty string. |
| EnvironmentPasskeyFirstFactor: `enabled when passkey is a first factor` | `firstFactorPasskeyPreserved` checks both generated flags; `enabledFirstFactorPermitsAutomaticPasskey` checks the UI gate with autofill enabled. |
| EnvironmentPasskeyFirstFactor: `disabled when passkey is registration only` | `registrationOnlyPasskeyPreserved` retains enabled=true/use=false; the matching availability case suppresses automatic sign-in. |
| EnvironmentPasskeyFirstFactor: `disabled when passkey attribute is disabled` | `disabledPasskeyPreserved` retains enabled=false/use=true; the reproduced availability regression and rendered journey require no automatic preparation. |
| EnvironmentPasskeyFirstFactor: `disabled when passkey attribute is absent` | The old native computed properties are removed. The UI's missing-entry check returns false; a missing required entry in an actual environment response is explicitly rejected by the selected generated contract. |
| OrganizationSettings: `decodes missing organization settings fields with defaults` | `organizationDefaults` checks all original fields, including max memberships=1, empty enrollment modes, disabled actions/slug/creation; it also checks a null default role. |
| OrganizationSettings: `decodes null organization settings children with defaults` | `organizationNullChildrenUseDefaults` checks all fields with actions/domains/slug/creation set to null. The shared null-child case verifies the same normalization. |
| OrganizationSettings: `decodes complete organization settings payload` | `completeOrganizationSettings` checks every original value, including max memberships=5, automatic invitation enrollment, and `org:member`. |
| UserSettings: `testAttributesConfig_deserialize_withImmutable_present` | Separate immutable true/false packaged cases preserve each Boolean. |
| UserSettings: `testAttributesConfig_deserialize_withoutImmutable` | `immutableOmittedPreservesFactorSettings` preserves absence as Kotlin null. |
| UserSettings: `testAttributesConfig_other_fields_still_deserialize_whenImmutable_changes` | All three immutable cases check enabled, required, first/second-factor use, factor arrays, verification arrays, sign-up verification and attribute name. |

These fixture checks establish projection and presentation behavior, not live backend configuration compatibility or signed-in application upgrades. Those remain broader migration gates.
