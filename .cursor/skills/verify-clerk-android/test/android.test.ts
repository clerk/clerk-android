import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, it } from 'node:test';
import { isRunning, run } from '../src/core/exec.ts';
import { encodeLaunchArguments } from '../src/core/state.ts';
import type { EvidencePath, LaunchId, LocalLease, PublishableKey, RunId, StorageScope } from '../src/core/types.ts';
import { takeSlot } from '../src/core/claims.ts';
import { LOG_FILTER, RECORD_SIZE, lanePort, localAndroidBackend, logFilter, laneSerial, logcatSince, parseAdbDevices, startScreenrecord } from '../src/platform/android/local.ts';
import { jdkCheck, resolveJavaHome } from '../src/platform/android/sdk.ts';

function fakeJdk(root: string, version: string): string {
  const home = join(root, `jdk-${version}`);
  mkdirSync(home, { recursive: true });
  writeFileSync(join(home, 'release'), `IMPLEMENTOR="test"\nJAVA_VERSION="${version}"\n`);
  return home;
}

describe('android lanes', () => {
  it('puts slot 1 on emulator-5560 and slot 2 on emulator-5562', () => {
    assert.equal(lanePort(1), 5560);
    assert.equal(laneSerial(2), 'emulator-5562');
  });

  it('reads adb devices, skipping the header and blank lines', () => {
    const devices = parseAdbDevices('List of devices attached\nemulator-5560\tdevice\nemulator-5562\toffline\n\n');
    assert.deepEqual([...devices], [['emulator-5560', 'device'], ['emulator-5562', 'offline']]);
  });

  it('asks logcat for lines since the run started, in epoch seconds', () => {
    assert.equal(logcatSince(new Date(1_791_014_000_123)), '1791014000.123');
  });

  it('keeps the host, SDK, network, and React Native console tags and silences the rest', () => {
    assert.deepEqual(LOG_FILTER.slice(-1), ['*:S']);
    for (const tag of ['ClerkVerify:V', 'ClerkLog:V', 'OkHttp:V', 'ReactNativeJS:V']) assert.ok(LOG_FILTER.includes(tag), tag);
  });

  it('adds a host log predicate before the final silence spec', () => {
    assert.deepEqual(logFilter('Expo:V  ReactNative:W').slice(-3), ['Expo:V', 'ReactNative:W', '*:S']);
    assert.deepEqual(logFilter(), LOG_FILTER);
  });

  it('encodes launch inputs as am start string extras', () => {
    const args = encodeLaunchArguments('android', {
      verifyPublishableKey: 'pk_test_abc' as PublishableKey,
      verifyRunId: 'r20261003-040814-846e' as RunId,
      verifyStorageScope: 'scope1' as StorageScope,
      verifyLaunchId: 'launch1' as LaunchId,
      verifyScreen: 'userProfile',
    });
    assert.deepEqual(args.slice(0, 3), ['--es', 'verifyPublishableKey', 'pk_test_abc']);
    assert.deepEqual(args.slice(-3), ['--es', 'verifyScreen', 'userProfile']);
  });
});

describe('android JDK', () => {
  const root = mkdtempSync(join(tmpdir(), 'verify-jdk-'));
  const jbr = fakeJdk(root, '21.0.8');
  const java17 = fakeJdk(root, '17.0.17');

  it('fails JAVA_HOME on Java 17 with a fix that names the Java 21 JBR', () => {
    const check = jdkCheck({ JAVA_HOME: java17 }, jbr);
    assert.equal(check.ok, false);
    assert.match(check.detail, /Java 17/);
    assert.equal(check.fix, `export JAVA_HOME="${jbr}"`);
  });

  it('builds with the JBR when JAVA_HOME is unset, and with JAVA_HOME when it is 21', () => {
    assert.deepEqual(resolveJavaHome({}, jbr), { ok: true, home: jbr, detail: 'Java 21 from the Android Studio JBR' });
    const java21 = fakeJdk(root, '21.0.2');
    const chosen = resolveJavaHome({ JAVA_HOME: java21 }, join(root, 'missing'));
    assert.equal(chosen.ok && chosen.home, java21);
  });

  it('fails with an install fix when there is no JBR and no JAVA_HOME', () => {
    const check = jdkCheck({}, join(root, 'missing'));
    assert.equal(check.ok, false);
    assert.match(check.fix ?? '', /install Android Studio/);
  });
});

