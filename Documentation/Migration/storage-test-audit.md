# Android credential-storage assertion audit

Baseline: `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. The review read every
`StorageHelperTest` assertion, `StorageCipher`, the cached-state/shared-snapshot
models, and the old `ClerkApi.json` configuration. Shared-session convergence and
biometric-key algorithms require their own audits.

Two migration problems were found. The old serializer uses SnakeCase and omits
default-valued fields: a snapshot contains `instance_id` and `device_token`, and
can omit `schema_version` when it is 1. The importer and positive/clear fixtures
now use that actual format. Unsupported explicit versions and different instances
remain ineligible for import.

Independent credential purposes also share a Keystore alias. Concurrent first
writes could create it repeatedly, replacing another writer's key. The new public
storage test reproduced `KeyPermanentlyInvalidatedException` during encryption
before the fix. Key creation now rechecks the alias under a process lock. A
bounded set of file locks also synchronizes separate `AtomicFile` objects for the
same path; that file API does not itself provide synchronization.

| Old assertions | Current disposition |
| --- | --- |
| Concurrent initialization creates one reflected preferences object | The global `StorageHelper` and initialization field are removed. Public tests launch concurrent first writes for all four credential purposes and verify all values decrypt after reconstruction. |
| Save/load/update/delete after initialization | Import, reconstruction and durable clear run against real Keystore and app-private files. There is no separate initialization API. |
| Multiple independent keys; deleting one preserves another | Client, magic-link, biometric-metadata and cleanup records use separate files. Tests check that clearing a magic link preserves the client and a different publishable key does not restore the credential. |
| Empty strings do not overwrite/store old values | The generic key-value API is removed. Clearing uses `remove()` and a durable empty record; the old ignore-empty-string policy is not a replacement contract. |
| Stored preferences are encrypted | New records use AES-GCM/Android Keystore under `noBackupFilesDir`. Reconstruction exercises decryption. The old `clerk:v1:` preference envelope remains a migration input. |
| Plaintext becomes an encrypted preference | The adapter can read authorized plaintext or encrypted legacy values and writes a new encrypted record. Old preferences remain intact; cached-key/explicit-key binding is required. This is not an in-place rewrite of the old SDK's preference. |
| Malformed ciphertext is deleted and returns null | Intentionally changed: decode/decrypt failure is reported without silently deleting old data. Permanent Keystore invalidation/recovery still needs device validation. |
| Save/load/delete never throw while racing with initialize | The initialization race is gone. New tests cover concurrent first key creation and writes through separate objects to one file. The final result must decrypt to one complete written value, and subsequent clear must survive reconstruction. Expected IO/security failures may throw. |

The initial seven `CredentialUpgradeTest` methods passed on the emulator after the fix.
The first-write test runs six fresh aliases with four concurrent purposes each;
the shared-file test checks four rounds of twelve writers. This is a storage and
format proof, not a signed-in released-app upgrade. The remaining shared-session and biometric tests require their own reviewed retirements.


## Integrity and retirement follow-up

The current `CredentialUpgradeTest` now has nine passing declarations, including the later token-only and biometric-policy migration checks. The new `CredentialStorageIntegrityTest` adds six passing emulator tests using real Android Keystore and app-private encrypted files. Its ContextWrapper redirects legacy preferences to a per-test UUID namespace; cleanup removes only that namespace, its encrypted file, and its key alias.

The new checks prove:

- Authorized plaintext legacy credentials, including Unicode, import to an encrypted record, survive reconstruction and update, and remain cleared after reconstruction. The original legacy preferences, including an unrelated DEVICE_ID, remain unchanged.
- Modifying the ciphertext's authentication tag causes repeated reads to fail without deleting/replacing the file or importing an older token. Restoring the original fixture bytes makes the same current credential readable again.
- Deleting the fixture's Keystore key causes reads to fail without generating a replacement key, replacing the encrypted file, or importing the older token. The current error code is `legacy_credential_key_unavailable` even when the missing key belongs to the new record.
- A malformed legacy encrypted envelope is preserved and fails repeatedly without creating a new record or key.

These are storage failure and format checks. Deleting a test key is not biometric-enrollment invalidation, and no device-security-setting change or signed-in released-app upgrade is claimed. Expected decode, IO and security failures are observable rather than silently converted to missing credentials. The backup-recovery correction below is the only SDK implementation change in this follow-up.

### Reproduced AtomicFile backup recovery defect

The host previously checked only `AtomicFile.baseFile.exists()` before reading. Android's AtomicFile.openRead restores a committed `.bak` record when present, including when the base file is missing. Skipping openRead in that state instead ran legacy import, and save then overwrote the recoverable record.

Two separate tests reproduced both failures on the Android 16 emulator: a valid committed credential was replaced by the older DEVICE_TOKEN, and a committed null/clear record also restored that older token. Both tests create a normal encrypted record through the actual storage adapter, move its bytes to the recoverable backup path, and reopen through a fresh adapter. The failing run discovered six tests with two failures.

The read path now recognizes either the base or backup file and delegates recovery to AtomicFile.openRead under the existing file lock. All six integrity tests pass after the fix. Both recovery cases check byte-for-byte restoration of the committed encrypted record, removal of the backup by AtomicFile, and a second successful reconstruction. The existing nine migration/concurrency tests also pass after the fix. The JavaScript bundle and generated API did not change.

This is a deterministic interrupted-write-layout test using the actual Android AtomicFile/Keystore implementation. It does not claim an OS-killed process, an API 24 device run, or a released-app upgrade.

All nine declarations in old `StorageHelperTest.kt` and all five in `DeviceIdGeneratorTest.kt` were reviewed with their complete helper/assertion bodies and are now retired.

### Every StorageHelper declaration

| Legacy declaration | Current disposition and evidence |
| --- | --- |
| `initialize called concurrently from multiple threads initializes exactly once with no exceptions` | The global initializer and reflected secureStorage field are removed. The six-alias concurrent-first-write test checks all four purposes remain decryptable; the shared-file test checks independent storage objects serialize complete writes. |
| `saveValue with empty string does not write to SharedPreferences` | The old generic ignore-empty-write policy is removed. Credentials have explicit write/remove operations; durable remove is checked after a stored value and reconstruction. This is not a promise that write("") ignores the caller's input. |
| `saveValue stores encrypted value in SharedPreferences` | The new plaintext-import test checks the destination file is not the credential text and decrypts after reconstruction. Source uses AES-GCM/Keystore; authenticated-ciphertext corruption is rejected. The old fake Base64 cipher and clerk:v1 preference prefix are migration inputs, not the new storage format. |
| `saveValue with empty string when no existing value does not write to SharedPreferences` | Removed with the generic key-value API. New empty records intentionally persist a clear marker to prevent legacy reimport; the API has no separate initialization phase. |
| `loadValue migrates legacy plaintext values to encrypted storage` | The new Unicode plaintext test verifies encrypted destination/reconstruction and byte-preserved original preferences. Migration is not an in-place rewrite of the old preference. |
| `loadValue deletes malformed encrypted values and returns null` | Intentional behavior change: the malformed-legacy test verifies a surfaced failure and unchanged old data, with no new destination. Corrupted current ciphertext and missing-key tests separately verify no fallback to older credentials. |
| `after initialize isInitialized returns true and values can be saved loaded and deleted` | The private initialized field is removed. Import/read/update/remove/reconstruction are checked through AndroidCredentialStorage directly. |
| `multiple save and load operations work correctly after initialization` | CredentialUpgradeTest checks client, magic-link, biometric metadata and cleanup purpose separation. Clearing one purpose preserves the client; another publishable key cannot import it. |
| `load save and delete never throw when racing with initialize` | The initialization race is removed; concurrent key creation/file-write tests cover the new public operations. Legitimate IO/security errors may throw, as the new integrity cases verify. There is no blanket never-throw contract. |

### Every DeviceIdGenerator declaration

This private generator persisted a UUID in DEVICE_ID for the old `VersioningUserAgentMiddleware`'s `x-native-device-id` header. It was not the DEVICE_TOKEN bearer credential. The selected core profile has no installation-ID generator/cache or automatic old native-device header injection; the native HTTP host forwards core-supplied headers. The old DEVICE_ID remains untouched during credential migration. This explicitly does not promise continuity of old analytics/installation identifiers.

| Legacy declaration | Current disposition |
| --- | --- |
| `initialize and getDeviceId returns existing device ID when one exists` | Removed installation-ID API. Its one-read/no-save mock expectations are not requirements of credential import; the new plaintext test preserves the unrelated old ID without treating it as authentication. |
| `initialize is thread safe and generates only one device ID` | Removed UUID/cache creation algorithm. Its concurrent identical-UUID/single-save assertions have no generated domain equivalent. Credential key-creation concurrency is separately tested and does not stand in for a device-ID generator. |
| `getDeviceId automatically initializes when not previously initialized` | Removed lazy initialization API and its one-read/one-write expectations. No replacement installation UUID is generated by the new host. |
| `consecutive calls to getDeviceId return same ID with caching` | Removed installation-ID cache. Credential reconstruction tests establish token persistence, not this unrelated cache contract. |
| `device IDs are cached and reused after initialization` | Removed UUID reuse algorithm and mock storage-call counts. The legacy ID stays in the old preferences; it is not silently recreated as a new public identifier. |
