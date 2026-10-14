import '../testing/git-env.ts';
import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, it } from 'node:test';
import { selectBackend } from '../src/core/devices.ts';
import { resolveSpecs } from '../src/core/e2e.ts';
import { STANDARD, declaredIn, planGroups, readSpecText, settingsOf } from '../src/core/instances/settings.ts';
import type { InstanceSettings } from '../src/core/types.ts';
import { host } from '../src/host.ts';
import { LANE_FAILED, LANE_READY, LOG_FILTER, RECORD_SIZE } from '../src/platform/android/emulator.ts';
import { localAndroidBackend } from '../src/platform/android/local.ts';
import { systemImage, type Machine } from '../src/platform/android/sdk.ts';
import sessionDevice from '../src/platform/android/session-device.ts';

const PACKAGE_DIR = join(import.meta.dirname, '..');
const scratch = mkdtempSync(join(tmpdir(), 'verify-host-'));
const machine = (overrides: Partial<Machine> = {}): Machine => ({ os: 'darwin', arch: 'arm64', home: scratch, env: {}, kvm: join(scratch, 'kvm'), ...overrides });

describe('the clerk-android golden specs', () => {
  const MFA: InstanceSettings = { config: { auth_multi_factor: { required_for_sign_up: true } }, environment: { 'user_settings.sign_up.mfa.required': true } };
  const FORCED_ORG: InstanceSettings = { config: { organization_settings: { force_organization_selection: true } }, environment: { 'organization_settings.force_organization_selection': true } };
  const golden = resolveSpecs(PACKAGE_DIR, { all: true }).map((spec) => ({ spec, ...readSpecText(PACKAGE_DIR, spec.path) }));
  const declarations = golden.map(({ spec, ...text }) => ({ path: spec.path, declared: declaredIn(text, spec.path) }));
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
    const plan = planGroups(golden, STANDARD.key);
    assert.equal(plan.length, 3);
    assert.equal(plan[0]!.settings, STANDARD);
    assert.deepEqual(plan.flatMap((group) => group.specs.map((spec) => spec.path)).sort(), golden.map(({ spec }) => spec.path).sort(), 'every spec is in one group, once');
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

  describe('in a session', () => {
    const device = sessionDevice({ id: 'emulator-5560', platform: 'android' });
    const line = (command: { command: string; args: readonly string[] }) => `${command.command} ${command.args.join(' ')}`;

    it('assembles the e2e host, waits for the emulator, then installs the debug APK it built', () => {
      const steps = device.build('/work');
      assert.deepEqual(steps.map((step) => step.command), ['./gradlew', 'sh', 'adb']);
      assert.deepEqual(steps[0]!.args, [':e2e:assembleDebug', '--console=plain', '--quiet']);
      assert.equal(steps[1]!.args.at(-1), '/work');
      assert.equal(line(steps[2]!), 'adb -s emulator-5560 install -r -t e2e/build/outputs/apk/debug/e2e-debug.apk');
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
