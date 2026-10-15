import '../testing/git-env.ts';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, it } from 'node:test';

const WORKFLOWS = join(import.meta.dirname, '..', '..', '.github', 'workflows');
const read = (file: string): string => readFileSync(join(WORKFLOWS, file), 'utf8');
const files = readdirSync(WORKFLOWS).filter((file) => file.endsWith('.yml')).sort();

interface Context {
  readonly event: string;
  readonly repository?: string;
  readonly draft?: boolean;
  readonly cancelled?: boolean;
  readonly needs?: Readonly<Record<string, string>>;
  readonly inputs?: Readonly<Record<string, boolean>>;
}

function triggers(workflow: string): string {
  const start = workflow.indexOf('\non:\n');
  return workflow.slice(start, workflow.indexOf('\n\n', start + 1));
}

function jobs(workflow: string): ReadonlyMap<string, string> {
  const section = workflow.slice(workflow.indexOf('\njobs:\n'));
  return new Map(section.split(/\n(?= {2}[\w-]+:\n)/).slice(1).map((text) => [/^ {2}([\w-]+):/.exec(text)![1]!, text]));
}

// GitHub compares values of different types as numbers, so null == false is true there.
function eq(left: unknown, right: unknown): boolean {
  if (typeof left === 'string' && typeof right === 'string') return left.toLowerCase() === right.toLowerCase();
  return left === right || Number(left) === Number(right);
}

function evaluate(condition: string, context: Context): boolean {
  const value = (path: string): unknown => {
    if (path === 'github.event_name') return context.event;
    if (path === 'github.repository') return context.repository ?? 'clerk/clerk-android';
    if (path === 'github.event.pull_request.draft') return context.event === 'pull_request' ? (context.draft ?? false) : null;
    const need = /^needs\.([\w-]+)\.result$/.exec(path);
    if (need) return context.needs?.[need[1]!] ?? '';
    const input = /^inputs\.(\w+)$/.exec(path);
    if (input) return context.inputs?.[input[1]!] ?? null;
    throw new Error(`no value for ${path} in ${condition}`);
  };
  const literal = String.raw`"(?:[^"\\]|\\.)*"|null|true|false`;
  const results = Object.values(context.needs ?? {});
  const bare = condition.replace(/^\$\{\{ (.+) \}\}$/, '$1');
  // GitHub adds success() to a condition that calls no status function.
  const script = (/\b(?:always|cancelled|failure|success)\(\)/.test(bare) ? bare : `success() && (${bare})`)
    .replaceAll('success()', String(!(context.cancelled ?? false) && results.every((result) => result === 'success')))
    .replaceAll('failure()', String(results.includes('failure')))
    .replaceAll('cancelled()', String(context.cancelled ?? false))
    .replaceAll('always()', 'true')
    .replaceAll('needs.*.result', JSON.stringify(results))
    .replaceAll(/'([^']*)'/g, (_, text: string) => JSON.stringify(text))
    .replaceAll(/\b(?:github|needs|inputs)\.[\w.-]+/g, (path) => JSON.stringify(value(path)))
    .replaceAll(new RegExp(`(${literal}) (==|!=) (${literal})`, 'g'), (_, left: string, op: string, right: string) => `${op === '!=' ? '!' : ''}eq(${left}, ${right})`);
  const contains = (list: readonly unknown[], item: unknown): boolean => list.some((entry) => eq(entry, item));
  return Boolean(new Function('eq', 'contains', `return ${script};`)(eq, contains));
}

function ranJobs(context: Context): readonly string[] {
  const ran: string[] = [];
  for (const file of files) {
    const on = triggers(read(file));
    const triggered = context.event === 'push' ? /^ {2}push:\n {4}branches: \[main\]$/m.test(on) : new RegExp(`^ {2}${context.event}:`, 'm').test(on);
    if (!triggered) continue;
    const started = new Set<string>();
    for (const [name, text] of jobs(read(file))) {
      const condition = /^ {4}if: (.+)$/m.exec(text)?.[1];
      const needs = /^ {4}needs: \[?([^\]\n]+)\]?$/m.exec(text)?.[1]?.split(',').map((need) => need.trim()) ?? [];
      if (condition === undefined ? needs.every((need) => started.has(need)) : evaluate(condition, context)) {
        started.add(name);
        ran.push(`${file} ${name}`);
      }
    }
  }
  return ran;
}

