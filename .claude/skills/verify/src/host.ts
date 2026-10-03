import { copyFileSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { spawn } from 'node:child_process';
import { VerifyFailure, type HostAdapter, type NativeHostScreen, type ScratchPath } from './core/types.ts';
import { localAndroidBackend } from './platform/android/local.ts';
import { resolveJavaHome, sdkRoot } from './platform/android/sdk.ts';

const APP_ID = 'com.clerk.e2e';
const APK = join('e2e', 'build', 'outputs', 'apk', 'debug', 'e2e-debug.apk');

function gradle(args: readonly string[], cwd: string, javaHome: string): Promise<{ code: number; tail: string }> {
  return new Promise((resolve) => {
    const child = spawn('./gradlew', [...args], { cwd, env: { ...process.env, JAVA_HOME: javaHome, ANDROID_HOME: sdkRoot() }, stdio: ['ignore', 'pipe', 'pipe'] });
    const lines: string[] = [];
    const collect = (chunk: Buffer) => {
      for (const line of chunk.toString().split('\n')) {
        if (line.trim().length === 0) continue;
        lines.push(line);
        if (lines.length > 40) lines.shift();
      }
    };
    child.stdout.on('data', collect);
    child.stderr.on('data', collect);
    child.on('error', () => resolve({ code: 127, tail: './gradlew could not start' }));
    child.on('close', (code) => resolve({ code: code ?? 1, tail: lines.slice(-15).join('\n') }));
  });
}

export const host: HostAdapter<NativeHostScreen> = {
  repo: 'clerk-android',
  platforms: ['android'],
  screens: ['home', 'auth', 'userProfile', 'orgSwitcher', 'orgList', 'orgProfile'],
  keysFile: '.keys.json',
  githubRepo: 'clerk/clerk-android',
  appId: () => APP_ID,
  buildInputs: () => ['source', 'e2e', 'gradle', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew'],
  buildSources: () => ['local'],
  async build(platform, source, key, into) {
    const java = resolveJavaHome();
    if (!java.ok) throw new VerifyFailure('NOT_READY', java.detail, java.fix);
    const worktree = new URL('../../../../', import.meta.url).pathname;
    const result = await gradle([':e2e:assembleDebug', '--console=plain', '--quiet'], worktree, java.home);
    if (result.code !== 0) {
      throw new VerifyFailure('BUILD_FAILED', `./gradlew :e2e:assembleDebug exited ${result.code}:\n${result.tail}`, 'fix the build error above, then rerun bin/verify up');
    }
    mkdirSync(into, { recursive: true });
    const path = join(into, 'e2e-debug.apk') as ScratchPath;
    copyFileSync(join(worktree, APK), path);
    return { platform, key, appId: APP_ID, path, source, sourceSha: null };
  },
  entry: () => ({ kind: 'binary' }),
  features: ['auth-start', 'sign-in-email-code', 'sign-up', 'user-button-and-profile', 'session-tasks', 'organizations'],
  backends: [localAndroidBackend()],
};
