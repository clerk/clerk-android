import { join } from 'node:path';
import type { CommandLine } from './core/exec.ts';

export const APK = join('e2e', 'build', 'outputs', 'apk', 'debug', 'e2e-debug.apk');

export const ASSEMBLE_HOST = [':e2e:assembleDebug', '--console=plain', '--quiet'] as const;

export const assembleHost = (): CommandLine => ({ command: './gradlew', args: [...ASSEMBLE_HOST] });
