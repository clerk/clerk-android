# Empty client refresh

The shared resource fetcher treated `{ "response": null }` as a client object
and erased the loaded client and selected session. `EmptyClientResponseTest`
reproduced that failure on the packaged QuickJS core, both during an ordinary
foreground refresh and after a newer response had introduced a pending session
task. Matching Swift and embedded tests reproduced it too.

The TypeScript fetcher now passes explicit null to the existing resource handler
and preserves support for wrapped and unwrapped resource responses. A null
refresh preserves the client, selected session, pending task and stored
credential. A real client response with no sessions still removes selection.
No native response policy was added.

Both SDKs package core `acfc5359dab0eca6a46ceed3282f2057bc0321f2`, SHA-256
`3c4ea6180da6abd15f6112ce669a8f51a4f48fb389acc2f45331e411323a7bf4`.
All 329 embedded tests, 157 focused source resource tests, 81 macOS and 78 iOS
Simulator contract declarations, and 69 Android fixture cases pass. Binding
generation, runtime type checking, reproducibility and attached-transport checks
pass; generated public API snapshots are unchanged.

The Android fixture run excludes the opt-in benchmark. Its live-startup case
reports an unmet assumption because no live-service configuration was supplied;
the runner XML labels this as a failure despite Gradle succeeding. That case is
not counted among the 69 passing fixture cases. These checks do not establish
live outage handling, actual released-app upgrades or physical performance.
