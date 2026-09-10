# Organization results and query filters

The packaged core now preserves returned organization resources independently of an older active root with the same server ID. The original TypeScript implementation can retain both objects. The bridge must not rebind the active root to the return value and then overwrite that value while reconciling the root. The exact root instance still retains its handle; collection refreshes keep their existing identity policy.

Two adjacent fixes belong to shared `clerk-js`:

- Logo deletion returns an image deletion receipt from the server. `setLogo(SetOrganizationLogoParams(null))` now reloads the organization after that receipt instead of decoding it as an organization. A complete organization response is still supported. Failed deletion or refresh throws a structured failure and preserves readable held state; refresh failure does not imply deletion rollback.
- Empty array filters are omitted from paginated requests. In particular, `getInvitations(GetInvitationsParams(status = emptyList()))` no longer sends an invalid blank status. Nonempty role/status arrays still serialize as repeated fields through the existing FAPI transport.

`NativeCoreTests/Android/OrganizationResourceTest.kt` exercises six generated scenarios: deletion receipt, complete response, delete rejection, refresh failure, separately fetched/updated organization and empty invitation filters. Five failed against the preceding bundle. All six pass against the pinned bundle below. The full deterministic QuickJS instrumentation run passes 84 tests. It invokes the runner directly, excluding only the opt-in benchmark and live-startup classes; this result does not depend on Gradle's comma-separated class filtering.

The shared generated-resource suite adds 47 cases for organization/member mutations, role/member/invitation/domain/request pagination, invitation and request decisions, domain affiliation verification and enrollment, both logo upload representations, structured errors, and known/unknown enrollment values. All 400 embedded tests pass, along with 12 relevant source tests, runtime TypeScript compilation, generation and bundle/Expo attached-transport reproducibility.

Kotlin consumers should keep the resource returned by methods that return a separate organization. The Compose profile-update model explicitly reloads its held organization before publishing success; creating an organization selects its ID through the core. No native organization service or state machine is added.

The focused organization account-list model checks also pass: seven Swift tests and two Compose instrumentation tests. Regenerated preview resources include the distinct organization reference produced by collection hydration.

Both SDKs pin:

- Core revision: `037bf3447780d13d757d077c6c6152372dabbe8c`
- Contract: `0f8387f260072ba6f894442b73d50afa6bda5f1205ed3c9209f5d6b1cfba16ce`
- Bundle SHA-256: `267191c1f5cfc44024a63a3d79f2534f8cf9f4666c2d58a6280feff4efeab8b6`

Apple's corresponding packaged contract suites pass 85 macOS and 82 iOS Simulator tests. These checks use deterministic host responses. They do not establish live service journeys, physical-device performance, OS credential/browser presentation, or actual old-major app-upgrade continuity.
