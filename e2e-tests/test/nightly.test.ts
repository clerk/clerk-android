import '@clerk/internal-mobile-e2e/testing/git-env';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, it } from 'node:test';

const workflow = readFileSync(join(import.meta.dirname, '..', '..', '.github', 'workflows', 'verify-e2e.yml'), 'utf8');
const triggers = workflow.slice(workflow.indexOf('\non:\n'), workflow.indexOf('\npermissions:'));
const jobs = workflow.split(/\n(?= {2}[\w-]+:\n)/);
const job = (name: string): string => jobs.find((text) => text.startsWith(`  ${name}:\n`)) ?? '';

function runs(context: { event: string; repository?: string }): boolean {
  const condition = (/^ {4}if: (.+)$/m.exec(job('e2e-tests'))?.[1] ?? 'true')
    .replaceAll('github.event_name', JSON.stringify(context.event))
    .replaceAll('github.repository', JSON.stringify(context.repository ?? 'clerk/clerk-android'))
    .replaceAll(' != ', ' !== ')
    .replaceAll(' == ', ' === ');
  return new Function(`return ${condition};`)() as boolean;
}

describe('when the end-to-end tests run', () => {
  it('run every night at 07:17 UTC, before each release, and when a person starts them', () => {
    assert.match(triggers, /^ {2}schedule:\n {4}- cron: "17 7 \* \* \*"$/m);
    assert.match(triggers, /^ {2}workflow_call:$/m);
    assert.match(triggers, /^ {2}workflow_dispatch:$/m);
  });

  it('run on the schedule only in clerk/clerk-android, so that the nightly run of a fork does nothing', () => {
    assert.ok(job('e2e-tests').length > 0, 'the workflow has an e2e-tests job');
    assert.deepEqual(
      [
        runs({ event: 'schedule' }),
        runs({ event: 'schedule', repository: 'someone/clerk-android' }),
        runs({ event: 'workflow_dispatch' }),
        runs({ event: 'workflow_dispatch', repository: 'someone/clerk-android' }),
      ],
      [true, false, true, true],
    );
  });
});

describe('a step that hangs', () => {
  it('ends at the time limit of its own step, well before the time limit of the job', () => {
    const e2e = job('e2e-tests');
    const steps = e2e.split(/\n(?= {6}- name: )/).filter((step) => /"\$CLI" (up|run) /.test(step));
    assert.deepEqual(
      {
        job: Number(/^ {4}timeout-minutes: (\d+)$/m.exec(e2e)?.[1]),
        steps: steps.map((step) => [/- name: (.+)/.exec(step)?.[1], Number(/^ {8}timeout-minutes: (\d+)$/m.exec(step)?.[1])]),
      },
      { job: 60, steps: [['Build the e2e host and boot an emulator', 15], ['Run the end-to-end tests', 35]] },
    );
  });
});
