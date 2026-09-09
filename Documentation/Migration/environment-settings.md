# Partial environment settings

The shared `OrganizationSettings` now initializes `forceOrganizationSelection` to false. Previously that required boolean was undefined when the server omitted organization settings or sent a partial object, and generated projection could reject otherwise valid startup. Explicit true values remain honored.

Sixteen shared fixture cases cover missing/empty/partial/full organization settings, development-warning defaults, native auth/session-minter flags, and billing metadata. The full embedded suite passes 181 tests. The packaged Kotlin test starts with missing and partial organization settings, then uses generated `environment.reload()` and checks updated state and wrapper identity after awaiting it. The Android packaged target passes seven tests; the iOS contract target passes 50. No Kotlin environment decoder or default-selection policy is introduced.

This verifies settings hydration and observation. It does not establish live Dashboard propagation, native payment-processing support, or an equivalent to the old iOS refresh-checkpoint/coalescing helper API.
