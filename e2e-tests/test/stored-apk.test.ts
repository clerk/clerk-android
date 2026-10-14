import '../testing/git-env.ts';
import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { after, describe, it } from 'node:test';
import { BUILD_INPUTS } from '../src/host-app.ts';
import { stageLine } from '../src/platform/android/session-build.ts';
import { apkKey } from '../src/platform/android/stored-apk.ts';

const PACKAGE_DIR = join(import.meta.dirname, '..');
const SESSION_BUILD = join(PACKAGE_DIR, 'src', 'platform', 'android', 'session-build.ts');
const WORKFLOWS = join(PACKAGE_DIR, '..', '.github', 'workflows');
const AUTHOR = ['-c', 'user.name=t', '-c', 'user.email=t@example.com'];
const SOURCE = 'source/ui/src/main/java/com/clerk/ui/auth/AuthStartView.kt';
const HOST_APP = 'e2e/src/main/java/com/clerk/e2e/MainActivity.kt';
const CATALOG = 'gradle/libs.versions.toml';

const GRADLEW = `#!/bin/sh
echo "gradlew $*" >> "$FAKE/calls"
[ -f "$FAKE/fail" ] && { echo "e: AuthStartView.kt: unresolved reference"; exit 1; }
[ -f "$FAKE/litter" ] && echo generated > source/ui/src/main/Generated.kt
mkdir -p e2e/build/outputs/apk/debug
cat ${SOURCE} ${HOST_APP} > e2e/build/outputs/apk/debug/e2e-debug.apk
`;

const JAVA = `#!/bin/sh
printf 'openjdk version "%s" 2026-07-21 LTS\\nOpenJDK Runtime Environment Zulu\\n' "$(cat "$FAKE/jdk-version")" >&2
`;

interface Checkout {
  readonly dir: string;
  readonly fake: string;
  readonly env: NodeJS.ProcessEnv;
}

const made: string[] = [];
after(() => made.forEach((dir) => rmSync(dir, { recursive: true, force: true })));

function scratch(prefix: string): string {
  const dir = mkdtempSync(join(tmpdir(), prefix));
  made.push(dir);
  return dir;
}

function write(dir: string, file: string, text: string, mode = 0o644): void {
  mkdirSync(dirname(join(dir, file)), { recursive: true });
  writeFileSync(join(dir, file), text, { mode });
}

function commit(checkout: Checkout, file: string, text: string): void {
  write(checkout.dir, file, text);
  execFileSync('git', ['add', '-A'], { cwd: checkout.dir });
  execFileSync('git', [...AUTHOR, 'commit', '-q', '-m', file], { cwd: checkout.dir });
}

const setRevision = (from: Checkout, revision: string): void => write(from.fake, 'sdk/platforms/android-37.0/source.properties', `Pkg.Desc=Android SDK Platform 37\nPkg.Revision=${revision}\n`);

function checkout(): Checkout {
  const dir = scratch('verify-stored-apk-');
  const fake = scratch('verify-stored-apk-fake-');
  write(fake, 'jdk/bin/java', JAVA, 0o755);
  writeFileSync(join(fake, 'jdk-version'), '21.0.12');
  writeFileSync(join(fake, 'calls'), '');
  const made: Checkout = { dir, fake, env: { PATH: process.env.PATH, FAKE: fake, HOME: fake, JAVA_HOME: join(fake, 'jdk'), ANDROID_HOME: join(fake, 'sdk') } };
  setRevision(made, '2');
  execFileSync('git', ['init', '-q'], { cwd: dir });
  const files = {
    '.gitignore': '/e2e/build/\n/source/ui/build/\n/.gradle\nlocal.properties\n',
    [SOURCE]: 'sdk one\n',
    [HOST_APP]: 'host one\n',
    [CATALOG]: '[versions]\ncompileSdk = "37"\nagp = "9.4.1"\n',
    'gradle/wrapper/gradle-wrapper.properties': 'distributionUrl=gradle-9.8.1-bin.zip\n',
    'build.gradle.kts': 'plugins {}\n',
    'settings.gradle.kts': 'include(":e2e")\n',
    'gradle.properties': 'org.gradle.caching=true\n',
    'e2e/build.gradle.kts': 'android {}\n',
    'README.md': 'readme\n',
    'e2e-tests/specs/features/a.e2e.ts': 'spec\n',
    'samples/quickstart/src/main/Main.kt': 'sample\n',
  };
  for (const [file, text] of Object.entries(files)) write(dir, file, text);
  write(dir, 'gradlew', GRADLEW, 0o755);
  execFileSync('git', ['add', '-A'], { cwd: dir });
  execFileSync('git', [...AUTHOR, 'commit', '-q', '-m', 'first'], { cwd: dir });
  return made;
}

