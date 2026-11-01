import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { main, parseProtectedPaths, protectedFiles } from './code-owner-approval.mjs';

const REPO = 'clerk/clerk-ios';
const API = 'https://api.github.test';

const PROTECTED_PATHS = `# High-risk paths
/.github/workflows/
/Package.swift
/Sources/ClerkKit/Identity/ # session identity
*.pem
`;

const RENOVATE_PATHS = `# Renovate's own updates
/Package.swift
`;

function fakeGitHub({ list = PROTECTED_PATHS, renovateList = RENOVATE_PATHS, files, changed = files.length, reviews = [], roles = {}, pull = {}, activity = [] }) {
  const reply = body => ({ ok: true, status: 200, json: async () => body, text: async () => JSON.stringify(body) });
  const notFound = { ok: false, status: 404, json: async () => ({}), text: async () => 'Not Found' };
  return async (url, { headers }) => {
    assert.equal(headers.authorization, 'Bearer test-token');
    const { pathname, searchParams } = new URL(url);
    const path = pathname.replace(`/repos/${REPO}`, '');
    if (path === '/contents/.github/protected-paths') {
      assert.equal(searchParams.get('ref'), 'main');
      return list === null ? notFound : reply({ content: Buffer.from(list).toString('base64') });
    }
    if (path === '/contents/.github/renovate-paths') {
      assert.equal(searchParams.get('ref'), 'main');
      return renovateList === null ? notFound : reply({ content: Buffer.from(renovateList).toString('base64') });
    }
    if (path === '/pulls/7') {
      return reply({ changed_files: changed, head: { sha: HEAD }, ...pull });
    }
    if (path === '/activity') {
      assert.equal(searchParams.get('ref'), 'refs/heads/renovate/node-26.x');
      return reply(activity);
    }
    const page = Number(searchParams.get('page'));
    const slice = items => items.slice((page - 1) * 100, page * 100);
    if (path === '/pulls/7/files') {
      return reply(slice(files));
    }
    if (path === '/pulls/7/reviews') {
      return reply(slice(reviews));
    }
    const match = /^\/collaborators\/([^/]+)\/permission$/.exec(path);
    if (match) {
      return match[1] in roles ? reply({ role_name: roles[match[1]] }) : notFound;
    }
    throw new Error(`unexpected request ${path}`);
  };
}

async function run(github, author = 'outside-dev') {
  const lines = [];
  const pass = await main(
    { GITHUB_API_URL: API, GITHUB_REPOSITORY: REPO, GITHUB_TOKEN: 'test-token', PR_NUMBER: '7', PR_AUTHOR: author, BASE_REF: 'main' },
    { fetch: fakeGitHub(github), out: { write: line => lines.push(line) } },
  );
  return { pass, output: lines.join('') };
}

const HEAD = 'c14cacc2763d2dfc271d9c286b09376a90c5b9ff';
const review = (login, state, commit = HEAD) => ({ user: { login }, state, commit_id: commit });

const renovatePull = { user: { login: 'renovate[bot]' }, head: { ref: 'renovate/node-26.x', sha: HEAD, repo: { full_name: REPO } } };
const push = (login, after) => ({ actor: { login }, after });

describe('protectedFiles', () => {
  const patterns = parseProtectedPaths(PROTECTED_PATHS);

  it('matches anchored directories, exact files, and unanchored globs', () => {
    assert.deepEqual(
      protectedFiles(patterns, ['.github/workflows/pr-ci.yml', 'Package.swift', 'Tools/Nested/Package.swift', 'certs/dev.pem', 'Sources/ClerkKit/Clerk.swift', '.github/workflowsX/a.yml', 'Sources/ClerkKit/Identity/Store.swift']),
      ['.github/workflows/pr-ci.yml', 'Package.swift', 'certs/dev.pem', 'Sources/ClerkKit/Identity/Store.swift'],
    );
  });

  it('matches only direct children for a wildcard last segment, as CODEOWNERS does', () => {
    assert.deepEqual(protectedFiles(parseProtectedPaths('docs/*\n'), ['docs/a.md', 'docs/private/b.md']), ['docs/a.md']);
  });
});

