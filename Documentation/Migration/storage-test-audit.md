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

All seven `CredentialUpgradeTest` methods pass on the emulator after the fix.
The first-write test runs six fresh aliases with four concurrent purposes each;
the shared-file test checks four rounds of twelve writers. This is a storage and
format proof, not a signed-in released-app upgrade. Old helper/shared-session/
biometric tests remain retained pending their reviewed retirements.