function sessionBuild(from: Checkout, args: readonly string[]): { readonly status: number | null; readonly stdout: string } {
  const ran = spawnSync(process.execPath, [SESSION_BUILD, ...args], { cwd: from.dir, env: from.env, encoding: 'utf8', timeout: 60_000 });
  return { status: ran.status, stdout: ran.stdout + ran.stderr };
}

function named(from: Checkout): Readonly<Record<string, string>> {
  const { stdout } = sessionBuild(from, ['name', scratch('verify-stored-apk-work-')]);
  return Object.fromEntries(stdout.split('\n').filter((line) => /^\w+=/.test(line)).map((line) => line.split('=') as [string, string]));
}

const builds = (from: Checkout): number => readFileSync(join(from.fake, 'calls'), 'utf8').split('\n').filter((line) => line.startsWith('gradlew :e2e:assembleDebug')).length;
const handedOver = (work: string): string => readFileSync(join(work, 'e2e-debug.apk'), 'utf8');
const outcomeOf = (work: string): { state: string; key: string | null; storable: boolean; why: string } => JSON.parse(readFileSync(join(work, 'app.json'), 'utf8'));

function carriedTo(work: string): string {
  const next = scratch('verify-stored-apk-work-');
  cpSync(join(work, 'apks'), join(next, 'apks'), { recursive: true });
  return next;
}

