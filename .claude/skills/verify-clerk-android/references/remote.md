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

- Node 24, at 24.8.0 or newer. On any other Node the CLI reruns itself under `node@24` through `npx` and says so.
- A pushed branch that holds `.github/workflows/verify-remote.yml`.
- A GitHub token that may dispatch that workflow. The CLI takes the token from `GH_TOKEN`, then `GITHUB_TOKEN`, then `gh auth token`.
- Network access to `*.trycloudflare.com`, `api.clerk.com`, and `*.clerk.accounts.dev`.
- The Platform API credential, as for a local lane.

It needs no Android SDK, no JDK, and no `adb`.

REST starts, reads, and ends sessions. `git push` gets a commit you make to GitHub so the session can build it. Without push access you can still verify any commit GitHub already has.

Node ignores `HTTPS_PROXY` unless told to honor it, so the CLI decides: it goes through the proxy when the direct path to GitHub does not work, and reruns itself with the proxy in effect for itself, e2e, and agent-device. `doctor`'s `remote-env` line says which path it took and why.

## Builds

A session builds a pushed commit, never your working tree. The runner builds the `:e2e` APK while it downloads the system image and boots the emulator, and the cold Gradle build is the long part. Run `up` as soon as you have pushed, then write the spec while it builds. While a runner is not ready, `up` prints `wait run <id> has waited <n>s, now for <what>`, which names the label it is waiting on.

`up` starts the session's run on your branch as GitHub has it. The run builds only the commit it was started on, or a later commit of that branch. When HEAD is not on that branch on GitHub, or is behind it, the run fails at the step `Read the request` and no session starts. Push or pull, then run `up` again.

After an edit to the app, commit and push, then `run` again. `run` sees that the app sources changed, asks the same session to build the new commit, and runs once it is installed. The session builds it only when GitHub shows it as a later commit of the branch the session was started on. After a rebase or an amend, the pushed commit is not a later commit of that branch, and the build fails with `BUILD_FAILED`. Run `down`, then `up`. That build is incremental. Uncommitted changes to the app sources, or a HEAD that GitHub does not have, fail with `BUILD_FAILED` and the fix `git commit` or `git push`. A commit that only touches specs or docs needs no push, because specs run from this machine.

A session keeps the package's code of the commit it started on. The build steps in `src/platform/android/session-device.ts` are read again from each commit it builds. The session agent in `src/core/` and the workflow steps are not: after a push that changes a file that `src/core/MANIFEST` lists, `up` and `run` fail with `NOT_READY` and the fix `down`, then `up`.

## Cost and lifetime

A session holds a CI runner until it stops, and on a Blacksmith label it is billed by the minute. It stops itself after 15 minutes without a call from the CLI, and always after 60 minutes. The CLI ends a session that is not ready 40 minutes after it asked for the build, and `up` then fails with `NOT_READY`. `down` stops it at once. After an idle stop, the next `up` or `run` prints `lost ... renewing` and starts a new session. A session with under two minutes left before its cap is replaced the same way. Set `VERIFY_REMOTE_IDLE_MINUTES` or `VERIFY_REMOTE_CAP_MINUTES` before `up` to change either.

Each checkout has a random id, and its sessions carry it. When the checkout holds no lease, `up` and `run` end any session with that id that is still running, and `down --stale` does so at any time. The idle stop ends whatever they miss, such as the session of a checkout that was deleted.

## Evidence from a machine that cannot attach

`attach` always tries `gh pr edit --attach` itself first, on every kind of machine, and hands off only when that cannot work. The hand-off sends the evidence of a remote run to the session's runner. It sends the video and the screenshots from the sealed run directory, with a manifest that names the run, the pull request, the device, the commit, and each file's size and SHA-256. The files travel over the session's tunnel, with the session's token, in pieces of 1 MiB. The session job keeps them in one directory and uploads that directory as the artifact `verify-evidence` when the job ends. The artifact holds nothing else of the session.

`.github/workflows/verify-attach.yml` then runs, from the default branch. It downloads the artifact, checks every file against the manifest and against its own limits, and puts the block in the description with `gh pr edit --attach`. It never runs anything from the branch or from the artifact. The workflow did not run the tests, so the line it writes says that the session reported the result, links the session's run in the Actions tab, and names the account that started that run, as GitHub records it.

