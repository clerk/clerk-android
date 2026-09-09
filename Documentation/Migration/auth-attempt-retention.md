# Authentication attempt retention

The packaged core at TypeScript pin `b9506775b3` keeps completed sign-in and
sign-up resources available for explicit finalization after an ordinary client
refresh clears the server attempt field. Identified mobile attempts use the
existing TypeScript resource signals; empty initial attempts use the live client.

The packaged regression starts with a completed sign-in while another session
is selected, refreshes that session with a cleared server sign-in, and verifies
the same generated attempt can still finalize its created session. It reproduced
`stale_resource`/lost-attempt behavior before the bundle update. Embedded tests
cover both sign-in and sign-up, verification before refresh, and sign-out/client
replacement retiring the old attempt.

Discarded attempts cannot replace the current TypeScript authentication state
with late resource updates. The shared state owner also retires retained attempts
when client identity changes or all sessions are removed. This adds no Kotlin
authentication state machine or native completion registry.