describe('android screenrecord', () => {
  it('stops on the device, waits for pidof to empty, and only then pulls', async () => {
    const dir = mkdtempSync(join(tmpdir(), 'verify-adb-'));
    const log = join(dir, 'adb.log');
    const adb = join(dir, 'adb');
    writeFileSync(
      adb,
      [
        '#!/bin/bash',
        `echo "$*" >> ${log}`,
        `state=${dir}/recording`,
        'case "$*" in',
        '  *"shell screenrecord"*) touch "$state"; while [ -f "$state" ]; do sleep 0.1; done ;;',
        '  *"shell pidof screenrecord"*) [ -f "$state" ] && echo 4242 ;;',
        '  *"shell pkill -INT screenrecord"*) rm -f "$state" ;;',
        '  *" pull "*) echo mp4 > "${@: -1}" ;;',
        'esac',
        '',
      ].join('\n'),
    );
    chmodSync(adb, 0o755);
    const into = join(dir, 'r20261003-040814-846e') as EvidencePath;
    mkdirSync(into);

    const recording = await startScreenrecord({ adbBin: adb, serial: 'emulator-5560', into, exec: run });
    const video = await recording.stop();

    assert.equal(video, join(into, 'video.mp4'));
    assert.ok(existsSync(video));
    const calls = readFileSync(log, 'utf8').trim().split('\n');
    const record = calls.findIndex((c) => c.includes('shell screenrecord'));
    const kill = calls.findIndex((c) => c.includes('pkill -INT screenrecord'));
    const pull = calls.findIndex((c) => c.includes(' pull '));
    const lastPidof = calls.findLastIndex((c) => c.includes('pidof screenrecord'));
    assert.match(calls[record]!, new RegExp(`--size ${RECORD_SIZE} --time-limit 0 /sdcard/verify-r20261003-040814-846e\\.mp4`));
    assert.ok(record < kill && kill < lastPidof && lastPidof < pull, calls.join('\n'));
    assert.ok(calls.some((c) => c.includes('rm -f /sdcard/verify-r20261003-040814-846e.mp4')), 'removes the device copy');
  });

  it('fails stop with adb\'s error when the pull fails', async () => {
    const dir = mkdtempSync(join(tmpdir(), 'verify-adb-'));
    const adb = fakeTool(dir, 'adb', [
      `state=${dir}/recording`,
      'case "$*" in',
      '  *"shell screenrecord"*) touch "$state"; while [ -f "$state" ]; do sleep 0.1; done ;;',
      '  *"shell pidof screenrecord"*) [ -f "$state" ] && echo 4242 ;;',
      '  *"shell pkill -INT screenrecord"*) rm -f "$state" ;;',
      '  *" pull "*) echo "adb: error: remote object does not exist" >&2; exit 1 ;;',
      'esac',
    ]);
    const into = join(dir, 'r20261003-040814-846e') as EvidencePath;
    mkdirSync(into);
    const recording = await startScreenrecord({ adbBin: adb, serial: 'emulator-5560', into, exec: run });
    await assert.rejects(recording.stop(), { code: 'NOT_READY', message: /remote object does not exist/ });
  });
});

function fakeTool(dir: string, name: string, body: readonly string[]): string {
  const path = join(dir, name);
  writeFileSync(path, ['#!/bin/bash', `echo "$*" >> ${join(dir, 'calls.log')}`, ...body, ''].join('\n'));
  chmodSync(path, 0o755);
  return path;
}

