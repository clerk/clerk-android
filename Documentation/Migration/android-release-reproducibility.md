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
Its execution on the committed fix is still pending. This gate compares two builds
on the same pinned toolchain host; it does not promise identical output across
macOS and Linux NDK distributions. The earlier cross-host native-library difference
remains a separate issue to characterize. This packaging check does not replace
runtime execution, live authentication, actual app upgrades or physical-device gates.
