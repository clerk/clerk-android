# The emulator on a CI runner

When this machine cannot run the emulator, the CLI borrows one on a GitHub Actions runner and drives it through a tunnel. Every verb, spec, and evidence file is the same. `SKILL.md` has the loop: commit, push, `up`, `run`, `down`.

## How the CLI chooses

`up`, `run`, and `doctor` print the choice and the reason on a `backend` line. This machine runs the emulator itself when all of these hold:

- It runs macOS or Linux.
- On Linux, `/dev/kvm` exists and this user can open it for reading and writing. A machine with no `/dev/kvm` is told so first and offered no fix, because nothing installed on it would help.
- It has an Android SDK with `emulator/emulator` and `platform-tools/adb`. The CLI looks in `ANDROID_HOME`, then `ANDROID_SDK_ROOT`, then `~/Library/Android/sdk` on macOS or `~/Android/Sdk` on Linux, then any SDK whose `platform-tools` or `emulator` directory is on `PATH`.
- That SDK has the system image `system-images;android-36;google_apis;arm64-v8a` on an ARM machine or `system-images;android-36;google_apis;x86_64` on an Intel or AMD one. When a `Clerk_Verify_Pixel` AVD already exists, the image that AVD names counts instead.

The check reads files and runs no tool. When something is missing, the `backend` line names it and, where a command fixes it, prints the command after `to run it here:`. A missing lane AVD or a missing Java 21 does not send you to a runner, because each has a quick fix on this machine. On Linux there is no bundled JBR, so set `JAVA_HOME` to a Java 21 JDK.

`--backend local` or `--backend remote` forces one, and fails with `UNSUPPORTED` and a fix when this machine cannot use it. A worktree that holds a lease keeps that lease's backend until `down`.

## What a session needs from this machine

- Node 24. On an older Node the CLI reruns itself under `node@24` through `npx` and says so.
- A pushed branch that holds `.github/workflows/verify-remote.yml`.
- A GitHub token that may dispatch that workflow, or push access for a branch named `verify-remote/*`. The CLI takes the token from `GH_TOKEN`, then `GITHUB_TOKEN`, then `gh auth token`.
- Network access to `*.trycloudflare.com`, `api.clerk.com`, `api.clerk.dev`, and `*.clerk.accounts.dev`.
- The Platform API credential, as for a local lane.

It needs no Android SDK, no JDK, and no `adb`.

REST starts, reads, and ends sessions. `git push` gets a commit you make to GitHub so the session can build it. Without push access you can still verify any commit GitHub already has.

Node ignores `HTTPS_PROXY` unless told to honor it, so the CLI decides: it goes through the proxy when the direct path to GitHub does not work, and reruns itself with the proxy in effect for itself, e2e, and agent-device. `doctor`'s `remote-env` line says which path it took and why.

## Builds

A session builds a pushed commit, never your working tree. The runner builds the `:e2e` APK while it downloads the system image and boots the emulator, and the cold Gradle build is the long part. Run `up` as soon as you have pushed, then write the spec while it builds. While a runner is not ready, `up` prints `wait run <id> has waited <n>s, now for <what>`, which names the label it is waiting on.

After an edit to the app, commit and push, then `run` again. `run` sees that the app sources changed, asks the same session to build the new commit, and runs once it is installed. That build is incremental. Uncommitted changes to the app sources, or a HEAD that GitHub does not have, fail with `BUILD_FAILED` and the fix `git commit` or `git push`. A commit that only touches specs or docs needs no push, because specs run from this machine.

A session keeps the skill's code of the commit it started on. The build steps in `src/platform/android/session-device.ts` are read again from each commit it builds. The session agent in `src/core/` and the workflow steps are not: after a push that changes `src/core/`, `up` and `run` fail with `NOT_READY` and the fix `down`, then `up`.

## Cost and lifetime

A session is billed by the minute. It stops itself after 15 minutes without a call from the CLI, and always after 60 minutes. `down` stops it at once. After an idle stop, the next `up` or `run` prints `lost ... renewing` and starts a new session. A session with under two minutes left before its cap is replaced the same way. Set `VERIFY_REMOTE_IDLE_MINUTES` or `VERIFY_REMOTE_CAP_MINUTES` before `up` to change either.

Each checkout has a random id, and its sessions carry it. When the checkout holds no lease, `up` and `run` end any session with that id that is still running, and `down --stale` does so at any time. The idle stop ends whatever they miss, such as the session of a checkout that was deleted.

## Runner labels

The default label is `blacksmith-4vcpu-ubuntu-2404`, which is billed. `--runner ubuntu-latest` or `VERIFY_REMOTE_RUNNER=ubuntu-latest` uses a free GitHub-hosted label instead, with a slower cold build. Any Linux x64 label with KVM, the Android SDK command-line tools, and `sudo` works. A held session keeps its label, and `--runner` with another label fails until `down`.

Before the session's runner starts, a job of a few seconds reads the request. For a session on a Blacksmith label it runs on `blacksmith-2vcpu-ubuntu-2404`, so the session does not first wait in the queue for GitHub's own runners. For a session on a GitHub-hosted label it runs on `ubuntu-latest`. `VERIFY_REMOTE_PLAN_RUNNER=<label>` names another label.

The session boots `Clerk_Verify_Pixel` through the same code as a local lane, on the `x86_64` build of the same Android 36 image, so the panel, the locale, and the lane mark match. It is the only emulator on that machine.

## Secrets

The Platform API key and the instance's secret key never leave this machine, and in a cloud environment the Platform API key never enters it. The session's bearer token is made here, lives in `.verify/remote/<session>/token`, and is deleted by `down`. Never print it. GitHub sees only the token's SHA-256. The runner sees sign-in tickets and publishable keys as launch arguments and uploads no artifact. The tunnel ends TLS at Cloudflare, so Cloudflare can read what crosses it, and the tunnel's host name is public while the session lives. Every route on it needs the bearer.
