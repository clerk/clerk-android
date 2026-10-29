import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { main } from './renovate-approval.mjs';

const REPO = 'clerk/clerk-android';
const API = 'https://api.github.test';

const renovatePull = { user: { login: 'renovate[bot]' }, head: { ref: 'renovate/node-26.x', sha: 'abc123', repo: { full_name: REPO } } };
const push = (login, after) => ({ actor: { login }, after });

function fakeGitHub({ pull = renovatePull, activity = [] }) {
  const reviews = [];
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
      return reply(activity);
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
  await main(
    { GITHUB_API_URL: API, GITHUB_REPOSITORY: REPO, GITHUB_TOKEN: 'test-token', PR_NUMBER: '7' },
    { fetch, out: { write: line => lines.push(line) } },
  );
  return { reviews, output: lines.join('') };
}

describe('renovate approval', () => {
  it('approves the head commit when Renovate pushed it', async () => {
    const { reviews, output } = await run({ activity: [push('renovate[bot]', 'abc123')] });
    assert.deepEqual(reviews.map(({ commit_id, event }) => ({ commit_id, event })), [{ commit_id: 'abc123', event: 'APPROVE' }]);
    assert.match(output, /Approved abc123/);
  });

  it('leaves the pull request alone when someone else pushed the head commit', async () => {
    const { reviews, output } = await run({ activity: [push('writer', 'abc123'), push('renovate[bot]', 'old456')] });
    assert.deepEqual(reviews, []);
    assert.match(output, /needs a person's review/);
  });

  it('leaves the pull request alone when the push is not in the activity log', async () => {
    const { reviews } = await run({ activity: [push('renovate[bot]', 'old456')] });
    assert.deepEqual(reviews, []);
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
