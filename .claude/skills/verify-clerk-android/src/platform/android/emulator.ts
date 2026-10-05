import { join } from 'node:path';
import type { CommandLine } from '../../core/exec.ts';
import { AVD_NAME } from './sdk.ts';

/** The emulator and adb commands the local backend and a remote session both run, so the two cannot drift. */

export const APP_ID = 'com.clerk.e2e';
export const APK = join('e2e', 'build', 'outputs', 'apk', 'debug', 'e2e-debug.apk');
/** The default size fails on this AVD's 1280x2856 panel. */
export const RECORD_SIZE = '720x1608';
export const LOG_FILTER = ['ClerkVerify:V', 'ClerkLog:V', 'OkHttp:V', 'ReactNativeJS:V', 'AndroidRuntime:E', '*:S'];

/** A Linux machine that runs lanes is headless as a rule and has no GPU the emulator can use. */
export function emulatorArgs(port: number, os: NodeJS.Platform): readonly string[] {
  return ['-avd', AVD_NAME, '-read-only', '-no-window', '-no-audio', '-no-boot-anim', ...(os === 'linux' ? ['-gpu', 'swiftshader_indirect'] : []), '-port', String(port)];
}

export const assembleHost = (): CommandLine => ({ command: './gradlew', args: [':e2e:assembleDebug', '--console=plain', '--quiet'] });

export const installArgs = (apk: string): readonly string[] => ['install', '-r', '-t', apk];

/** Logcat filter specs (`Tag:Level`, space separated) from HostAdapter.logPredicates go before the final `*:S`. */
export function logFilter(extraPredicate?: string | null): readonly string[] {
  const extra = extraPredicate?.split(/\s+/).filter((spec) => spec.length > 0) ?? [];
  return [...LOG_FILTER.slice(0, -1), ...extra, '*:S'];
}

export function logcatSince(since: Date): string {
  return (since.getTime() / 1000).toFixed(3);
}

export const logcatArgs = (since: Date, extraPredicate?: string | null): readonly string[] => ['logcat', '-d', '-v', 'threadtime', '-T', logcatSince(since), ...logFilter(extraPredicate)];

/** Shell-owned and there from the first second of boot, where /sdcard refuses writes until storage is mounted. */
export const recordingOnDevice = (name: string): string => `/data/local/tmp/verify-${name}.mp4`;

/** Files the runner's lane boot leaves in the session's work directory, which the build recipe waits on. */
export const LANE_BOOTED = 'lane-booted';
export const LANE_FAILED = 'lane-failed';

/** `--time-limit 0` lifts the 180 second cap. */
export const screenrecordArgs = (deviceFile: string): readonly string[] => ['shell', 'screenrecord', '--size', RECORD_SIZE, '--time-limit', '0', deviceFile];
