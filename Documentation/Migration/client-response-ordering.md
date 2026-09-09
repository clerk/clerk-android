# Shared client response ordering

The shared transport also requires a returned or stored client credential before
hydrating a native client snapshot. `canonicalClientRequiresANewOrRestoredCredential`
reproduced an active fixture session being exposed without either credential,
then passed with the bundle pinned to TypeScript commit `2122fc015f`. It checks
both rejected credentialless startup (`missing_client_credential`) and successful
startup with a restored credential when HTTP responses omit the header.

The pinned TypeScript core now protects client state from an older HTTP reply
arriving after a newer accepted response. The native audit reproduced two
generated `Session.reload()` calls where the newer response reported a pending
organization task, but a delayed older response restored active state and
overwrote the client credential.

The shared mobile credential transport checks client snapshots before credential
writes and FAPI hydration. Newer request sequences are accepted. An older/equal
sequence requires a newer server date, or a tied date plus newer client version.
Sequence/date watermarks do not move backwards; authentication invalidation
starts a new date history and rejects requests from the previous generation.
This is shared TypeScript behavior, with no Kotlin response-ordering policy.

`PackagedCoreTest.olderClientResponseCannotRemoveANewerPendingTaskOrCredential`
failed against the prior bundle, then passed against this bundle on the Android
16 emulator. It checks a structured `stale_client_response` error, retained
pending task and credential, stable session wrapper, and another usable reload.
The embedded suite additionally covers both client piggyback locations, direct
foreground refresh, server-date/version precedence, and reset behavior.

The rule applies to responses carrying a client snapshot. It does not serialize
unrelated domain-only results or prove real server clock/version behavior. The
old Kotlin DTO/service and shared-session convergence assertions still require
their own migration audits.