const READ_RUNS_AND_PULLS = ['actions: read', 'contents: read', 'pull-requests: read'];

const permissions = (text: string): string[] => (/^ {4}permissions:\n((?: {6}[\w-]+: \w+\n)+)/m.exec(text)?.[1] ?? '').trim().split('\n').filter(Boolean).map((line) => line.trim());

const PULL_REQUEST_JOBS = [
  'android-test.yml e2e-runner-tests',
  'android-test.yml test-module',
  'android-test.yml api-check',
  'android.yml build-module',
  'checks.yml checks',
  'detekt.yml detekt',
];

describe('a pull request', () => {
  it('runs only the build, unit test, and lint jobs', () => {
    assert.deepEqual(ranJobs({ event: 'pull_request', draft: false }), PULL_REQUEST_JOBS);
  });

  it('runs no job while it is a draft', () => {
    assert.deepEqual(ranJobs({ event: 'pull_request', draft: true }), []);
  });

  it('runs the same jobs again when it is merged, as a push to main', () => {
    assert.deepEqual(ranJobs({ event: 'push' }), PULL_REQUEST_JOBS);
  });

  // setup-gradle restores nothing when another step has already created ~/.gradle/caches, as setup-java's Gradle cache does.
  it('restores the Gradle home that main saved through setup-gradle alone, in every job that runs Gradle', () => {
    const text = new Map(PULL_REQUEST_JOBS.map((job) => {
      const [file, name] = job.split(' ');
      return [job, jobs(read(file!)).get(name!) ?? ''];
    }));
    const gradle = PULL_REQUEST_JOBS.filter((job) => text.get(job)!.includes('./gradlew'));
    assert.deepEqual(gradle, ['android-test.yml test-module', 'android-test.yml api-check', 'android.yml build-module', 'checks.yml checks', 'detekt.yml detekt']);
    const setUpBeforeGradle = (job: string): boolean => {
      const setUp = /^ +uses: gradle\/actions\/setup-gradle@/m.exec(text.get(job)!)?.index ?? Infinity;
      return setUp < text.get(job)!.indexOf('./gradlew');
    };
    assert.deepEqual(gradle.filter((job) => !setUpBeforeGradle(job)), []);
    assert.deepEqual(gradle.filter((job) => /^\s+cache:\s*['"]?gradle\b/m.test(text.get(job)!)), []);
  });
});

const WEBHOOK = '${{ secrets.SLACK_MOBILE_TEAM_ALERTS_WEBHOOK_URL }}';

const INTEGRATION_IN_RELEASE = 'integration-test / integration-test';
const E2E_IN_RELEASE = 'verify-e2e / e2e-tests';
const PAPARAZZI_IN_RELEASE = 'paparazzi-verify / verify';

describe('the nightly run', () => {
  const nightly = [
    { file: 'integration-test.yml', job: 'integration-test', checks: 'integration tests', workflows: ['integration-test.yml', `manual-release.yml=${INTEGRATION_IN_RELEASE}`] },
    { file: 'paparazzi-verify.yml', job: 'verify', checks: 'Paparazzi screenshot tests', workflows: ['paparazzi-verify.yml', `manual-release.yml=${PAPARAZZI_IN_RELEASE}`] },
    { file: 'verify-e2e.yml', job: 'e2e-tests', checks: 'end-to-end tests', workflows: ['verify-e2e.yml', `manual-release.yml=${E2E_IN_RELEASE}`] },
  ];
  const workflowsInput = (notify: string): string[] => {
    const input = /^ {6}workflows: (?:\|\n((?: {8}.+\n)+)|(.+)$)/m.exec(notify);
    return input?.[1] !== undefined ? input[1].trimEnd().split('\n').map((line) => line.trim()) : [input?.[2] ?? ''];
  };

  it('runs the integration tests, the Paparazzi screenshot tests and the end-to-end tests at 07:17 UTC in clerk/clerk-android, and nothing in a fork', () => {
    for (const { file, job } of nightly) {
      assert.match(triggers(read(file)), /^ {2}schedule:\n {4}- cron: "17 7 \* \* \*"$/m, `${file} runs at 07:17 UTC`);
      const condition = /^ {4}if: (.+)$/m.exec(jobs(read(file)).get(job) ?? '')?.[1] ?? 'true';
      assert.deepEqual(
        [evaluate(condition, { event: 'schedule' }), evaluate(condition, { event: 'schedule', repository: 'someone/clerk-android' })],
        [true, false],
        file,
      );
    }
  });

  it('counts a release as the last passing run of the integration tests, the Paparazzi screenshot tests or the end-to-end tests only by the name their job has in the release', () => {
    const name = (text: string, id: string): string => /^ {4}name: (.+)$/m.exec(text)?.[1] ?? id;
    const inRelease = (file: string, job: string): string => {
      const [callerId, caller] = [...jobs(read('manual-release.yml'))].find(([, text]) => new RegExp(`^ {4}uses: \\./\\.github/workflows/${file.replaceAll('.', '\\.')}$`, 'm').test(text)) ?? ['', ''];
      return `${name(caller, callerId)} / ${name(jobs(read(file)).get(job) ?? '', job)}`;
    };
    assert.deepEqual(
      [inRelease('integration-test.yml', 'integration-test'), inRelease('paparazzi-verify.yml', 'verify'), inRelease('verify-e2e.yml', 'e2e-tests')],
      [INTEGRATION_IN_RELEASE, PAPARAZZI_IN_RELEASE, E2E_IN_RELEASE],
    );
  });

  it('posts to Slack when a scheduled run fails, times out, or is cancelled, and not when it passes, when a fork skipped it, when a person started it, or when a release ran it', () => {
    for (const { file, job, checks, workflows } of nightly) {
      const notify = jobs(read(file)).get('notify') ?? '';
      assert.match(notify, /^ {4}uses: \.\/\.github\/workflows\/notify-nightly-failure\.yml$/m, `${file} notifies Slack`);
      assert.match(notify, new RegExp(`^ {4}needs: ${job}$`, 'm'));
      assert.match(notify, new RegExp(`^ {6}checks: ${checks}$`, 'm'));
      assert.deepEqual(workflowsInput(notify), workflows, `${file} names the workflows whose runs count as passing`);
      assert.deepEqual(permissions(notify), READ_RUNS_AND_PULLS, `${file} grants the notify workflow what it reads`);
      const condition = /^ {4}if: (.+)$/m.exec(notify)![1]!;
      assert.deepEqual(
        [
          evaluate(condition, { event: 'schedule', needs: { [job]: 'failure' } }),
          evaluate(condition, { event: 'schedule', needs: { [job]: 'cancelled' } }),
          evaluate(condition, { event: 'schedule', needs: { [job]: 'success' } }),
          evaluate(condition, { event: 'schedule', needs: { [job]: 'skipped' } }),
          evaluate(condition, { event: 'schedule', needs: { [job]: 'cancelled' }, cancelled: true }),
          evaluate(condition, { event: 'workflow_dispatch', needs: { [job]: 'failure' } }),
          evaluate(condition, { event: 'release', needs: { [job]: 'failure' } }),
        ],
        [true, true, false, false, true, false, false],
        file,
      );
    }
  });

  it('passes the mobile team alerts webhook to the shared notify workflow', () => {
    for (const { file } of nightly) {
      const notify = jobs(read(file)).get('notify') ?? '';
      assert.equal(/^ {4}secrets:\n((?: {6}.+\n)+)/m.exec(notify)?.[1], `      SLACK_MOBILE_TEAM_ALERTS_WEBHOOK_URL: ${WEBHOOK}\n`, file);
    }
  });
});

describe('the Slack message about a failed nightly run', () => {
  const SINCE = 'Merged since the <https://github.com/clerk/clerk-android/actions/runs/41|last passing run> (2026-10-13 07:17 UTC):\n• <https://github.com/clerk/clerk-android/pull/1097|#1097 Restore Credentials> · swolfand';
  const HEADLINE = ':red_circle: clerk/clerk-android nightly integration tests failed · <https://github.com/clerk/clerk-android/actions/runs/123|View the run>';
  const notify = jobs(read('notify-nightly-failure.yml')).get('notify') ?? '';
  const post = notify.split(/\n(?= {6}- )/).find((step) => step.includes('- name: Notify Slack')) ?? '';

  function notified(webhook: string, script: 'lists' | 'fails' = 'lists'): { status: number | null; stdout: string; node: string[]; posted: unknown } {
    const step = post.slice(post.indexOf('run: |') + 'run: |'.length).split('\n').map((line) => line.trim()).join('\n');
    const work = mkdtempSync(join(tmpdir(), 'ci-nightly-notify-'));
    mkdirSync(join(work, 'bin'));
    writeFileSync(join(work, 'bin', 'curl'), `#!/bin/sh\nwhile [ "$#" -gt 1 ]; do [ "$1" = --data ] && printf '%s' "$2" > "${work}/posted"; shift; done\necho "$1" > "${work}/url"\n`, { mode: 0o755 });
    const node = script === 'lists' ? `printf '%s\\n' '${SINCE}'` : `echo 'GET /actions/workflows/integration-test.yml/runs answered 403' >&2; exit 1`;
    writeFileSync(join(work, 'bin', 'node'), `#!/bin/sh\nprintf '%s\\n' "$@" > "${work}/node"\n${node}\n`, { mode: 0o755 });
    const ran = spawnSync('bash', ['-c', step], {
      env: {
        PATH: `${join(work, 'bin')}:${process.env.PATH}`,
        SLACK_MOBILE_TEAM_ALERTS_WEBHOOK_URL: webhook,
        CHECKS: 'integration tests',
        WORKFLOWS: `integration-test.yml\nmanual-release.yml=${INTEGRATION_IN_RELEASE}\n`,
        GITHUB_SERVER_URL: 'https://github.com',
        GITHUB_REPOSITORY: 'clerk/clerk-android',
        GITHUB_RUN_ID: '123',
      },
      encoding: 'utf8',
      timeout: 20_000,
    });
    let posted: unknown = null;
    try {
      posted = { url: readFileSync(join(work, 'url'), 'utf8').trim(), payload: JSON.parse(readFileSync(join(work, 'posted'), 'utf8')) };
    } catch {}
    let args: string[] = [];
    try {
      args = readFileSync(join(work, 'node'), 'utf8').split('\n').slice(0, -1);
    } catch {}
    rmSync(work, { recursive: true, force: true });
    return { status: ran.status, stdout: ran.stdout, node: args, posted };
  }

  it('names the checks that failed and the repository, links the run, and lists the pull requests merged since the last passing run', () => {
    assert.deepEqual(notified('https://hooks.slack.test/T/B/x'), {
      status: 0,
      stdout: '',
      node: ['.github/scripts/nightly-failure-message.mjs', 'integration-test.yml', `manual-release.yml=${INTEGRATION_IN_RELEASE}`],
      posted: { url: 'https://hooks.slack.test/T/B/x', payload: { text: `${HEADLINE}\n${SINCE}`, unfurl_links: false, unfurl_media: false } },
    });
  });

  it('still names the checks and links the run when the list of pull requests cannot be made', () => {
    assert.deepEqual(notified('https://hooks.slack.test/T/B/x', 'fails'), {
      status: 0,
      stdout: '::warning::Could not list the pull requests merged since the last passing run; the message has only its first line\n',
      node: ['.github/scripts/nightly-failure-message.mjs', 'integration-test.yml', `manual-release.yml=${INTEGRATION_IN_RELEASE}`],
      posted: { url: 'https://hooks.slack.test/T/B/x', payload: { text: HEADLINE, unfurl_links: false, unfurl_media: false } },
    });
  });

  it('posts even when the checkout of the script fails, and reads the runs and pull requests with only read access', () => {
    assert.deepEqual(
      { post: /^ {8}if: (.+)$/m.exec(post)?.[1], token: /^ {10}GITHUB_TOKEN: (.+)$/m.exec(post)?.[1], permissions: permissions(notify) },
      { post: 'always()', token: '${{ github.token }}', permissions: READ_RUNS_AND_PULLS },
    );
  });

  it('is skipped with a warning when the webhook secret is empty', () => {
    assert.deepEqual(notified(''), { status: 0, stdout: '::warning::SLACK_MOBILE_TEAM_ALERTS_WEBHOOK_URL is not configured; skipping Slack notification\n', node: [], posted: null });
  });

  it('posts to the webhook its caller passes', () => {
    const shared = read('notify-nightly-failure.yml');
    assert.deepEqual(
      { declared: /^ {4}secrets:\n((?: {6}.+\n)+)/m.exec(shared.slice(0, shared.indexOf('\njobs:')))?.[1], read: /^ {10}SLACK_MOBILE_TEAM_ALERTS_WEBHOOK_URL: (.+)$/m.exec(post)?.[1] },
      { declared: '      SLACK_MOBILE_TEAM_ALERTS_WEBHOOK_URL:\n        required: false\n', read: WEBHOOK },
    );
  });

  it('is posted when the whole nightly run was cancelled, since the caller alone decides whether it runs', () => {
    const condition = /^ {4}if: (.+)$/m.exec(notify)?.[1] ?? 'true';
    assert.deepEqual([evaluate(condition, { event: 'schedule' }), evaluate(condition, { event: 'schedule', cancelled: true })], [true, true]);
  });
});

describe('a release', () => {
  const release = jobs(read('manual-release.yml'));

  const integration = jobs(read('integration-test.yml')).get('integration-test') ?? '';
  const keyCheck = integration.split(/\n(?= {6}- )/).find((step) => step.includes('- name: Check that the integration tests have a key')) ?? '';

  it('runs the integration tests with only the key they need, and asks them to require it', () => {
    assert.equal(
      release.get('integration-test'),
      '  integration-test:\n    uses: ./.github/workflows/integration-test.yml\n    with:\n      require_key: true\n    secrets:\n      CLERK_INTEGRATION_TEST_PK: ${{ secrets.CLERK_INTEGRATION_TEST_PK }}\n',
    );
  });

  it('runs the integration test job whether a published release or a person started the release workflow', () => {
    const condition = /^ {4}if: (.+)$/m.exec(integration)?.[1] ?? 'true';
    assert.deepEqual([evaluate(condition, { event: 'release' }), evaluate(condition, { event: 'workflow_dispatch' })], [true, true]);
  });

  it('fails the integration tests when the release passes an empty key, instead of passing with every test skipped', () => {
    const condition = /^ {8}if: (.+)$/m.exec(keyCheck)?.[1] ?? 'false';
    const required = /^ {6}require_key: (true|false)$/m.exec(release.get('integration-test') ?? '')?.[1] === 'true';
    assert.deepEqual(
      [evaluate(condition, { event: 'release', inputs: { require_key: required } }), evaluate(condition, { event: 'schedule' }), evaluate(condition, { event: 'pull_request' })],
      [true, false, false],
    );
    const script = keyCheck.slice(keyCheck.indexOf('run: |') + 'run: |'.length).split('\n').map((line) => line.trim()).join('\n');
    const checked = (key: string) => {
      const ran = spawnSync('bash', ['-e', '-c', script], { env: { PATH: process.env.PATH, CLERK_INTEGRATION_TEST_PK: key }, encoding: 'utf8', timeout: 20_000 });
      return { status: ran.status, stdout: ran.stdout };
    };
    assert.deepEqual(
      [checked(''), checked('pk_test_Y2xlcmsuZXhhbXBsZS5jb20k')],
      [
        { status: 1, stdout: '::error title=Integration tests::The CLERK_INTEGRATION_TEST_PK secret is empty, so every integration test would be skipped.\n' },
        { status: 0, stdout: '' },
      ],
    );
  });

  it('runs the Paparazzi screenshot tests with no secrets', () => {
    assert.equal(release.get('paparazzi-verify'), '  paparazzi-verify:\n    uses: ./.github/workflows/paparazzi-verify.yml\n');
  });

  it('creates no release and publishes nothing unless the integration tests and the Paparazzi screenshot tests pass', () => {
    assert.match(release.get('create-release') ?? '', /^ {4}needs: \[[^\]]*\bintegration-test\b[^\]]*\]$/m);
    assert.match(release.get('create-release') ?? '', /^ {4}needs: \[[^\]]*\bpaparazzi-verify\b[^\]]*\]$/m);
    const condition = /^ {4}if: (.+)$/m.exec(release.get('publish') ?? '')?.[1] ?? 'true';
    const needs = (integration: string, paparazzi: string) => ({ verify: 'success', 'verify-e2e': 'success', 'integration-test': integration, 'paparazzi-verify': paparazzi, 'create-release': 'skipped' });
    assert.deepEqual(
      [
        evaluate(condition, { event: 'release', needs: needs('success', 'success') }),
        evaluate(condition, { event: 'release', needs: needs('failure', 'success') }),
        evaluate(condition, { event: 'release', needs: needs('success', 'failure') }),
      ],
      [true, false, false],
    );
  });
});