describe('android lane ownership', () => {
  function setup(avd: string, laneProperty: (ownNonce: string) => string) {
    const dir = mkdtempSync(join(tmpdir(), 'verify-lane-'));
    const claimsDir = join(dir, 'claims');
    const claim = takeSlot(claimsDir, 'android', 1, 0, join(dir, 'worktree'))!;
    const killed = join(dir, 'killed');
    const adbBin = fakeTool(dir, 'adb', [
      'case "$*" in',
      `  "devices") echo "List of devices attached"; [ -f ${killed} ] || printf "emulator-5560\\tdevice\\n" ;;`,
      `  *"emu avd name"*) printf "${avd}\\nOK\\n" ;;`,
      `  *"getprop debug.verify.lane"*) echo "${laneProperty(claim.nonce)}" ;;`,
      '  *"getprop sys.boot_completed"*) echo 1 ;;',
      `  *"emu kill"*) touch ${killed} ;;`,
      'esac',
    ]);
    const emulatorBin = fakeTool(dir, 'emulator', ['echo Clerk_Verify_Pixel']);
    const lease: LocalLease = { backend: 'local', platform: 'android', slot: 1, deviceName: 'verify-android-1', deviceId: 'emulator-5560', claimNonce: claim.nonce, acquiredAt: '', installedBuild: null };
    const calls = () => readFileSync(join(dir, 'calls.log'), 'utf8');
    return { backend: localAndroidBackend({ claimsDir, adbBin, emulatorBin }), lease, dir, calls };
  }

  it('never kills another AVD that sits on a lane port, and frees the claim', async () => {
    const { backend, lease, calls } = setup('Pixel_9_Pro', () => '');
    await backend.release(lease);
    assert.doesNotMatch(calls(), /emu kill/);
    assert.equal(await backend.check(lease), 'lost');
  });

  it('never kills a Clerk_Verify_Pixel emulator booted for another claim', async () => {
    const { backend, lease, calls } = setup('Clerk_Verify_Pixel', () => 'someone-elses-claim');
    await backend.release(lease);
    assert.doesNotMatch(calls(), /emu kill/);
  });

  it('kills the lane it booted', async () => {
    const { backend, lease, calls } = setup('Clerk_Verify_Pixel', (own) => own);
    assert.equal(await backend.check(lease), 'held');
    await backend.release(lease);
    assert.match(calls(), /-s emulator-5560 emu kill/);
  });

  it('doctor flags a foreign emulator on a lane port with a kill command for its owner', async () => {
    const { backend } = setup('Pixel_9_Pro', () => '');
    const lanePorts = (await backend.doctorChecks()).device.find((c) => c.id === 'lane-ports');
    assert.equal(lanePorts?.ok, false);
    assert.match(lanePorts?.detail ?? '', /emulator-5560 \(Pixel_9_Pro, not a verify lane\)/);
    assert.match(lanePorts?.fix ?? '', /^adb -s emulator-5560 emu kill, but only if that emulator is yours/);
  });

  it('doctor passes a lane port that holds this claim\'s own lane', async () => {
    const { backend } = setup('Clerk_Verify_Pixel', (own) => own);
    assert.equal((await backend.doctorChecks()).device.find((c) => c.id === 'lane-ports')?.ok, true);
  });

  it('reports a foreign emulator on a lane port in POOL_FULL instead of claiming it', async () => {
    const { backend, lease, dir } = setup('Pixel_9_Pro', () => '');
    await backend.release(lease);
    takeSlot(join(dir, 'claims'), 'android', 2, 0, join(dir, 'other'));
    await assert.rejects(backend.acquire({ platform: 'android', worktree: join(dir, 'worktree'), waitSeconds: 0, retryWith: 'bin/verify up --wait <seconds>', progress: () => undefined }), {
      code: 'POOL_FULL',
      message: /emulator-5560 \(Pixel_9_Pro, not a verify lane\), verify-android-2 \(held by .*other\)/,
      fix: /`adb -s emulator-5560 emu kill` frees a lane, but only if that emulator is yours/,
    });
  });
});