describe('code owner approval', () => {
  const coderabbit = review('coderabbitai[bot]', 'APPROVED');
  const ordinary = [{ filename: 'Sources/ClerkKit/Clerk.swift' }];
  const workflow = [{ filename: '.github/workflows/pr-ci.yml' }];
  const manifest = [{ filename: 'Package.swift' }];

  it('passes a Team Mobile author on ordinary files once CodeRabbit approves', async () => {
    const { pass, output } = await run({ files: ordinary, reviews: [coderabbit], roles: { mikepitre: 'admin' } }, 'mikepitre');
    assert.equal(pass, true);
    assert.match(output, /mikepitre has maintain or admin access, and CodeRabbit approved/);
  });

  it('fails a Team Mobile author until CodeRabbit approves, as when it rates the risk high', async () => {
    const { pass, output } = await run({ files: ordinary, reviews: [review('coderabbitai[bot]', 'COMMENTED')], roles: { mikepitre: 'admin' } }, 'mikepitre');
    assert.equal(pass, false);
    assert.match(output, /CodeRabbit hasn't approved this commit/);
  });

  it('fails a Team Mobile author when CodeRabbit approved an earlier commit', async () => {
    const stale = review('coderabbitai[bot]', 'APPROVED', 'e225ce56d448093f7910a36cf47bcc9a664e04d0');
    const { pass } = await run({ files: ordinary, reviews: [stale], roles: { mikepitre: 'admin' } }, 'mikepitre');
    assert.equal(pass, false);
  });

  it('fails a Team Mobile author on protected files even after CodeRabbit approves', async () => {
    const { pass, output } = await run({ files: workflow, reviews: [coderabbit], roles: { swolfand: 'maintain' } }, 'swolfand');
    assert.equal(pass, false);
    assert.match(output, /This changes 1 protected file\(s\) \(\.github\/workflows\/pr-ci\.yml\), so it needs an approval from another Team Mobile member/);
  });

  it('passes a Team Mobile author on protected files once another member approves', async () => {
    const { pass, output } = await run({ files: workflow, reviews: [review('seanperez29', 'APPROVED')], roles: { swolfand: 'maintain', seanperez29: 'admin' } }, 'swolfand');
    assert.equal(pass, true);
    assert.match(output, /seanperez29 approved/);
  });

  it('fails an author with write access on ordinary files even after CodeRabbit and a writer approve', async () => {
    const { pass, output } = await run({
      files: ordinary,
      reviews: [coderabbit, review('writer', 'APPROVED')],
      roles: { 'outside-dev': 'write', writer: 'write' },
    });
    assert.equal(pass, false);
    assert.match(output, /^::error title=Code owner approval::outside-dev doesn't have maintain or admin access/);
  });

  it('fails a contributor with no access to the repository', async () => {
    const { pass } = await run({ files: ordinary, reviews: [coderabbit] }, 'fork-contributor');
    assert.equal(pass, false);
  });

  it('passes an author with write access once Team Mobile approves', async () => {
    const { pass } = await run({ files: workflow, reviews: [review('wobsoriano', 'APPROVED')], roles: { 'outside-dev': 'write', wobsoriano: 'maintain' } });
    assert.equal(pass, true);
  });

  it('ignores an approval that was later dismissed or followed by a change request', async () => {
    const { pass } = await run({
      files: ordinary,
      reviews: [review('swolfand', 'APPROVED'), review('swolfand', 'DISMISSED'), review('seanperez29', 'APPROVED'), review('seanperez29', 'COMMENTED'), review('seanperez29', 'CHANGES_REQUESTED')],
      roles: { swolfand: 'maintain', seanperez29: 'admin' },
    });
    assert.equal(pass, false);
  });

  it('fails when Team Mobile approved an earlier commit', async () => {
    const stale = review('seanperez29', 'APPROVED', 'e225ce56d448093f7910a36cf47bcc9a664e04d0');
    const { pass, output } = await run({ files: workflow, reviews: [stale], roles: { swolfand: 'maintain', seanperez29: 'admin' } }, 'swolfand');
    assert.equal(pass, false);
    assert.match(output, /so it needs an approval from another Team Mobile member/);
  });

  it('keeps an approval when a later review only comments', async () => {
    const { pass } = await run({
      files: ordinary,
      reviews: [review('seanperez29', 'APPROVED'), review('seanperez29', 'COMMENTED')],
      roles: { seanperez29: 'admin' },
    });
    assert.equal(pass, true);
  });

  it('passes Renovate on a protected file that renovate-paths lists', async () => {
    const { pass, output } = await run({ files: manifest, pull: renovatePull, activity: [push('renovate[bot]', HEAD)] }, 'renovate[bot]');
    assert.equal(pass, true);
    assert.match(output, new RegExp(`Renovate pushed ${HEAD}, and .github/renovate-paths lists every protected file it changes`));
  });

  it('passes Renovate on ordinary files without a CodeRabbit approval', async () => {
    const { pass } = await run({ files: ordinary, pull: renovatePull, activity: [push('renovate[bot]', HEAD)] }, 'renovate[bot]');
    assert.equal(pass, true);
  });

  it('fails Renovate on a protected file that renovate-paths does not list', async () => {
    const { pass, output } = await run(
      { files: [...manifest, ...workflow], pull: renovatePull, activity: [push('renovate[bot]', HEAD)] },
      'renovate[bot]',
    );
    assert.equal(pass, false);
    assert.match(output, /changes 1 protected file\(s\) that \.github\/renovate-paths doesn't list \(\.github\/workflows\/pr-ci\.yml\)/);
  });

  it('fails Renovate on every protected file when the base branch has no renovate-paths list', async () => {
    const { pass } = await run({ renovateList: null, files: manifest, pull: renovatePull, activity: [push('renovate[bot]', HEAD)] }, 'renovate[bot]');
    assert.equal(pass, false);
  });

  it('passes Renovate on a protected file once Team Mobile approves', async () => {
    const { pass } = await run(
      { files: [{ filename: 'Sources/ClerkKit/Identity/Store.swift' }], reviews: [review('seanperez29', 'APPROVED')], roles: { seanperez29: 'admin' }, pull: renovatePull, activity: [push('renovate[bot]', HEAD)] },
      'renovate[bot]',
    );
    assert.equal(pass, true);
  });

  it('fails when someone else pushed the head commit of a Renovate pull request', async () => {
    const { pass } = await run(
      {
        files: workflow,
        pull: renovatePull,
        activity: [push('writer', HEAD), push('renovate[bot]', 'e225ce56d448093f7910a36cf47bcc9a664e04d0')],
        roles: { writer: 'write' },
      },
      'renovate[bot]',
    );
    assert.equal(pass, false);
  });

  it('counts the old path of a renamed file', async () => {
    const { pass } = await run({ files: [{ filename: 'Old/Identity.swift', previous_filename: 'Sources/ClerkKit/Identity/Store.swift' }], reviews: [coderabbit], roles: { mikepitre: 'admin' } }, 'mikepitre');
    assert.equal(pass, false);
  });

  it('reads every page of changed files', async () => {
    const files = Array.from({ length: 150 }, (_, i) => ({ filename: `Sources/File${i}.swift` }));
    files.push({ filename: '.github/workflows/late.yml' });
    const { pass } = await run({ files, reviews: [coderabbit], roles: { mikepitre: 'admin' } }, 'mikepitre');
    assert.equal(pass, false);
  });

  it('fails when GitHub lists fewer files than the pull request changed', async () => {
    const { pass, output } = await run({ files: ordinary, changed: 3001, reviews: [coderabbit], roles: { mikepitre: 'admin' } }, 'mikepitre');
    assert.equal(pass, false);
    assert.match(output, /listed 1 of the 3001 changed files/);
  });

  it('treats every file as unprotected when the base branch has no protected-paths list', async () => {
    const { pass } = await run({ list: null, files: workflow, reviews: [coderabbit], roles: { mikepitre: 'admin' } }, 'mikepitre');
    assert.equal(pass, true);
  });
});
