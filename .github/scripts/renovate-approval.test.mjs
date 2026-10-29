import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { main } from './renovate-approval.mjs';

const REPO = 'clerk/clerk-android';
const API = 'https://api.github.test';

const renovatePull = { user: { login: 'renovate[bot]' }, head: { ref: 'renovate/node-26.x', sha: 'abc123', repo: { full_name: REPO } } };
const push = (login, after) => ({ actor: { login }, after });

function fakeGitHub({ pull = renovatePull, activity = [], unloggedReads = 0 }) {
  const reviews = [];
  let activityReads = 0;
  const reply = body => ({ ok: true, status: 200, json: async () => body, text: async () => JSON.stringify(body) });
  const fetch = async (url, { method, headers, body }) => {
    assert.equal(headers.authorization, 'Bearer test-token');
    const { pathname, searchParams } = new URL(url);
    const path = pathname.replace(`/repos/${REPO}`, '');
    if (method === 'GET' && path === '/pulls/7') {
      return reply(pull);
    }
    if (method === 'GET' && path === '/activity') {
      assert.equal(searchParams.get('ref'), 'refs/heads/renovate/node-26.x');
      activityReads += 1;
      return reply(activityReads > unloggedReads ? activity : []);
    }
    if (method === 'POST' && path === '/pulls/7/reviews') {
      reviews.push(JSON.parse(body));
      return reply({});
    }
    throw new Error(`unexpected request ${method} ${path}`);
  };
  return { fetch, reviews };
}

async function run(github) {
  const { fetch, reviews } = fakeGitHub(github);
  const lines = [];
  const waits = [];
  await main(
    { GITHUB_API_URL: API, GITHUB_REPOSITORY: REPO, GITHUB_TOKEN: 'test-token', PR_NUMBER: '7' },
    { fetch, out: { write: line => lines.push(line) }, wait: async ms => waits.push(ms) },
  );
  return { reviews, output: lines.join(''), waits };
}

describe('renovate approval', () => {
  it('approves the head commit when Renovate pushed it', async () => {
    const { reviews, output } = await run({ activity: [push('renovate[bot]', 'abc123')] });
    assert.deepEqual(reviews.map(({ commit_id, event }) => ({ commit_id, event })), [{ commit_id: 'abc123', event: 'APPROVE' }]);
    assert.match(output, /Approved abc123/);
  });

  it('waits for the activity log to record the push', async () => {
    const { reviews, waits } = await run({ activity: [push('renovate[bot]', 'abc123')], unloggedReads: 2 });
    assert.deepEqual(reviews.map(({ commit_id }) => commit_id), ['abc123']);
    assert.deepEqual(waits, [5_000, 15_000]);
  });

  it('leaves the pull request alone when someone else pushed the head commit', async () => {
    const { reviews, output, waits } = await run({ activity: [push('writer', 'abc123'), push('renovate[bot]', 'old456')] });
    assert.deepEqual(reviews, []);
    assert.match(output, /needs a person's review/);
    assert.deepEqual(waits, []);
  });

  it('leaves the pull request alone when the push never reaches the activity log', async () => {
    const { reviews, waits } = await run({ activity: [push('renovate[bot]', 'old456')] });
    assert.deepEqual(reviews, []);
    assert.deepEqual(waits, [5_000, 15_000, 30_000]);
  });

  it('leaves a pull request from another author alone', async () => {
    const { reviews } = await run({ pull: { ...renovatePull, user: { login: 'writer' } }, activity: [push('renovate[bot]', 'abc123')] });
    assert.deepEqual(reviews, []);
  });

  it('leaves a pull request from a fork alone', async () => {
    const fork = { ...renovatePull, head: { ...renovatePull.head, repo: { full_name: 'someone/clerk-android' } } };
    const { reviews } = await run({ pull: fork, activity: [push('renovate[bot]', 'abc123')] });
    assert.deepEqual(reviews, []);
  });
});
