import { join } from 'node:path';

export const APK = join('e2e', 'build', 'outputs', 'apk', 'debug', 'e2e-debug.apk');

export const ASSEMBLE_HOST = [':e2e:assembleDebug', '--console=plain', '--quiet'] as const;
