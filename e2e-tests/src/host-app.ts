import { join } from 'node:path';

export const BUILD_INPUTS = ['source', 'e2e', 'gradle', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew'] as const;

export const APK = join('e2e', 'build', 'outputs', 'apk', 'debug', 'e2e-debug.apk');

export const ASSEMBLE_HOST = [':e2e:assembleDebug', '--console=plain', '--quiet'] as const;
