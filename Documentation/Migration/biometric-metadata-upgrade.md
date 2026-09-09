# Biometric metadata upgrade

The shared TypeScript core at pin `ad16bc0d65` accepts the previous Android
SDK's biometric metadata. The native storage adapter continues to move scoped,
encrypted bytes; TypeScript owns record normalization, selection and cleanup.

## Reproduced defect

At baseline `1ea9f972`, `BiometricCredentialLocalStore` encoded records with
`ClerkApi.json`. Its `SnakeCase` naming strategy also applies to the explicit
`SerialName` values. The stored fields include `local_key_id`, `user_id`,
`app_identifier`, `created_at` and `updated_at`. The encoder omits default values,
including `biometry_or_device_passcode`. The new core originally required
camelCase fields and an explicit policy, so imported credentials became
`noLocalCredential` despite the encrypted storage import succeeding.

`CredentialUpgradeTest.legacyBiometricMetadataReachesTheCoreWithItsOriginalPolicy`
uses a copied serializer shape and the exact old JSON configuration. It asserts
the actual encoded names and omitted policy, encrypts the output with the old
storage format, imports through `AndroidCredentialStorage`, and invokes the
packaged core's generated `localAvailability` and `forgetLocalCredentials` APIs.
The availability assertion failed against the prior bundle. The fixture covers
both the omitted default and an explicit `biometry_current_set` policy, and
checks that cleanup names the original local key and survives storage reopen.

Embedded core tests additionally verify canonical biometric sign-in uses the
imported record and preserves explicit finalization. Current camelCase records
retain their existing validation. Legacy normalization applies only to Android;
a missing policy is defaulted only in the legacy snake_case shape.

## Limits

The Android test uses real encrypted preferences, Android Keystore encryption,
AtomicFile storage and QuickJS execution. Biometric key availability/deletion and
HTTP are fixture capabilities. This is not a physical biometric prompt or an
upgrade of a released application with an enrolled user. Those remain release
gates. Apple metadata uses a separate camelCase/millisecond encoder and does not
need this Android normalization.

Validation: the embedded suite passes 213 tests. Android's migration class
passes eight tests after repackaging (the new format case failed before it).
The separate packaged-core class passes eleven tests. Apple passes 59 iOS
Simulator and 62 macOS contract tests, including its added legacy-metadata
storage check. These are scoped suites, not a complete release acceptance claim.

The subsequent core pin `617418e19c` adds an optional Apple installation-marker
capability. Android does not advertise it, so its existing encrypted metadata
and cleanup behavior remain in use. The repackaged Android contract class passes
11 tests with that capability absent.
