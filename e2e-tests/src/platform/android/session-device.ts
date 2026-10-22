import { fileURLToPath } from 'node:url';
import type { SessionDeviceFactory } from '@clerk/internal-mobile-e2e/core/remote/protocol';
import { BUILD_INPUTS } from '../../host-app.ts';
import { collectRecordingArgs, logcatArgs, recordingOnDevice, screenrecordArgs, stopScreenrecordArgs, waitForLane } from './emulator.ts';
import { installedApk } from './session-build.ts';
import { APP_ID } from '../../../specs/app.ts';

const SESSION_BUILD = fileURLToPath(new URL('./session-build.ts', import.meta.url));
const WORKTREE = fileURLToPath(new URL('../../../../', import.meta.url));
const RECORDING = recordingOnDevice('session');

// A debug APK is signed with the debug key of the machine that built it, and Android refuses to update an app signed with another key.
const INSTALL_OVER_ANOTHER_KEY = [
  'out=$(adb -s "$0" install -r -t "$1" 2>&1); status=$?; echo "$out"',
  'case "$out" in *INSTALL_FAILED_UPDATE_INCOMPATIBLE*) adb -s "$0" uninstall "$2" && adb -s "$0" install -t "$1" ;; *) exit $status ;; esac',
].join('\n');

const sessionDevice: SessionDeviceFactory = ({ id: serial }) => {
  const adb = (args: readonly string[]) => ({ command: 'adb', args: ['-s', serial, ...args] });
  return {
    inputs: BUILD_INPUTS,
    build: (work) => [{ command: process.execPath, args: [SESSION_BUILD, 'build', work], cwd: WORKTREE }, waitForLane(work), { command: 'sh', args: ['-c', INSTALL_OVER_ANOTHER_KEY, serial, installedApk(work), APP_ID] }],
    record: {
      start: () => adb(screenrecordArgs(RECORDING)),
      stop: () => [adb(stopScreenrecordArgs)],
      collect: (file) => collectRecordingArgs(RECORDING, file).map(adb),
    },
    logs: (since, predicate) => adb(logcatArgs(since, predicate)),
  };
};

export default sessionDevice;
