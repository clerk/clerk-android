import { join } from 'node:path';

export const APP_ID = 'com.clerk.e2e';

export const APK = join('e2e', 'build', 'outputs', 'apk', 'debug', 'e2e-debug.apk');

export const ASSEMBLE_HOST = [':e2e:assembleDebug', '--console=plain', '--quiet'] as const;