describe('the Paparazzi screenshot tests', () => {
  const steps = (jobs(read('paparazzi-verify.yml')).get('verify') ?? '').split(/\n(?= {6}- )/);
  const SNAPSHOT_STEPS = ['Verify Paparazzi snapshots', 'Record Paparazzi snapshots', 'Upload the recorded snapshots'];
  const ran = (context: Context): string[] =>
    SNAPSHOT_STEPS.filter((name) => {
      const step = steps.find((text) => text.includes(`- name: ${name}\n`));
      return step !== undefined && evaluate(/^ {8}if: (.+)$/m.exec(step)?.[1] ?? 'true', context);
    });

  it('verify the snapshots every night, before each release, and when a person starts them, and record them on the runner only when that person asks', () => {
    assert.deepEqual(
      [
        ran({ event: 'schedule' }),
        ran({ event: 'release' }),
        ran({ event: 'workflow_dispatch' }),
        ran({ event: 'workflow_dispatch', inputs: { record: false } }),
        ran({ event: 'workflow_dispatch', inputs: { record: true } }),
      ],
      [
        ['Verify Paparazzi snapshots'],
        ['Verify Paparazzi snapshots'],
        ['Verify Paparazzi snapshots'],
        ['Verify Paparazzi snapshots'],
        ['Record Paparazzi snapshots', 'Upload the recorded snapshots'],
      ],
    );
  });

  it('are left out of the unit tests of a pull request, and run in full when this workflow verifies or records them', () => {
    const gradle = (text: string): string => /^ {8}run: (\.\/gradlew .+)$/m.exec(text)?.[1] ?? '';
    const unitTests = (jobs(read('android-test.yml')).get('test-module') ?? '').split(/\n(?= {6}- )/).find((text) => text.includes('- name: Run unit tests for ')) ?? '';
    const property = /providers\.gradleProperty\("(\w+)"\)\.orNull == "true"\) \{\n\s+useJUnit \{ excludeCategories\("com\.clerk\.base\.SnapshotTest"\) \}/.exec(readFileSync(join(WORKFLOWS, '..', '..', 'source', 'ui', 'build.gradle.kts'), 'utf8'))?.[1];
    assert.ok(property !== undefined, 'source/ui leaves out the SnapshotTest category when a Gradle property is true');
    assert.deepEqual(
      [gradle(unitTests), ...['Verify Paparazzi snapshots', 'Record Paparazzi snapshots'].map((name) => gradle(steps.find((text) => text.includes(`- name: ${name}\n`)) ?? ''))].map((command) => command.includes(`-P${property}=true`)),
      [true, false, false],
    );
  });

  it('upload the recorded images from the folder that holds the committed snapshots', () => {
    const upload = steps.find((text) => text.includes('- name: Upload the recorded snapshots\n')) ?? '';
    const path = /^ {10}path: (.+)$/m.exec(upload)?.[1] ?? '';
    assert.ok(path !== '' && readdirSync(join(WORKFLOWS, '..', '..', path)).some((file) => file.endsWith('.png')), `${path} holds the committed snapshots`);
  });
});

