import '../testing/git-env.ts';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, it } from 'node:test';
import { selectBackend } from '../src/core/devices.ts';
import { resolveSpecs } from '../src/core/e2e.ts';
import { STANDARD, declaredIn, planGroups, readSpecText, settingsOf } from '../src/core/instances/settings.ts';
import type { BuildKey, InstanceSettings, ScratchPath, VerifyFailure } from '../src/core/types.ts';
import { host } from '../src/host.ts';
import { LANE_FAILED, LANE_READY, LOG_FILTER, RECORD_SIZE } from '../src/platform/android/emulator.ts';
import { localAndroidBackend } from '../src/platform/android/local.ts';
import { systemImage, type Machine } from '../src/platform/android/sdk.ts';
import { answer } from '../src/core/remote/recipe.ts';
import sessionDevice from '../src/platform/android/session-device.ts';

const PACKAGE_DIR = join(import.meta.dirname, '..');
const scratch = mkdtempSync(join(tmpdir(), 'verify-host-'));
const machine = (overrides: Partial<Machine> = {}): Machine => ({ os: 'darwin', arch: 'arm64', home: scratch, env: {}, kvm: join(scratch, 'kvm'), ...overrides });

describe('the clerk-android host build', () => {
  it('runs gradlew without the Clerk and agent keys of the environment, and with the rest of it', async () => {
    const work = mkdtempSync(join(tmpdir(), 'verify-host-build-'));
    const jdk = join(work, 'jdk');
    mkdirSync(join(jdk, 'bin'), { recursive: true });
    writeFileSync(join(jdk, 'release'), 'JAVA_VERSION="21.0.0"\n');
    writeFileSync(join(jdk, 'bin', 'java'), `#!/bin/sh\nenv > '${join(work, 'env')}'\necho "the stand-in for java ran: $*"\nexit 7\n`, { mode: 0o755 });
    const before = { ...process.env };
    const set = {
      CLERK_PLATFORM_API_KEY: 'ak_platform',
      CLERK_PLATFORM_API_KEY_FILE: '/home/x/key',
      VERIFY_PLATFORM_KEY_REFERENCE: 'op://vault/item',
      CLERK_SECRET_KEY: 'sk_test_instance',
      AI_GATEWAY_API_KEY: 'vck_gateway',
      AI_GATEWAY_API_KEY_FILE: '/home/x/gateway',
      VERIFY_HOST_BUILD_MARK: 'kept',
    };
    Object.assign(process.env, set, { JAVA_HOME: jdk });
    try {
      await assert.rejects(
        host.build('android', 'android-0000' as BuildKey, join(work, 'build') as ScratchPath, () => {}),
        (error: VerifyFailure) => error.code === 'BUILD_FAILED' && /^\.\/gradlew :e2e:assembleDebug exited 7:\n.*the stand-in for java ran: .* :e2e:assembleDebug --console=plain --quiet$/s.test(error.message),
      );
      const seen = readFileSync(join(work, 'env'), 'utf8').split('\n');
      assert.deepEqual(seen.filter((line) => Object.keys(set).includes(line.split('=')[0]!)), ['VERIFY_HOST_BUILD_MARK=kept']);
    } finally {
      for (const name of Object.keys(process.env)) if (!(name in before)) delete process.env[name];
      Object.assign(process.env, before);
      rmSync(work, { recursive: true, force: true });
    }
  });
});

