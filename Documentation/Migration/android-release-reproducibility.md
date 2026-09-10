# Android release reproducibility

Two clean builds of the same committed source in different directories originally
produced different QuickJS libraries for all three ABIs. Section-level comparison
found only `.note.gnu.build-id` differed. The executable sections and every other
AAR entry matched. The linker had derived build IDs from debug metadata containing
checkout and build paths, before the release libraries were stripped.

The Android CMake target now maps source, build and NDK paths to stable prefixes.
Debug information and ELF build IDs remain available; the build does not remove
build IDs to obtain matching hashes. In the initial local comparison, both builds
with these options produced AAR SHA-256
`273f800bfb170ec89eb4d636f0305369ce7f42c970ed7d32851e9199c232bccc`.
The existing AAR packaging verifier accepted the normalized output.

## Repeatable gate

With the configured Android SDK and JDK, run from a clean committed checkout:

```sh
python3 -B scripts/verify-reproducible-aar.py --output build/reproducible-core
```

Use a fresh output directory. The script rejects modified tracked files, archives
committed `HEAD`, and extracts two independent source copies to differently named
fresh directories. Both assemble the release API AAR with build and configuration
caches disabled and every task rerun. Neither copy contains prior CMake objects or
Gradle project outputs. Shared downloaded dependencies may still be cached.

Each output must satisfy the existing AAR contract: the packaged core and notices
match their manifest, all selected ABIs are present, native load segments have
16 KB alignment, and the manifest has the required permissions and minSdk. The
complete AAR files must then match byte for byte. The output retains both AARs,
build logs, the source revision/archive hash, Gradle and Java version information,
per-ABI hashes and any differing entries. Temporary source/build trees are removed.

The Android build workflow now runs the same gate on Linux and uploads its output.
The [September 10 Linux run](https://github.com/clerk/clerk-android/actions/runs/34508121416) passed this gate at the committed fix. This gate compares two builds
on the same pinned toolchain host; it does not promise identical output across
macOS and Linux NDK distributions. The observed cross-host native-library difference is characterized below. This packaging check does not replace
runtime execution, live authentication, actual app upgrades or physical-device gates.

## Committed-source local evidence

The complete gate passed locally at `00df8158482467e9a0b5ea3171c32b8c9e4559e7`,
with two fresh source archives and 36 executed tasks in each build. Both artifacts
have the same `273f800b…` hash recorded above. [The macOS report](evidence/android-reproducibility/macos-report.json)
records the exact source, toolchain, AAR and per-ABI identities.
[The original failing comparison](evidence/android-reproducibility/before.json)
records the differing AARs and build-ID-only section differences.
[The normalized section report](evidence/android-reproducibility/normalized-sections.json)
confirms that all three libraries retain nonempty build IDs and that the fix changed
only those build-ID sections in the inspected stripped libraries; their executable
code and other sections are identical to the prior build.

## Linux CI and cross-host comparison

[The successful build workflow](https://github.com/clerk/clerk-android/actions/runs/34508121416)
ran the same gate at `00df8158482467e9a0b5ea3171c32b8c9e4559e7`. Both downloaded Linux
AARs match SHA-256 `fa35a3411bc8522ebcbb50caf50bce47abc8bda0c513665a6f74d6a7ee0f57fa`.
[The Linux report](evidence/android-reproducibility/linux-report.json) and
[workflow record](evidence/android-reproducibility/build-workflow.json) preserve
the source revision, toolchain, per-library identities and completed jobs.

Comparing the normalized macOS and Linux artifacts found differences only in their
three native libraries. Within each library, the `.comment` and `.note.gnu.build-id`
sections differed; every other file-backed ELF section, including `.text`, matched.
The macOS compiler comments identify `-bolt`/`-mlgo`, while Linux identifies
`+bolt`/`+mlgo`; ARM outputs also include a shared prebuilt compiler comment.
[The comparison report](evidence/android-reproducibility/cross-host-sections.json)
records these strings and code hashes. This supports reproducibility within each
recorded host toolchain. Cross-host byte identity remains outside that guarantee.

[The runtime test workflow](https://github.com/clerk/clerk-android/actions/runs/34508124478)
also passed all four jobs at the same source revision: 12 API and 491 UI JVM cases,
the packaged-core and capability probes on API 24 and API 36 x86_64, and the rendered
API 36 authentication journey. Downloaded XML contains no failures, errors or skips.
[The test record](evidence/android-reproducibility/test-workflow.json) retains the
exact case reports, individual job URLs and unit-report hashes.
