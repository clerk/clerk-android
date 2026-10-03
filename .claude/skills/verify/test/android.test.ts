import assert from 'node:assert/strict';
import { chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, it } from 'node:test';
import { run } from '../src/core/exec.ts';
import { encodeLaunchArguments } from '../src/core/state.ts';
import type { EvidencePath, LaunchId, PublishableKey, RunId, StorageScope } from '../src/core/types.ts';
import { LOG_FILTER, RECORD_SIZE, lanePort, logFilter, laneSerial, logcatSince, parseAdbDevices, startScreenrecord } from '../src/platform/android/local.ts';
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
});