describe('the key of a stored e2e APK', () => {
  it('is 32 hex digits made from the sources, the JDK, the Android platform and the build command', () => {
    const identity = { inputs: 'a', jdk: 'openjdk version "21.0.12"', platform: 'android-37r2', command: './gradlew :e2e:assembleDebug' };
    assert.equal(apkKey(identity), apkKey({ ...identity }));
    assert.match(apkKey(identity), /^[0-9a-f]{32}$/);
    for (const field of Object.keys(identity) as (keyof typeof identity)[]) assert.notEqual(apkKey({ ...identity, [field]: 'other' }), apkKey(identity), field);
  });

  it('changes with an SDK source, the host app, the version catalog, the wrapper, a Gradle build file, the properties, gradlew, the JDK and the Android platform', () => {
    const repo = checkout();
    const first = named(repo);
    assert.match(first.key!, /^[0-9a-f]{32}$/);
    assert.equal(first.toolchain, '21.0.12-android-37.0r2');
    const changes: Readonly<Record<string, () => void>> = {
      'a Kotlin source under source': () => commit(repo, SOURCE, 'sdk two\n'),
      'a new Kotlin source under source': () => commit(repo, 'source/api/src/main/java/New.kt', 'new\n'),
      'the host app': () => commit(repo, HOST_APP, 'host two\n'),
      'the version catalog': () => commit(repo, CATALOG, '[versions]\ncompileSdk = "37"\nagp = "9.4.2"\n'),
      'the Gradle wrapper': () => commit(repo, 'gradle/wrapper/gradle-wrapper.properties', 'distributionUrl=gradle-9.9.0-bin.zip\n'),
      'the root build file': () => commit(repo, 'build.gradle.kts', 'plugins { id("x") }\n'),
      'the settings file': () => commit(repo, 'settings.gradle.kts', 'include(":e2e", ":x")\n'),
      'a module build file': () => commit(repo, 'e2e/build.gradle.kts', 'android { namespace = "x" }\n'),
      'gradle.properties': () => commit(repo, 'gradle.properties', 'org.gradle.caching=false\n'),
      gradlew: () => commit(repo, 'gradlew', `${GRADLEW}\n`),
      'the JDK': () => writeFileSync(join(repo.fake, 'jdk-version'), '21.0.13'),
      'the Android platform revision': () => setRevision(repo, '3'),
    };
    const seen = [first.key];
    for (const [what, change] of Object.entries(changes)) {
      change();
      const key = named(repo).key!;
      assert.match(key, /^[0-9a-f]{32}$/, what);
      assert.ok(!seen.includes(key), `${what} gave a key seen before`);
      seen.push(key);
    }
  });

  it('stays the same when only a spec, a doc, a workflow or a sample app changes', () => {
    const repo = checkout();
    const first = named(repo).key;
    commit(repo, 'samples/quickstart/src/main/Main.kt', 'sample two\n');
    commit(repo, 'e2e-tests/specs/features/a.e2e.ts', 'spec two\n');
    commit(repo, 'README.md', 'readme two\n');
    commit(repo, '.github/workflows/checks.yml', 'on: push\n');
    assert.equal(named(repo).key, first);
  });

  it('is empty, with the reason, when the app sources differ from the commit', () => {
    const repo = checkout();
    writeFileSync(join(repo.dir, SOURCE), 'not committed\n');
    const { stdout } = sessionBuild(repo, ['name', scratch('verify-stored-apk-work-')]);
    assert.match(stdout, /^::warning title=verify::no stored APK is used or kept: the checkout differs from [0-9a-f]{12} in source\/ui\/src\/main\/java\/com\/clerk\/ui\/auth\/AuthStartView\.kt$/m);
    assert.match(stdout, /^key=$/m);
  });

  it('is empty, with the reason, when a source set holds a file git ignores', () => {
    const repo = checkout();
    commit(repo, '.gitignore', '/e2e/build/\n*.secret.kt\n');
    write(repo.dir, 'e2e/src/main/java/Keys.secret.kt', 'secret\n');
    const { stdout } = sessionBuild(repo, ['name', scratch('verify-stored-apk-work-')]);
    assert.match(stdout, /^::warning title=verify::no stored APK is used or kept: the checkout differs from [0-9a-f]{12} in e2e\/src\/main\/java\/Keys\.secret\.kt$/m);
  });

  it('stays the same when a build leaves its ignored output folders', () => {
    const repo = checkout();
    const before = named(repo).key;
    write(repo.dir, 'e2e/build/outputs/apk/debug/e2e-debug.apk', 'apk\n');
    write(repo.dir, 'source/ui/build/tmp/kotlin/x', 'x\n');
    assert.equal(named(repo).key, before);
  });

  it('names the SDK package of the platform that compileSdk names, so that both workflows install it before they name the APK', () => {
    const repo = checkout();
    assert.deepEqual(sessionBuild(repo, ['platform', scratch('verify-stored-apk-work-')]), { status: 0, stdout: 'platforms;android-37.0 platforms;android-37\n' });
    for (const file of ['verify-remote.yml', 'verify-app-cache.yml']) {
      const steps = readFileSync(join(WORKFLOWS, file), 'utf8').split(/\n(?= {6}- )/);
      const install = steps.findIndex((step) => step.includes('for package in $(node "$SESSION_BUILD" platform "$VERIFY_SESSION_WORK"); do'));
      assert.ok(install > 0 && install < steps.findIndex((step) => step.includes('- name: Name the APK')), file);
    }
  });

  it('is empty, with the reason, when the SDK has no platform for compileSdk', () => {
    const repo = checkout();
    rmSync(join(repo.fake, 'sdk', 'platforms'), { recursive: true });
    const { stdout } = sessionBuild(repo, ['name', scratch('verify-stored-apk-work-')]);
    assert.match(stdout, /^::warning title=verify::no stored APK is used or kept: the Android SDK at .* has no platform android-37\.0 or android-37 with a revision$/m);
    assert.match(stdout, /^toolchain=unknown\nkey=$/m);
  });
});