The repository needs this set up once:

1. `verify-remote.yml`, `verify-attach.yml`, and `.github/scripts/verify-attach.mjs` are on the default branch. GitHub starts `verify-attach.yml` only from there, so until then a hand-off succeeds and nothing is published.
2. A machine account has write access to the repository.
3. That account has a fine-grained personal access token that is limited to this repository and has the permission "Pull requests: read and write".
4. The repository has an environment named `verify-evidence`, and its deployment branches are limited to the default branch.
5. The token is the secret `VERIFY_EVIDENCE_TOKEN` of that environment. The repository has no repository secret of that name.

Do not store the token as a repository secret. Anyone who can push a branch can add a workflow to it, and a workflow on any branch can read a repository secret. A secret of an environment that allows only the default branch reaches only jobs that run on the default branch, and the job of `verify-attach.yml` is one.

Without the environment or its secret, the workflow prints a notice and publishes nothing.

The workflow publishes only when all of these hold. Otherwise it prints the reason in its run and publishes nothing.

- The pull request that `--pr` names is open, and its branch is in this repository, not in a fork.
- Its branch is the branch the session was started on.
- The commit the run was made at is one of the pull request's commits.
- The commit that branch had when `up` started the session is one of them too. GitHub lists the first 250 commits of a pull request, and the workflow reads no further. When a longer pull request has either commit past the first 250, the workflow says so and publishes nothing. Squash or rebase the branch to 250 commits or fewer, or attach from a machine whose `gh` can attach.
- The hand-off has at least one file.
- The description has the two evidence comments of the platform once each, or neither.
- A description without the comments does not end inside a code fence that is never closed.

`attach` checks the same rules before it sends anything, and fails with the reason.

A push after the run does not lose the evidence. The line in the block names the commit the run was made at, and when the pull request has moved on it ends with "The pull request has newer commits." To show the newer commit, `run` again and `attach` again in the same session, which replaces the block. The session builds the new commit, and no new session is needed. After a rewrite of the branch's history that removes either commit, `down` and `up` before the next `run`.

A hand-off holds one video of up to 100 MiB and screenshots of up to 10 MiB each, with at most 50 files and 150 MiB in all. A session keeps one hand-off, and a later `attach` in the same session replaces it. `down` gives a session that holds evidence up to five minutes to upload it before it cancels the run.

The runs of `verify-attach` for one branch take turns, and each one publishes.

The workflow reads the description again just before it writes. An edit that someone saves after that read, and before `gh` has uploaded the files and written the description, is overwritten. That is a few seconds for screenshots and up to a few minutes for a large video. `attach` on a machine whose `gh` can attach has the same window.

If the description has no evidence a few minutes after `down`, read the newest run of `verify-attach` in the Actions tab of the repository. It says what it refused and why.

## Runner labels

The default label is `ubuntu-24.04`, a GitHub-hosted runner that costs nothing for a public repository. It names the Ubuntu version, so a new `ubuntu-latest` image cannot change the machine under the emulator. The session's 60 minutes start when the runner starts, so about 50 are left after `up`.

`--runner blacksmith-4vcpu-ubuntu-2404` uses a Blacksmith runner instead, which is billed by the minute. Any Linux x64 label with KVM, the Android SDK command-line tools, and `sudo` works. A held session keeps its label, and `--runner` with another label fails until `down`.

Before the session's runner starts, a job of a few seconds reads the request. That job runs on `ubuntu-latest`, a free GitHub-hosted label, whatever the session's label.

The session boots `Clerk_Verify_Pixel` through the same code as a local lane, on the `x86_64` build of the same Android 36 image, so the panel, the locale, and the lane mark match. It is the only emulator on that machine.

## Secrets

The Platform API key and the instance's secret key never leave this machine, and in a cloud environment the Platform API key never enters it. The session's bearer token is made here, lives in `.verify/remote/<session>/token`, and is deleted by `down`. GitHub sees only the token's SHA-256. The runner sees sign-in tickets and publishable keys as launch arguments. It uploads one artifact, which holds only the files that `attach` handed off, and it uploads no log and no build. The tunnel ends TLS at Cloudflare, so Cloudflare can read what crosses it, and the tunnel's host name is public while the session lives. Every route on it needs the bearer.
