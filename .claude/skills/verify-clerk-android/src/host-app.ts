import { join } from 'node:path';
import type { CommandLine } from './core/exec.ts';

export const APP_ID = 'com.clerk.e2e';

export const APK = join('e2e', 'build', 'outputs', 'apk', 'debug', 'e2e-debug.apk');

export const assembleHost = (): CommandLine => ({ command: './gradlew', args: [':e2e:assembleDebug', '--console=plain', '--quiet'] });