describe('a session build', () => {
  it('builds an APK that is not stored, hands it to the install step, and stores it under its key', () => {
    const repo = checkout();
    const work = scratch('verify-stored-apk-work-');
    const { key } = named(repo);
    assert.equal(sessionBuild(repo, ['build', work]).status, 0);
    assert.equal(builds(repo), 1);
    assert.equal(handedOver(work), 'sdk one\nhost one\n');
    assert.deepEqual(outcomeOf(work), { state: 'built', key, storable: true, why: 'stored for the next session with the same sources, JDK and Android platform' });
    assert.deepEqual(readdirSync(join(work, 'apks')), [key]);
  });

  it('hands over the stored APK without a build when the key is the same', () => {
    const repo = checkout();
    const first = scratch('verify-stored-apk-work-');
    sessionBuild(repo, ['build', first]);
    commit(repo, 'e2e-tests/specs/features/a.e2e.ts', 'spec two\n');
    rmSync(join(repo.dir, 'e2e', 'build'), { recursive: true });
    const next = carriedTo(first);
    assert.equal(sessionBuild(repo, ['build', next]).status, 0);
    assert.equal(builds(repo), 1, 'no second build');
    assert.equal(handedOver(next), 'sdk one\nhost one\n');
    assert.equal(outcomeOf(next).state, 'stored');
  });

  it('builds again, and never hands over the stored APK, after a Kotlin source changes', () => {
    const repo = checkout();
    const first = scratch('verify-stored-apk-work-');
    sessionBuild(repo, ['build', first]);
    commit(repo, SOURCE, 'sdk two\n');
    const next = carriedTo(first);
    assert.equal(sessionBuild(repo, ['build', next]).status, 0);
    assert.equal(builds(repo), 2);
    assert.equal(handedOver(next), 'sdk two\nhost one\n');
    assert.equal(readdirSync(join(next, 'apks')).length, 2);
  });

  it('builds again when the stored APK was made with another JDK', () => {
    const repo = checkout();
    const first = scratch('verify-stored-apk-work-');
    sessionBuild(repo, ['build', first]);
    writeFileSync(join(repo.fake, 'jdk-version'), '21.0.13');
    assert.equal(sessionBuild(repo, ['build', carriedTo(first)]).status, 0);
    assert.equal(builds(repo), 2);
  });

  it('ignores a stored APK whose record does not make its key', () => {
    const repo = checkout();
    const first = scratch('verify-stored-apk-work-');
    sessionBuild(repo, ['build', first]);
    const key = outcomeOf(first).key!;
    commit(repo, SOURCE, 'sdk two\n');
    const wanted = named(repo).key!;
    const next = scratch('verify-stored-apk-work-');
    cpSync(join(first, 'apks', key), join(next, 'apks', wanted), { recursive: true });
    assert.equal(sessionBuild(repo, ['build', next]).status, 0);
    assert.equal(builds(repo), 2);
    assert.equal(handedOver(next), 'sdk two\nhost one\n');
  });

  it('does not store an APK when the build changed the checkout', () => {
    const repo = checkout();
    const work = scratch('verify-stored-apk-work-');
    writeFileSync(join(repo.fake, 'litter'), '');
    assert.equal(sessionBuild(repo, ['build', work]).status, 0);
    assert.equal(outcomeOf(work).why, 'not stored: the build changed the checkout: source/ui/src/main/Generated.kt');
    assert.equal(existsSync(join(work, 'apks')), false);
    assert.match(sessionBuild(repo, ['outcome', work]).stdout, /^store=$/m);
  });

  it('fails with the exit code of gradlew and hands over no APK when the build fails', () => {
    const repo = checkout();
    const work = scratch('verify-stored-apk-work-');
    writeFileSync(join(repo.fake, 'fail'), '');
    const { status, stdout } = sessionBuild(repo, ['build', work]);
    assert.equal(status, 1);
    assert.match(stdout, /^e: AuthStartView\.kt: unresolved reference$/m);
    assert.equal(outcomeOf(work).state, 'failed');
    assert.equal(existsSync(join(work, 'e2e-debug.apk')), false);
  });

  it('builds changes applied on top of the commit and stores nothing', () => {
    const repo = checkout();
    const work = scratch('verify-stored-apk-work-');
    sessionBuild(repo, ['build', work]);
    writeFileSync(join(repo.dir, SOURCE), 'sdk patched\n');
    rmSync(join(repo.dir, 'README.md'));
    write(repo.dir, 'source/api/src/main/java/New.kt', 'new\n');
    assert.equal(sessionBuild(repo, ['build', work]).status, 0);
    assert.equal(builds(repo), 2);
    assert.equal(handedOver(work), 'sdk patched\nhost one\n');
    const outcome = outcomeOf(work);
    assert.deepEqual([outcome.state, outcome.key, outcome.storable], ['built', null, false]);
    assert.match(outcome.why, /^not stored: the checkout differs from [0-9a-f]{12} in /);
    assert.equal(readdirSync(join(work, 'apks')).length, 1, 'only the APK of the commit is stored');
    assert.match(sessionBuild(repo, ['outcome', work]).stdout, /^store=$/m);
  });

  it('reports what to store, and nothing for an APK that was already stored', () => {
    const repo = checkout();
    const work = scratch('verify-stored-apk-work-');
    sessionBuild(repo, ['build', work]);
    const key = outcomeOf(work).key;
    assert.equal(sessionBuild(repo, ['outcome', work]).stdout, `state=built\nstore=${key}\nbuilt=${key}\n`);
    const next = carriedTo(work);
    sessionBuild(repo, ['build', next]);
    assert.equal(sessionBuild(repo, ['outcome', next]).stdout, 'state=stored\nstore=\nbuilt=\n');
  });
});

