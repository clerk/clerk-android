import fs from 'node:fs';
import { fileURLToPath } from 'node:url';

import { client } from './github.mjs';

const RENOVATE = 'renovate[bot]';
// The pull_request event can arrive before the activity log records the push that caused it.
const ACTIVITY_LOG_RETRY_DELAYS_MS = [5_000, 15_000, 30_000];

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

async function headPusher(request, pull) {
  const activity = await request(`/activity?ref=${encodeURIComponent(`refs/heads/${pull.head.ref}`)}&per_page=100`);
  return activity.find(entry => entry.after === pull.head.sha)?.actor?.login;
}

// Anyone with write access can push a commit that names Renovate as its author, but the activity log records who pushed it.
async function builtByRenovate(request, pull, repo, wait = sleep) {
  if (pull.user?.login !== RENOVATE || pull.head.repo?.full_name !== repo) {
    return false;
  }
  let pusher = await headPusher(request, pull);
  for (const delay of ACTIVITY_LOG_RETRY_DELAYS_MS) {
    if (pusher !== undefined) {
      break;
    }
    await wait(delay);
    pusher = await headPusher(request, pull);
  }
  return pusher === RENOVATE;
}

async function approveRenovate({ fetch, api, token, repo, number, wait }) {
  const request = client({ fetch, api, token, repo });
  const pull = await request(`/pulls/${number}`);
  if (!(await builtByRenovate(request, pull, repo, wait))) {
    return `Renovate didn't push ${pull.head.sha}, so this pull request needs a person's review.`;
  }
  await request(`/pulls/${number}/reviews`, {
    method: 'POST',
    body: {
      commit_id: pull.head.sha,
      event: 'APPROVE',
      body: 'Renovate pushed this commit. The merge queue takes it once the required checks pass.',
    },
  });
  return `Approved ${pull.head.sha}, which Renovate pushed.`;
}

async function main(env, { fetch = globalThis.fetch, out = process.stdout, wait = sleep } = {}) {
  const missing = ['GITHUB_REPOSITORY', 'GITHUB_TOKEN', 'PR_NUMBER'].filter(name => !env[name]);
  if (missing.length > 0) {
    throw new Error(`missing ${missing.join(', ')}`);
  }
  const message = await approveRenovate({
    fetch,
    api: env.GITHUB_API_URL || 'https://api.github.com',
    token: env.GITHUB_TOKEN,
    repo: env.GITHUB_REPOSITORY,
    number: env.PR_NUMBER,
    wait,
  });
  out.write(`${message}\n`);
}

export { builtByRenovate, main };

if (process.argv[1] !== undefined && fs.realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    await main(process.env);
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  }
}
