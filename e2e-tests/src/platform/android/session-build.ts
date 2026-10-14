import { spawnSync } from 'node:child_process';
import { appendFileSync, copyFileSync, existsSync, mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { APK, ASSEMBLE_HOST } from '../../host-app.ts';
import { BUILD_COMMAND, apkKey, inputTree, readStored, store, storedApk, toolchain, toolchainName, type ApkIdentity, type InputTree } from './stored-apk.ts';

export interface ApkOutcome {
  readonly state: 'stored' | 'built' | 'failed';
  readonly key: string | null;
  readonly storable: boolean;
  readonly why: string;
}

export const installedApk = (work: string): string => join(work, 'e2e-debug.apk');

function mark(work: string, name: string): void {
  appendFileSync(join(work, 'marks'), `${name} ${Date.now()}\n`);
}

function say(line: string): void {
  process.stdout.write(`${line}\n`);
}

function writeWhole(file: string, text: string): void {
  writeFileSync(`${file}.part`, text);
  renameSync(`${file}.part`, file);
}

function identify(worktree: string): { readonly tree: InputTree; readonly identity: ApkIdentity | null; readonly why: string } {
  const tree = inputTree(worktree);
  if (tree.changed.length > 0) return { tree, identity: null, why: `the checkout differs from ${tree.head.slice(0, 12)} in ${tree.changed.slice(0, 3).join(', ')}` };
  return { tree, identity: { inputs: tree.hash, ...toolchain(worktree), command: BUILD_COMMAND }, why: '' };
}

function decide(work: string, outcome: ApkOutcome): void {
  writeWhole(join(work, 'app.json'), `${JSON.stringify(outcome)}\n`);
  say(`app     ${outcome.state}  ${outcome.key ?? 'no key'}  ${outcome.why}`);
}

function buildOrTakeStored(work: string): number {
  const worktree = process.cwd();
  const { tree, identity, why } = identify(worktree);
  const key = identity === null ? null : apkKey(identity);
  if (key !== null && readStored(work, key) !== null) {
    copyFileSync(storedApk(work, key), installedApk(work));
    decide(work, { state: 'stored', key, storable: false, why: 'built before from the same sources, JDK and Android platform, so nothing is built' });
    return 0;
  }
  mark(work, 'build-started');
  say(`$ ${BUILD_COMMAND}`);
  const code = spawnSync('./gradlew', [...ASSEMBLE_HOST], { stdio: 'inherit' }).status ?? 1;
  mark(work, 'built');
  if (code !== 0) {
    decide(work, { state: 'failed', key, storable: false, why: `./gradlew exited ${code}` });
    return code;
  }
  copyFileSync(join(worktree, APK), installedApk(work));
  const after = identity === null ? null : inputTree(worktree);
  const refusal = identity === null || key === null ? why : after?.head !== tree.head || after.hash !== tree.hash || after.changed.length > 0 ? `the build changed the checkout: ${after?.changed.slice(0, 3).join(', ')}` : null;
  if (identity !== null && key !== null && refusal === null) store(work, installedApk(work), { key, identity, commit: tree.head, builtAt: new Date().toISOString() });
  decide(work, { state: 'built', key, storable: refusal === null, why: refusal === null ? 'stored for the next session with the same sources, JDK and Android platform' : `not stored: ${refusal}` });
  return 0;
}

function build(work: string): number {
  mkdirSync(work, { recursive: true });
  rmSync(join(work, 'app.json'), { force: true });
  rmSync(installedApk(work), { force: true });
  try {
    return buildOrTakeStored(work);
  } catch (error) {
    decide(work, { state: 'failed', key: null, storable: false, why: (error as Error).message });
    return 1;
  }
}

function setOutputs(values: Readonly<Record<string, string>>): void {
  const text = Object.entries(values).map(([name, value]) => `${name}=${value}\n`).join('');
  const file = process.env.GITHUB_OUTPUT ?? '';
  if (file === '') process.stdout.write(text);
  else appendFileSync(file, text);
}

function name(work: string): number {
  mkdirSync(work, { recursive: true });
  try {
    const worktree = process.cwd();
    const tools = toolchain(worktree);
    const { identity, why } = identify(worktree);
    if (identity === null) say(`::warning title=verify::no stored APK is used or kept: ${why}`);
    setOutputs({ toolchain: toolchainName(tools), key: identity === null ? '' : apkKey(identity) });
  } catch (error) {
    say(`::warning title=verify::no stored APK is used or kept: ${(error as Error).message}`);
    setOutputs({ toolchain: 'unknown', key: '' });
  }
  return 0;
}

function readOutcome(work: string): ApkOutcome | null {
  try {
    return JSON.parse(readFileSync(join(work, 'app.json'), 'utf8')) as ApkOutcome;
  } catch {
    return null;
  }
}

function outcome(work: string): number {
  const app = readOutcome(work);
  setOutputs({ state: app?.state ?? 'none', store: app?.state === 'built' && app.storable ? (app.key ?? '') : '', built: app?.state === 'built' ? (app.key ?? '') : '' });
  return 0;
}

export function stageLine(marks: string, app: ApkOutcome | null): string {
  const at = marks.split('\n').map((line) => line.trim().split(' ')).filter((parts): parts is [string, string] => parts.length === 2 && /^\d+$/.test(parts[1] ?? '')).map(([stage, ms]) => ({ stage, ms: Number(ms) }));
  const first = at[0]?.ms ?? 0;
  const stages = at.slice(1).sort((a, b) => a.ms - b.ms).map(({ stage, ms }) => `${stage} ${Math.round((ms - first) / 1000)}`);
  return `verify-remote stages, in seconds after the job's first step: app ${app?.state ?? 'none'}${app?.key == null ? '' : ` ${app.key}`}; ${stages.join(', ')}`;
}

function stages(work: string): number {
  say(stageLine(existsSync(join(work, 'marks')) ? readFileSync(join(work, 'marks'), 'utf8') : '', readOutcome(work)));
  return 0;
}

if (import.meta.main) {
  const [command, work = ''] = process.argv.slice(2);
  const commands: Readonly<Record<string, () => number>> = { name: () => name(work), build: () => build(work), outcome: () => outcome(work), stages: () => stages(work) };
  const chosen = work === '' ? undefined : commands[command ?? ''];
  if (chosen === undefined) {
    console.error('usage: session-build.ts name|build|outcome|stages <work>');
    process.exit(2);
  }
  process.exitCode = chosen();
}