describe('the stage line of a session', () => {
  it('gives each stage in seconds after the first one, in the order they happened', () => {
    const marks = 'start 1000000\ntools-installed 1009000\nbuild-started 1004400\nbooted 1031000\ndevice-ready 1050000\n';
    assert.equal(
      stageLine(marks, { state: 'stored', key: 'f1a099607eaff57479b9398c783e7ae3', storable: false, why: '' }),
      "verify-remote stages, in seconds after the job's first step: app stored f1a099607eaff57479b9398c783e7ae3; build-started 4, tools-installed 9, booted 31, device-ready 50",
    );
  });
});

describe('the workflows that store and restore', () => {
  const session = readFileSync(join(WORKFLOWS, 'verify-remote.yml'), 'utf8');
  const main = readFileSync(join(WORKFLOWS, 'verify-app-cache.yml'), 'utf8');

  function caches(workflow: string, action: 'restore' | 'save'): readonly string[] {
    const steps = workflow.split(/\n(?= {6}- )/).filter((step) => step.includes(`uses: actions/cache/${action}@`));
    return steps.map((step) => `${/^ {10}path: \|\n((?: {12}.+\n)+)|^ {10}path: (.+)$/m.exec(step)?.slice(1).find((path) => path !== undefined)?.trim().split(/\s+/).join(' ')} as ${/^ {10}key: (.+)$/m.exec(step)?.[1]}`).sort();
  }

  it('save from a session what main saves, except the build state, under the same path and key', () => {
    const buildState = caches(main, 'save').filter((line) => line.includes(' as verify-gradle-'));
    assert.equal(buildState.length, 1);
    assert.deepEqual(caches(session, 'save'), caches(main, 'save').filter((line) => !buildState.includes(line)));
  });

  it('restore in a session each thing that main saves, from the path it was saved at', () => {
    const restoredFallback = (line: string): string => line.replace('${{ steps.app.outputs.key || github.run_id }}', '${{ steps.report.outputs.built }}').replaceAll('steps.app.outputs.key', 'steps.report.outputs.store');
    assert.deepEqual(caches(session, 'restore').map(restoredFallback).sort(), caches(main, 'save'));
  });

  it('build the APK on main when any build input changes', () => {
    const paths = main.slice(main.indexOf('paths:'), main.indexOf('workflow_dispatch:')).split('\n').map((line) => line.trim().replace(/^- /, ''));
    for (const input of BUILD_INPUTS) assert.ok(paths.includes(input) || paths.includes(`${input}/**`), `${input} is not a path that starts verify-app-cache.yml`);
  });

  it('build the APK on main once a week, so that a new JDK or Android platform is stored before sessions need it, and build nothing when it is stored already', () => {
    assert.match(main, /^ {2}schedule:\n {4}- cron: "\d+ \d+ \* \* [0-6]"$/m);
    const steps = main.split(/\n(?= {6}- )/);
    assert.match(steps.find((step) => step.includes('id: stored')) ?? '', /^ {10}lookup-only: true$/m);
    assert.match(steps.find((step) => step.includes('- name: Build the e2e APK from nothing')) ?? '', /^ {8}if: .*steps\.stored\.outputs\.cache-hit != 'true'$/m);
  });

  it('leave the Gradle home to the build state, so that setup-gradle neither restores nor saves one of its own', () => {
    for (const workflow of [session, main]) {
      const steps = workflow.split(/\n(?= {6}- )/).filter((step) => step.includes('uses: gradle/actions/setup-gradle@'));
      assert.equal(steps.length, 1);
      assert.match(steps[0]!, /^ {10}cache-disabled: true$/m);
    }
  });
});