describe('the Slack posts of the workflows', () => {
  it('go to the mobile team alerts channel, so no workflow reads the old SLACK_WEBHOOK_URL secret', () => {
    assert.ok(files.includes('manual-release.yml'), files.join(', '));
    assert.deepEqual(files.filter((file) => /\bSLACK_WEBHOOK_URL\b/.test(read(file))), []);
  });
});

describe('the Slack announcement of a release', () => {
  const step = (jobs(read('manual-release.yml')).get('notify-slack') ?? '').split(/\n(?= {6}- )/).find((text) => text.includes('- name: Notify Slack')) ?? '';

  function announced(webhook: string): { status: number | null; stdout: string; posted: unknown } {
    const run = step.slice(step.indexOf('run: |') + 'run: |'.length);
    const work = mkdtempSync(join(tmpdir(), 'release-notify-'));
    mkdirSync(join(work, 'bin'));
    writeFileSync(join(work, 'bin', 'curl'), `#!/bin/sh\nwhile [ "$#" -gt 1 ]; do [ "$1" = --data ] && printf '%s' "$2" > "${work}/posted"; shift; done\necho "$1" > "${work}/url"\n`, { mode: 0o755 });
    const ran = spawnSync('bash', ['-c', run], {
      env: {
        PATH: `${join(work, 'bin')}:${process.env.PATH}`,
        SLACK_BOT_TOKEN: '',
        SLACK_CHANNEL_ID: '',
        SLACK_MOBILE_TEAM_ALERTS_WEBHOOK_URL: webhook,
        RELEASE_TAG: 'v1.2.3',
        RELEASE_URL: 'https://github.com/clerk/clerk-android/releases/tag/v1.2.3',
        PR_URL: '',
      },
      encoding: 'utf8',
      timeout: 20_000,
    });
    let posted: unknown = null;
    try {
      posted = { url: readFileSync(join(work, 'url'), 'utf8').trim(), payload: JSON.parse(readFileSync(join(work, 'posted'), 'utf8')).text };
    } catch {}
    rmSync(work, { recursive: true, force: true });
    return { status: ran.status, stdout: ran.stdout, posted };
  }

  it('reads the mobile team alerts webhook', () => {
    assert.equal(/^ {10}SLACK_MOBILE_TEAM_ALERTS_WEBHOOK_URL: (.+)$/m.exec(step)?.[1], WEBHOOK);
  });

  it('posts the release to that webhook when the repository has no Slack bot token', () => {
    assert.deepEqual(announced('https://hooks.slack.test/T/B/x'), {
      status: 0,
      stdout: '::warning::SLACK_BOT_TOKEN and SLACK_CHANNEL_ID are not configured; falling back to webhook release notification\n',
      posted: { url: 'https://hooks.slack.test/T/B/x', payload: 'Clerk Android SDK v1.2.3 has been released' },
    });
  });
});
