# Canonical token snapshot behavior

`NativeCoreTests/Android/TokenSnapshotTest.kt` exercises seven generated-resource
scenarios against the packaged QuickJS core. Initial session snapshots avoid a
mint request. Clearing the cache requires a new request, with the last-active
token retained as input to the session minter. A forced mint on a full timestamp
tie stays cached until a newer server response arrives.

After a session reload carries an updated client snapshot, stale tokens cannot
replace fresher cached tokens; fresh tokens are available without another mint.
An incoming snapshot wins on a full timestamp tie under the existing TypeScript
policy. If both tokens are expired, the fresher last-active token survives as
the input to the next mint. The retained generated session handle remains valid.

All seven instrumentation cases pass on the Android 16 emulator. Matching
embedded tests and iOS Simulator cases pass, along with 200 TypeScript source
token/session tests. This is deterministic host-fixture evidence; live service
acceptance, physical lifecycle transitions, and released-app upgrades remain
separate gates. No native cache policy or production runtime changes were added.
