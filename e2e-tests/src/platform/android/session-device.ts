import type { SessionDeviceFactory } from '../../core/remote/protocol.ts';
import { APK, BUILD_INPUTS, assembleHost } from '../../host-app.ts';
import { collectRecordingArgs, installArgs, logcatArgs, recordingOnDevice, screenrecordArgs, stopScreenrecordArgs, waitForLane } from './emulator.ts';

const RECORDING = recordingOnDevice('session');

const sessionDevice: SessionDeviceFactory = ({ id: serial }) => {
  const adb = (args: readonly string[]) => ({ command: 'adb', args: ['-s', serial, ...args] });
  return {
    inputs: BUILD_INPUTS,
    build: (work) => [assembleHost(), waitForLane(work), adb(installArgs(APK))],
    record: {
      start: () => adb(screenrecordArgs(RECORDING)),
      stop: () => [adb(stopScreenrecordArgs)],
      collect: (file) => collectRecordingArgs(RECORDING, file).map(adb),
    },
    logs: (since, predicate) => adb(logcatArgs(since, predicate)),
  };
};

export default sessionDevice;
