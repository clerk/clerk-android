import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { copyFileSync, existsSync, mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { ASSEMBLE_HOST, BUILD_INPUTS } from '../../host-app.ts';

export interface ApkIdentity {
  readonly inputs: string;
  readonly jdk: string;
  readonly platform: string;
  readonly command: string;
}

export interface InputTree {
  readonly head: string;
  readonly hash: string;
  readonly changed: readonly string[];
}

export interface Toolchain {
  readonly jdk: string;
  readonly platform: string;
}

export interface StoredApk {
  readonly key: string;
  readonly identity: ApkIdentity;
  readonly commit: string;
  readonly builtAt: string;
}

const sha256 = (text: string): string => createHash('sha256').update(text).digest('hex');

export const BUILD_COMMAND = ['./gradlew', ...ASSEMBLE_HOST].join(' ');

export function apkKey(identity: ApkIdentity): string {
  return sha256(JSON.stringify([identity.inputs, identity.jdk, identity.platform, identity.command])).slice(0, 32);
}

function output(command: string, args: readonly string[], cwd: string): { readonly stdout: string; readonly stderr: string } {
  const ran = spawnSync(command, [...args], { cwd, encoding: 'utf8', timeout: 60_000, maxBuffer: 64 * 1024 * 1024 });
  if (ran.status !== 0) throw new Error(`${command} ${args.join(' ')} exited ${ran.status ?? ran.signal}: ${(ran.stderr ?? '').trim() || (ran.error?.message ?? 'no output')}`);
  return { stdout: ran.stdout, stderr: ran.stderr };
}

const git = (worktree: string, ...args: string[]): string => output('git', args, worktree).stdout;

export function inputTree(worktree: string): InputTree {
  const files = git(worktree, 'ls-tree', '-r', '-z', 'HEAD', '--', ...BUILD_INPUTS).split('\0').filter((line) => line !== '');
  const changed = git(worktree, 'status', '--porcelain', '-z', '--', ...BUILD_INPUTS).split('\0').filter((line) => line !== '').map((line) => line.slice(3));
  // Gradle compiles and packages every file in a source set, whether git ignores it or not.
  const ignored = git(worktree, 'status', '--porcelain', '--ignored', '-z', '--', ...BUILD_INPUTS).split('\0').filter((line) => line.startsWith('!! ')).map((line) => line.slice(3)).filter((path) => path.includes('/src/') && !path.endsWith('/.DS_Store'));
  return { head: git(worktree, 'rev-parse', 'HEAD').trim(), hash: sha256(files.join('\0')), changed: [...changed, ...ignored] };
}

export function compileSdk(worktree: string): string {
  const version = /^compileSdk\s*=\s*"(\d+)"/m.exec(readFileSync(join(worktree, 'gradle', 'libs.versions.toml'), 'utf8'))?.[1];
  if (version === undefined) throw new Error('gradle/libs.versions.toml names no compileSdk');
  return version;
}

export function toolchain(worktree: string, env: NodeJS.ProcessEnv = process.env): Toolchain {
  const java = env.JAVA_HOME === undefined || env.JAVA_HOME === '' ? 'java' : join(env.JAVA_HOME, 'bin', 'java');
  const jdk = output(java, ['-version'], worktree).stderr.trim().split('\n').map((line) => line.trim()).join('; ');
  if (jdk === '') throw new Error(`${java} -version printed nothing`);
  const platform = `android-${compileSdk(worktree)}`;
  const sdk = env.ANDROID_HOME ?? env.ANDROID_SDK_ROOT ?? '';
  const properties = join(sdk, 'platforms', platform, 'source.properties');
  const revision = existsSync(properties) ? /^Pkg\.Revision=(\S+)$/m.exec(readFileSync(properties, 'utf8'))?.[1] : undefined;
  if (sdk === '' || revision === undefined) throw new Error(`the Android SDK has no ${platform} platform with a revision at ${properties}`);
  return { jdk, platform: `${platform}r${revision}` };
}

export const toolchainName = (tools: Toolchain): string => `${/version "([^"]+)"/.exec(tools.jdk)?.[1] ?? sha256(tools.jdk).slice(0, 12)}-${tools.platform}`.replace(/[^\w.-]/g, '_');

const storedDir = (work: string, key: string): string => join(work, 'apks', key);

export const storedApk = (work: string, key: string): string => join(storedDir(work, key), 'e2e-debug.apk');

export function readStored(work: string, key: string): StoredApk | null {
  try {
    const stored = JSON.parse(readFileSync(join(storedDir(work, key), 'stored.json'), 'utf8')) as StoredApk;
    return stored.key === key && apkKey(stored.identity) === key && existsSync(storedApk(work, key)) ? stored : null;
  } catch {
    return null;
  }
}

export function store(work: string, apk: string, stored: StoredApk): void {
  const dir = storedDir(work, stored.key);
  const staging = `${dir}.staging`;
  rmSync(staging, { recursive: true, force: true });
  rmSync(dir, { recursive: true, force: true });
  mkdirSync(staging, { recursive: true });
  copyFileSync(apk, join(staging, 'e2e-debug.apk'));
  writeFileSync(join(staging, 'stored.json'), `${JSON.stringify(stored, null, 2)}\n`);
  renameSync(staging, dir);
}
