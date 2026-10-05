import { copyFileSync, mkdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';
import { remoteBackend } from './core/remote/backend.ts';
import { VerifyFailure, type HostAdapter, type NativeHostScreen, type ScratchPath } from './core/types.ts';
import { APK, APP_ID, assembleHost } from './platform/android/emulator.ts';
import { localAndroidBackend } from './platform/android/local.ts';
import { AVD_NAME, resolveJavaHome, sdkRoot } from './platform/android/sdk.ts';

const SKILL_DIR = fileURLToPath(new URL('../', import.meta.url));
const GITHUB_REPO = 'clerk/clerk-android';

function pinnedAgentDevice(): string {
  try {
    const mobile = JSON.parse(readFileSync(join(SKILL_DIR, 'node_modules', '@e2e-dev', 'mobile', 'package.json'), 'utf8')) as { dependencies?: Record<string, string> };
    const version = mobile.dependencies?.['agent-device'];
    if (version !== undefined) return version;
  } catch {
    // Falls through to the fix below.
  }
  throw new VerifyFailure('NOT_READY', 'the pinned @e2e-dev/mobile is not installed, so the agent-device version a session needs is unknown', `npm ci --prefix ${SKILL_DIR}`);
}

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
  githubRepo: GITHUB_REPO,
  cli: '.claude/skills/verify-clerk-android/bin/control-clerk-android',
  appId: () => APP_ID,
  buildInputs: () => ['source', 'e2e', 'gradle', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew'],
  buildSources: () => ['local'],
  async build(platform, source, key, into) {
    const java = resolveJavaHome();
    if (!java.ok) throw new VerifyFailure('NOT_READY', java.detail, java.fix);
    const worktree = fileURLToPath(new URL('../../../../', import.meta.url));
    const result = await gradle(assembleHost().args, worktree, java.home);
    if (result.code !== 0) {
      throw new VerifyFailure('BUILD_FAILED', `./gradlew :e2e:assembleDebug exited ${result.code}:\n${result.tail}`, 'fix the build error above, then rerun {cli} up');
    }
    mkdirSync(into, { recursive: true });
    const path = join(into, 'e2e-debug.apk') as ScratchPath;
    copyFileSync(join(worktree, APK), path);
    return { platform, key, appId: APP_ID, path, source, sourceSha: null };
  },
  entry: () => ({ kind: 'binary' }),
  features: ['auth-start', 'sign-in-email-code', 'sign-up', 'user-button-and-profile', 'session-tasks', 'organizations'],
  backends: [
    localAndroidBackend(),
    remoteBackend({
      platform: 'android',
      repo: GITHUB_REPO,
      workflow: 'verify-remote.yml',
      sessionsDir: join(SKILL_DIR, '.verify', 'remote'),
      runner: 'blacksmith-4vcpu-ubuntu-2404',
      planRunner: 'blacksmith-2vcpu-ubuntu-2404',
      plumbingRunner: 'ubuntu-latest',
      device: AVD_NAME,
      idleMinutes: 15,
      capMinutes: 60,
      agentDevice: pinnedAgentDevice,
      requirement: 'a pushed branch and access to GitHub Actions on clerk/clerk-android',
    }),
  ],
};