describe('the build state in a session', () => {
  const steps = readFileSync(join(WORKFLOWS, 'verify-remote.yml'), 'utf8').split(/\n(?= {6}- )/);
  const at = (needle: string) => steps.findIndex((step) => step.includes(needle));
  const restore = at('- name: Restore the build state');

  it('is restored whether or not the APK is stored, so that the first build of a change in the session is incremental', () => {
    assert.ok(restore > 0, 'the session restores the build state');
    assert.match(steps[restore]!, /^ {8}if: steps\.device\.outputs\.serial != ''$/m);
  });

  it('falls back to the build state of another JDK or Android platform, because the dependencies in the Gradle home do not depend on them', () => {
    assert.match(steps[restore]!, /^ {10}restore-keys: \|\n {12}verify-gradle-\$\{\{ steps\.app\.outputs\.toolchain \}\}-\n {12}verify-gradle-\n/m);
  });

  it('is restored before the session agent starts, because the agent can start a build at once', () => {
    assert.ok(restore < at('- name: Start the session agent and tunnel'), 'the restore comes before the agent');
  });
});

describe('a session that built uncommitted changes', () => {
  const steps = readFileSync(join(WORKFLOWS, 'verify-remote.yml'), 'utf8').split(/\n(?= {6}- )/);
  const at = (needle: string) => steps.findIndex((step) => step.includes(needle));
  const check = steps[at('id: patched')] ?? '';
  const script = check.slice(check.indexOf('run: |') + 'run: |'.length).split('\n').map((line) => line.trim()).join('\n');

  function checked(marked: boolean): string {
    const work = scratch('verify-patched-');
    if (marked) writeFileSync(join(work, 'patched'), `${'c'.repeat(64)}\n`);
    writeFileSync(join(work, 'output'), '');
    const ran = spawnSync('bash', ['-e', '-c', script], { env: { PATH: process.env.PATH, VERIFY_SESSION_WORK: work, GITHUB_OUTPUT: join(work, 'output') }, encoding: 'utf8', timeout: 20_000 });
    assert.equal(ran.status, 0);
    return readFileSync(join(work, 'output'), 'utf8');
  }

  it('is found from the mark the session agent leaves before it applies the changes', () => {
    assert.deepEqual([checked(true), checked(false)], ['built=true\n', '']);
  });

  it('stores no APK for later sessions, and checks for the mark after the build it waits for', () => {
    const saves = steps.flatMap((step, index) => (step.includes('uses: actions/cache/save@') ? [index] : []));
    assert.equal(saves.length, 1);
    for (const index of saves) {
      assert.match(steps[index]!, /^ {8}if: steps\.patched\.outputs\.built != 'true' && /m, steps[index]!.split('\n')[0]);
      assert.ok(at('id: patched') < index, 'the check comes before the save');
    }
    assert.ok(at('- name: Report the APK and the stage times') < at('id: patched'), 'the report waits for a running build, so a build that applied changes has left its mark');
  });
});
