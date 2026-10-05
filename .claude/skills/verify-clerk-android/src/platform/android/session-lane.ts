import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import type { BuiltApp } from '../../core/types.ts';
import { LANE_BOOTED, LANE_FAILED } from './emulator.ts';
import { laneSerial, localAndroidBackend } from './local.ts';

/**
 * Boots the emulator of a remote session. The runner is a Linux machine with KVM, so it boots a lane exactly as a
 * developer's machine does, through the local backend, and the session agent then serves that lane.
 */

const SLOT = 1;

if (import.meta.main) {
  const [command, work = '.'] = process.argv.slice(2);
  if (command === 'serial') {
    console.log(laneSerial(SLOT));
  } else if (command === 'boot') {
    try {
      const lease = await localAndroidBackend().acquire({ platform: 'android', worktree: process.cwd(), waitSeconds: 0, app: null as unknown as BuiltApp, retryWith: '', progress: (line) => console.log(line) });
      if (lease.deviceId !== laneSerial(SLOT)) throw new Error(`the lane came up as ${lease.deviceId}, not ${laneSerial(SLOT)}`);
      writeFileSync(join(work, LANE_BOOTED), `${lease.deviceId}\n`);
    } catch (error) {
      writeFileSync(join(work, LANE_FAILED), `${(error as Error).message}\n`);
      console.error((error as Error).message);
      process.exit(1);
    }
  } else {
    console.error('usage: session-lane.ts serial | boot <work-dir>');
    process.exit(2);
  }
}
