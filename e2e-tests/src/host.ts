import { copyFileSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { run } from './core/exec.ts';
import { VerifyFailure, type HostAdapter, type ScratchPath } from './core/types.ts';
import { localAndroidBackend } from './platform/android/local.ts';
import { resolveJavaHome, sdkRoot } from './platform/android/sdk.ts';
import { APK, ASSEMBLE_HOST } from './host-app.ts';
import { APP_ID, app } from '../specs/app.ts';

const WORKTREE = fileURLToPath(new URL('../../', import.meta.url));
const GITHUB_REPO = 'clerk/clerk-android';

export const host: HostAdapter = {
  repo: 'clerk-android',
  cli: 'e2e-tests/bin/control-clerk-android',
  platforms: ['android'],
  githubRepo: GITHUB_REPO,
  appId: app.id,
  buildInputs: () => ['source', 'e2e', 'gradle', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew'],
  async build(platform, key, into) {
    const java = resolveJavaHome();
    if (!java.ok) throw new VerifyFailure('NOT_READY', java.detail, java.fix);
    const result = await run('./gradlew', ASSEMBLE_HOST, { cwd: WORKTREE, env: { ...process.env, JAVA_HOME: java.home, ANDROID_HOME: sdkRoot() } });
    if (result.code !== 0) {
      const tail = `${result.stdout}\n${result.stderr}`.split('\n').filter((line) => line.trim().length > 0).slice(-15).join('\n');
      throw new VerifyFailure('BUILD_FAILED', `./gradlew :e2e:assembleDebug exited ${result.code}:\n${tail}`, 'fix the build error above, then rerun {cli} up');
    }
    mkdirSync(into, { recursive: true });
    const path = join(into, 'e2e-debug.apk') as ScratchPath;
    copyFileSync(join(WORKTREE, APK), path);
    return { platform, key, appId: APP_ID, path, source: 'local' };
  },
  backends: [localAndroidBackend()],
};
