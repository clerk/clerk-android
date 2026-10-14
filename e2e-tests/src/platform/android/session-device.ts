import { fileURLToPath } from 'node:url';
import type { SessionDeviceFactory } from '../../core/remote/protocol.ts';
import { BUILD_INPUTS } from '../../host-app.ts';
import { collectRecordingArgs, installArgs, logcatArgs, recordingOnDevice, screenrecordArgs, stopScreenrecordArgs, waitForLane } from './emulator.ts';
import { installedApk } from './session-build.ts';

const SESSION_BUILD = fileURLToPath(new URL('./session-build.ts', import.meta.url));
const WORKTREE = fileURLToPath(new URL('../../../../', import.meta.url));
const RECORDING = recordingOnDevice('session');

const sessionDevice: SessionDeviceFactory = ({ id: serial }) => {
  const adb = (args: readonly string[]) => ({ command: 'adb', args: ['-s', serial, ...args] });
  return {
    inputs: BUILD_INPUTS,
    build: (work) => [{ command: process.execPath, args: [SESSION_BUILD, 'build', work], cwd: WORKTREE }, waitForLane(work), adb(installArgs(installedApk(work)))],
    record: {
      start: () => adb(screenrecordArgs(RECORDING)),
      stop: () => [adb(stopScreenrecordArgs)],
      collect: (file) => collectRecordingArgs(RECORDING, file).map(adb),
    },
    logs: (since, predicate) => adb(logcatArgs(since, predicate)),
  };
};

export default sessionDevice;
