# Organization list pagination

The generated API accepts a page number and page size. Native UI previously
converted its loaded row count into a fractional page. After accepting nine of
ten invitations, the remaining pending count was one; converting that count
through `1.0 / 10 + 1` produced an outgoing offset of
`1.0000000000000009` when the core converted the page back to an offset.

Organization account lists, member lists, invitations, membership requests and
verified domains now request the whole page containing the next row. Appending
deduplicates resource IDs because a partial page can overlap previously loaded
rows. Accepted invitation rows remain visible, and only pending rows contribute
to the invitation pagination position.

`OrganizationAccountListCoreTest` runs the actual Compose view model against the
packaged QuickJS core with an in-memory service fixture. It checks accepting one
of two invitations and nine of ten invitations, loading the remaining rows,
preserving accepted status and public organization data, unique visible IDs,
pending counts, end-of-list state, and integer outgoing offsets. Before the fix,
the ten-row case failed with the fractional offset above; both cases pass after
the fix on the Android 16 emulator. All UI production sources compile as part of
the instrumentation build.

The fixture rejects non-integer offsets to enforce the pagination contract.
This is deterministic request evidence, not an observation of a live backend
error or a visual interaction test. The same pagination arithmetic was changed
in the related UI lists; their complete interactive journeys remain separate
verification work.

The September 10 CI run also exposed a stale JVM pagination mock that still
expected fractional page `1.5`. Its membership, invitation and suggestion stubs
now return an overlapping whole page on the next request, retaining assertions
that newly returned rows are appended, prior IDs are not duplicated, and all
three lists reach their reported end. The production pagination behavior is
unchanged; the fixture now follows the whole-page contract already checked by
the packaged-core tests.