describe('the clerk-android committed specs', () => {
  const MFA: InstanceSettings = { config: { auth_multi_factor: { required_for_sign_up: true } }, environment: { 'user_settings.sign_up.mfa.required': true } };
  const FORCED_ORG: InstanceSettings = { config: { organization_settings: { force_organization_selection: true } }, environment: { 'organization_settings.force_organization_selection': true } };
  const committed = resolveSpecs(PACKAGE_DIR, { all: true }).map((spec) => ({ spec, ...readSpecText(PACKAGE_DIR, spec.path) }));
  const declarations = committed.map(({ spec, ...text }) => ({ path: spec.path, declared: declaredIn(text, spec.path) }));
  const declaredBy = (file: string) => declarations.find(({ path }) => path.endsWith(`/session-tasks/${file}`))?.declared;

  it('declare forced organization selection or required MFA at sign-up and nothing else', () => {
    assert.deepEqual(declaredBy('choose-organization.e2e.ts'), FORCED_ORG);
    assert.deepEqual(declaredBy('setup-mfa.e2e.ts'), MFA);
    const known = [settingsOf(FORCED_ORG, 'x').key, settingsOf(MFA, 'x').key];
    for (const { path, declared } of declarations) {
      if (declared !== null) assert.ok(known.includes(settingsOf(declared, path).key), path);
    }
  });

  it('plan from the standard settings into three groups, standard first, each asked for by its own first spec', () => {
    const plan = planGroups(committed, STANDARD.key);
    assert.equal(plan.length, 3);
    assert.equal(plan[0]!.settings, STANDARD);
    assert.deepEqual(plan.flatMap((group) => group.specs.map((spec) => spec.path)).sort(), committed.map(({ spec }) => spec.path).sort(), 'every spec is in one group, once');
    for (const group of plan.slice(1)) assert.equal(group.settings.askedBy, group.specs[0]!.path);
  });
});

