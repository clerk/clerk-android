import type { SessionDeviceFactory } from '../../core/remote/protocol.ts';
import { APK, LANE_BOOTED, LANE_FAILED, assembleHost, installArgs, logcatArgs, recordingOnDevice, screenrecordArgs } from './emulator.ts';

const RECORDING = recordingOnDevice('session');

const sessionDevice: SessionDeviceFactory = ({ id: serial }) => {
  const adb = (args: readonly string[]) => ({ command: 'adb', args: ['-s', serial, ...args] });
  return {
    build: (work) => [
      assembleHost(),
      // Booted is not enough: the lane boot may still restart zygote for the locale, and an install then fails.
      { command: 'sh', args: ['-c', `until [ -f "$0/${LANE_BOOTED}" ]; do if [ -f "$0/${LANE_FAILED}" ]; then echo "the emulator did not boot: $(cat "$0/${LANE_FAILED}")"; exit 1; fi; sleep 1; done`, work] },
      adb(installArgs(APK)),
    ],
    record: {
      start: () => adb(screenrecordArgs(RECORDING)),
      // SIGINT to the adb process on this machine would leave an mp4 with no moov atom, so screenrecord is stopped where it runs.
      stop: () => [adb(['shell', 'pkill -INT screenrecord; i=0; while pidof screenrecord > /dev/null && [ $i -lt 100 ]; do sleep 0.2; i=$((i+1)); done'])],
      collect: (file) => [adb(['pull', RECORDING, file]), adb(['shell', 'rm', '-f', RECORDING])],
    },
    logs: (since, predicate) => adb(logcatArgs(since, predicate)),
  };
};

export default sessionDevice;
