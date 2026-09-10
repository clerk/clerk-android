# Android browser activity lifecycle

Two platform lifecycle defects were reproduced against `f2cb7be9bd7b1b118a46fcb45cc9cbcb3eb53bf3` on the API 36 arm64 emulator:

- Completing one authorization resumed its caller before the manager activity was marked finished. If the caller immediately opened another authorization, Android could reuse the closing `singleTask` manager. The new request remained pending without opening its browser. The manager now calls `finish()` before resuming the continuation, matching the existing cancellation ordering.
- Rotating the device while the external activity was foreground caused Android to recreate the stopped manager when the callback returned. That manager could resume without the new intent's URI and treat the return as cancellation. The callback receiver now retains the validated URI in the pending presentation; the recreated manager consumes it on resume.

These are native presentation responsibilities. The URI is returned unchanged to TypeScript, which continues to own provider interpretation, reconciliation, transfers and session activation. Pending presentation state remains in memory; this change does not add process-death recovery.

`BrowserActivityLifecycleTest` runs the actual SDK manager and callback receiver through Android's activity lifecycle. Its four required cases verify normal callback delivery, cancellation through Back, immediate sequential authorizations, and callback completion after real emulator rotation. The rotation case requires a new manager instance and exactly one external launch. The sequential case requires both distinct authorization URLs to be presented and both callbacks to complete.

The external browser intent is intercepted and launches Android Settings as an opaque foreground stand-in. Callback delivery uses an external `am start` to the SDK receiver. No provider or network is involved. This proves framework activity ordering and recreation on the emulator; actual Chrome/custom-tab behavior, live authorization, process death and physical-device prompts remain separate release checks.

[Before evidence](evidence/browser-activity/before/proof.json) retains the unchanged runtime hash, exact test source and XML: the original implementation passes normal callback and Back cancellation but fails sequential presentation and rotation. [After evidence](evidence/browser-activity/after/proof.json) records the same source passing all four cases and the complete 82-case authentication gate. The gate lists every required method and rejects failures, errors, skips, missing cases and duplicates.