describe('android lanes verify spawned', () => {
  function fakeEmulatorProcess(port: number): { readonly pid: number; readonly startedAt: number } {
    const child = spawn('bash', ['-c', `exec -a "qemu-system-aarch64-headless -avd Clerk_Verify_Pixel -read-only -port ${port}" sleep 60`], { detached: true, stdio: 'ignore' });
    child.unref();
    return { pid: child.pid!, startedAt: Date.now() };
  }

  async function setup(pidRecord: (own: string) => object | null) {
    const dir = mkdtempSync(join(tmpdir(), 'verify-spawned-'));
    const emulatorsDir = join(dir, 'emulators');
    mkdirSync(emulatorsDir);
    const claimsDir = join(dir, 'claims');
    const worktree = join(dir, 'worktree');
    const claim = takeSlot(claimsDir, 'android', 1, 0, worktree)!;
    const adbBin = fakeTool(dir, 'adb', ['case "$*" in', '  "devices") echo "List of devices attached" ;;', 'esac']);
    const emulatorBin = fakeTool(dir, 'emulator', ['echo Clerk_Verify_Pixel']);
    const emulator = fakeEmulatorProcess(5560);
    await new Promise((resolve) => setTimeout(resolve, 300));
    const record = pidRecord(claim.nonce);
    if (record !== null) writeFileSync(join(emulatorsDir, 'android-1.pid'), JSON.stringify({ ...emulator, ...record }));
    const backend = localAndroidBackend({ claimsDir, adbBin, emulatorBin, emulatorsDir });
    return { backend, emulator, emulatorsDir, worktree };
  }

  it('reclaims an interrupted boot that left its pid file, through the next up\'s reap', async () => {
    const { backend, emulator, emulatorsDir, worktree } = await setup((own) => ({ nonce: own }));
    assert.ok(isRunning(emulator));
    const [stale] = await backend.reapable(worktree);
    await backend.release(stale!);
    assert.equal(isRunning(emulator), false, 'the emulator verify spawned is gone');
    assert.equal(existsSync(join(emulatorsDir, 'android-1.pid')), false, 'the pid file is removed');
  });

  it('never kills a hand-booted Clerk_Verify_Pixel with no pid file', async () => {
    const { backend, emulator, worktree } = await setup(() => null);
    const [stale] = await backend.reapable(worktree);
    await backend.release(stale!);
    assert.ok(isRunning(emulator));
    process.kill(-emulator.pid, 'SIGKILL');
  });

  it('never kills a Clerk_Verify_Pixel whose pid file names another claim', async () => {
    const { backend, emulator, worktree } = await setup(() => ({ nonce: 'another-claim' }));
    const [stale] = await backend.reapable(worktree);
    await backend.release(stale!);
    assert.ok(isRunning(emulator));
    process.kill(-emulator.pid, 'SIGKILL');
  });

  it('doctor does not flag a claimed lane that is still booting, before its marker is set', async () => {
    const dir = mkdtempSync(join(tmpdir(), 'verify-booting-'));
    const emulatorsDir = join(dir, 'emulators');
    mkdirSync(emulatorsDir);
    const claim = takeSlot(join(dir, 'claims'), 'android', 1, 0, join(dir, 'worktree'))!;
    const adbBin = fakeTool(dir, 'adb', [
      'case "$*" in',
      '  "devices") printf "List of devices attached\\nemulator-5560\\toffline\\n" ;;',
      '  *"emu avd name"*) printf "Clerk_Verify_Pixel\\nOK\\n" ;;',
      'esac',
    ]);
    const emulator = fakeEmulatorProcess(5560);
    await new Promise((resolve) => setTimeout(resolve, 300));
    writeFileSync(join(emulatorsDir, 'android-1.pid'), JSON.stringify({ ...emulator, nonce: claim.nonce }));
    const backend = localAndroidBackend({ claimsDir: join(dir, 'claims'), adbBin, emulatorBin: fakeTool(dir, 'emulator', ['echo Clerk_Verify_Pixel']), emulatorsDir });
    assert.equal((await backend.doctorChecks()).device.find((c) => c.id === 'lane-ports')?.ok, true);
    process.kill(-emulator.pid, 'SIGKILL');
  });

  it('never kills a reused pid that is no longer the emulator it recorded', async () => {
    const { backend, emulator, worktree } = await setup((own) => ({ nonce: own, startedAt: Date.now() - 600_000 }));
    const [stale] = await backend.reapable(worktree);
    await backend.release(stale!);
    assert.ok(isRunning(emulator));
    process.kill(-emulator.pid, 'SIGKILL');
  });
});