describe('the clerk-android host with a remote device', () => {
  it('tries the local emulator first and falls back to a runner, saying why', () => {
    assert.deepEqual(host.backends.map((b) => `${b.platform} ${b.kind}`), ['android local', 'android remote']);
    const runner = 'the device runs on a CI runner \\(ubuntu-24\\.04 unless --runner names another\\), started through verify-remote\\.yml on clerk\\/clerk-android$';
    const choose = (on: Machine) => selectBackend({ ...host, backends: [localAndroidBackend({ machine: on }), host.backends[1]!] }, 'android', undefined, null);
    const noKvm = choose(machine({ os: 'linux', arch: 'x64' }));
    assert.equal(noKvm.backend.kind, 'remote');
    assert.match(noKvm.why, new RegExp(`^local is out: there is no .*kvm, so this machine has no hardware virtualization for the emulator; ${runner}`));
    const noSdk = choose(machine());
    assert.match(noSdk.why, new RegExp(`^local is out: no Android SDK with an emulator and adb .*\\(to run it here: .*ANDROID_HOME.*\\); ${runner}`));
  });

  it('gives the session workflow the image and the boot files this code uses', () => {
    const workflow = readFileSync(join(PACKAGE_DIR, '..', '.github', 'workflows', 'verify-remote.yml'), 'utf8');
    assert.ok(workflow.includes(`IMAGE="${systemImage(machine({ os: 'linux', arch: 'x64' }))}"`), 'the runner installs the image the lane AVD names');
    for (const file of [LANE_READY, LANE_FAILED]) assert.ok(workflow.includes(`$VERIFY_SESSION_WORK/${file}`), file);
    assert.ok(workflow.includes('src/platform/android/session-lane.ts" boot "$VERIFY_SESSION_WORK"'));
  });

  it('has the session workflow start on the two runner labels this repository uses, and fail for any other', () => {
    const lines = readFileSync(join(PACKAGE_DIR, '..', '.github', 'workflows', 'verify-remote.yml'), 'utf8').split('\n').map((line) => line.trim());
    const first = lines.indexOf('case "$RUNNER_LABEL" in');
    const check = lines.slice(first, lines.indexOf('esac', first) + 1).join('\n');
    const checked = (label: string) => {
      const ran = spawnSync('bash', ['-c', check], { env: { PATH: process.env.PATH, RUNNER_LABEL: label }, encoding: 'utf8' });
      return { status: ran.status, stdout: ran.stdout };
    };
    assert.ok(first > 0, 'the label check is in the workflow');
    for (const label of ['ubuntu-24.04', 'ubuntu-latest']) assert.deepEqual(checked(label), { status: 0, stdout: '' }, label);
    assert.deepEqual(checked('macos-15-xlarge'), { status: 1, stdout: '::error title=verify-remote::no session started: the runner label macos-15-xlarge is not ubuntu-24.04 or ubuntu-latest\n' });
  });

  describe('in a session', () => {
    const device = sessionDevice({ id: 'emulator-5560', platform: 'android' });
    const line = (command: { command: string; args: readonly string[] }) => `${command.command} ${command.args.join(' ')}`;

    it('builds or takes the stored e2e APK, waits for the emulator, then installs the APK the build step handed over', () => {
      const steps = device.build('/work');
      assert.deepEqual(steps.map((step) => step.command), [process.execPath, 'sh', 'sh']);
      assert.deepEqual(steps[0]!.args, [join(PACKAGE_DIR, 'src', 'platform', 'android', 'session-build.ts'), 'build', '/work']);
      assert.equal(steps[0]!.cwd, `${join(PACKAGE_DIR, '..')}/`);
      assert.equal(steps[1]!.args.at(-1), '/work');
      assert.deepEqual(steps[2]!.args.slice(2), ['emulator-5560', '/work/e2e-debug.apk', 'com.clerk.e2e']);
    });

    function installed(answers: readonly string[]): { readonly status: number | null; readonly calls: readonly string[] } {
      const work = mkdtempSync(join(tmpdir(), 'verify-host-install-'));
      mkdirSync(join(work, 'bin'));
      writeFileSync(join(work, 'answers'), `${answers.join('\n')}\n`);
      writeFileSync(join(work, 'calls'), '');
      writeFileSync(join(work, 'bin', 'adb'), `#!/bin/sh\necho "$*" >> "${work}/calls"\nanswer=$(sed -n "$(wc -l < "${work}/calls" | tr -d ' ')p" "${work}/answers")\necho "$answer"\n[ "$answer" = Success ]\n`, { mode: 0o755 });
      const install = device.build('/work')[2]!;
      const ran = spawnSync(install.command, [...install.args], { env: { PATH: `${join(work, 'bin')}:${process.env.PATH}` }, encoding: 'utf8' });
      const calls = readFileSync(join(work, 'calls'), 'utf8').split('\n').filter((call) => call !== '');
      rmSync(work, { recursive: true, force: true });
      return { status: ran.status, calls };
    }

    it('replaces the app in place when it can', () => {
      assert.deepEqual(installed(['Success']), { status: 0, calls: ['-s emulator-5560 install -r -t /work/e2e-debug.apk'] });
    });

    it('uninstalls an app that another machine signed, such as a stored APK, then installs the new build', () => {
      const refused = 'adb: failed to install /work/e2e-debug.apk: Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.clerk.e2e signatures do not match newer version; ignoring!]';
      assert.deepEqual(installed([refused, 'Success', 'Success']), {
        status: 0,
        calls: ['-s emulator-5560 install -r -t /work/e2e-debug.apk', '-s emulator-5560 uninstall com.clerk.e2e', '-s emulator-5560 install -t /work/e2e-debug.apk'],
      });
    });

    it('fails, and uninstalls nothing, when the install fails for another reason', () => {
      assert.deepEqual(installed(['adb: failed to install: Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]']), { status: 1, calls: ['-s emulator-5560 install -r -t /work/e2e-debug.apk'] });
    });

    it('lets a patch change exactly the paths the CLI sends from the tree', () => {
      assert.deepEqual(answer(device, { op: 'inputs' }), host.buildInputs('android', 'remote'));
      assert.deepEqual(device.inputs, ['source', 'e2e', 'gradle', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew']);
    });

    it('records to a file on the emulator, stops screenrecord there, then pulls the file', () => {
      const record = device.record;
      assert.equal(line(record.start('/work/recording.mp4')), `adb -s emulator-5560 shell screenrecord --size ${RECORD_SIZE} --time-limit 0 /data/local/tmp/verify-session.mp4`);
      assert.match(line(record.stop!('/work/recording.mp4')[0]!), /^adb -s emulator-5560 shell pkill -INT screenrecord; /);
      assert.deepEqual(record.collect!('/work/recording.mp4').map(line), ['adb -s emulator-5560 pull /data/local/tmp/verify-session.mp4 /work/recording.mp4', 'adb -s emulator-5560 shell rm -f /data/local/tmp/verify-session.mp4']);
    });

    it('reads the same logcat tags as the local lane', () => {
      assert.equal(line(device.logs(new Date(1_791_014_000_123), 'Expo:V')), `adb -s emulator-5560 logcat -d -v threadtime -T 1791014000.123 ${[...LOG_FILTER.slice(0, -1), 'Expo:V', '*:S'].join(' ')}`);
    });
  });
});
