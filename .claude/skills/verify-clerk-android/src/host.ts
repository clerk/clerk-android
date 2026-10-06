import { copyFileSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';
import { VerifyFailure, type HostAdapter, type NativeHostScreen, type ScratchPath } from './core/types.ts';
import { localAndroidBackend } from './platform/android/local.ts';
import { resolveJavaHome, sdkRoot } from './platform/android/sdk.ts';
import { APK, APP_ID, assembleHost } from './host-app.ts';

const WORKTREE = fileURLToPath(new URL('../../../../', import.meta.url));
const GITHUB_REPO = 'clerk/clerk-android';

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
  cli: '.claude/skills/verify-clerk-android/bin/control-clerk-android',
  platforms: ['android'],
  screens: ['home', 'auth', 'userProfile', 'orgSwitcher', 'orgList', 'orgProfile'],
  githubRepo: GITHUB_REPO,
  appId: () => APP_ID,
  buildInputs: () => ['source', 'e2e', 'gradle', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew'],
  async build(platform, key, into) {
    const java = resolveJavaHome();
    if (!java.ok) throw new VerifyFailure('NOT_READY', java.detail, java.fix);
    const result = await gradle(assembleHost().args, WORKTREE, java.home);
    if (result.code !== 0) {
      throw new VerifyFailure('BUILD_FAILED', `./gradlew :e2e:assembleDebug exited ${result.code}:\n${result.tail}`, 'fix the build error above, then rerun {cli} up');
    }
    mkdirSync(into, { recursive: true });
    const path = join(into, 'e2e-debug.apk') as ScratchPath;
    copyFileSync(join(WORKTREE, APK), path);
    return { platform, key, appId: APP_ID, path, source: 'local' };
  },
  entry: () => ({ kind: 'binary' }),
  features: ['auth-start', 'sign-in-email-code', 'sign-up', 'user-button-and-profile', 'session-tasks', 'organizations'],
  backends: [
    localAndroidBackend(),
  ],
};
