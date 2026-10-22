import '@clerk/internal-mobile-e2e/testing/git-env';
import assert from 'node:assert/strict';
import { execFileSync, spawn, type ChildProcess } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import http from 'node:http';
import net from 'node:net';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';
import { workingPatch } from '@clerk/internal-mobile-e2e/core/remote/backend';
import { sha256Hex, type SessionHealth, type SessionRequest } from '@clerk/internal-mobile-e2e/core/remote/protocol';
import { BUILD_INPUTS } from '../src/host-app.ts';

const SESSION_AGENT = fileURLToPath(import.meta.resolve('@clerk/internal-mobile-e2e/core/remote/session-agent'));
const token = 'b'.repeat(64);
const base: SessionRequest = { v: 1, session: 'and0000aa', owner: '0123456789ab', platform: 'android', runner: 'test-runner', device: 'Test Phone', sha: null, idleMinutes: 5, capMinutes: 10, tokenSha256: sha256Hex(token) };
const children: ChildProcess[] = [];
const servers: http.Server[] = [];
after(() => {
  children.forEach((child) => child.kill('SIGKILL'));
  servers.forEach((server) => server.close());
});

const sleep = (ms: number) => new Promise((done) => setTimeout(done, ms));
async function until<T>(read: () => Promise<T | null> | T | null, what: string, ms = 60_000): Promise<T> {
  const deadline = Date.now() + ms;
  for (;;) {
    const value = await read();
    if (value !== null) return value;
    if (Date.now() > deadline) throw new Error(`timed out waiting for ${what}`);
    await sleep(100);
  }
}

async function freePort(): Promise<number> {
  const server = net.createServer();
  await new Promise<void>((listening) => server.listen(0, '127.0.0.1', listening));
  const { port } = server.address() as net.AddressInfo;
  await new Promise((closed) => server.close(closed));
  return port;
}

function repoWithCommit(): { dir: string; commit(text: string): string; git(...args: string[]): string } {
  const dir = mkdtempSync(join(tmpdir(), 'verify-agent-repo-'));
  const git = (...args: string[]) => execFileSync('git', ['-c', 'user.name=t', '-c', 'user.email=t@example.com', ...args], { cwd: dir, encoding: 'utf8' }).trim();
  git('init', '-q');
  const commit = (text: string) => {
    writeFileSync(join(dir, 'app.txt'), text);
    git('add', '-A');
    git('commit', '-q', '-m', text);
    return git('rev-parse', 'HEAD');
  };
  commit('one');
  git('remote', 'add', 'origin', dir);
  return { dir, commit, git };
}

async function gitHubAt(sha: string): Promise<Readonly<Record<string, string>>> {
  const server = http.createServer((req, res) => {
    const path = (req.url ?? '').replace(/^\/repos\/clerk\/sample/, '');
    const found = path === '' ? { default_branch: 'main' } : path === '/git/ref/heads/feature/change' ? { object: { sha } } : path === `/compare/${sha}...${sha}` ? { status: 'identical' } : null;
    res.writeHead(found === null ? 404 : 200, { 'content-type': 'application/json' }).end(JSON.stringify(found ?? { message: 'Not Found' }));
  });
  servers.push(server);
  await new Promise<void>((listening) => server.listen(0, '127.0.0.1', listening));
  const { port } = server.address() as net.AddressInfo;
  return { GITHUB_API_URL: `http://127.0.0.1:${port}`, GITHUB_REPOSITORY: 'clerk/sample', GITHUB_SHA: sha, GITHUB_REF: 'refs/heads/feature/change', GITHUB_TOKEN: 'ghs_unitTestOnly', VERIFY_SESSION_DEFAULT_BRANCH: 'main' };
}

async function startAgent(request: SessionRequest, cwd: string, env: Readonly<Record<string, string>>) {
  const work = mkdtempSync(join(tmpdir(), 'verify-agent-'));
  const port = await freePort();
  const bin = join(work, 'bin');
  mkdirSync(bin);
  writeFileSync(join(bin, 'cloudflared'), `#!/bin/sh\necho 'INF |  https://fake-session-host.trycloudflare.com  |' >&2\nexec sleep 3600\n`, { mode: 0o755 });
  const child = spawn(process.execPath, [SESSION_AGENT, 'serve'], {
    cwd,
    env: {
      PATH: `${bin}:/usr/bin:/bin`,
      VERIFY_SESSION_REQUEST: JSON.stringify(request),
      VERIFY_SESSION_WORK: work,
      VERIFY_SESSION_PORT: String(port),
      VERIFY_SESSION_AGENT_PORT: String(await freePort()),
      VERIFY_SESSION_CLOUDFLARED: join(bin, 'cloudflared'),
      VERIFY_SESSION_AGENT_DEVICE: join(bin, 'agent-device'),
      VERIFY_SESSION_SKIP_DNS: '1',
      VERIFY_SESSION_DEVICE_ID: 'emulator-5560',
      VERIFY_SESSION_DEVICE_NAME: 'Test Phone',
      VERIFY_SESSION_DEVICE_MODULE: 'device.ts',
      ...env,
    },
    stdio: 'ignore',
  });
  children.push(child);
  const exited = new Promise<number>((done) => child.on('close', (code) => done(code ?? 1)));
  const call = (path: string, init: { method?: string; bearer?: string | null; body?: string } = {}) =>
    fetch(`http://127.0.0.1:${port}${path}`, { method: init.method ?? 'GET', headers: init.bearer === null ? {} : { Authorization: `Bearer ${init.bearer ?? token}` }, ...(init.body === undefined ? {} : { body: init.body }) });
  await until(() => (existsSync(join(work, 'tunnel')) ? true : null), 'the tunnel file');
  await until(() => call('/__sim/health', { bearer: null }).then((r) => r.status).catch(() => null), 'the agent to listen');
  const health = async () => (await (await call('/__sim/health')).json()) as SessionHealth;
  return { call, health, exited };
}

describe('a session on this repository', () => {
  it('refuses changes to a path that only begins with the name of an app source, such as e2e-tests beside e2e', async () => {
    assert.ok(BUILD_INPUTS.includes('e2e'));
    const repo = repoWithCommit();
    writeFileSync(join(repo.dir, 'device.ts'), `export default () => ({ inputs: ${JSON.stringify(BUILD_INPUTS)}, build: (work) => [{ command: process.execPath, args: ['-e', "require('fs').writeFileSync(require('path').join(process.argv[1], 'built'), 'built')", work] }] });\n`);
    const started = repo.commit('two');
    const agent = await startAgent({ ...base, sha: started }, repo.dir, await gitHubAt(started));
    const settled = (patch: string | null) => until(async () => { const b = (await agent.health()).build; return b.state !== 'building' && b.state !== 'none' && b.patch === patch ? b : null; }, `the build with ${patch ?? 'no changes'}`);
    assert.equal((await settled(null)).state, 'built');

    const client = mkdtempSync(join(tmpdir(), 'verify-agent-client-'));
    execFileSync('git', ['clone', '-q', repo.dir, client]);
    mkdirSync(join(client, 'e2e-tests'));
    writeFileSync(join(client, 'e2e-tests', 'recipe.ts'), 'a new file beside the e2e app');
    const changes = workingPatch(client, ['.'])!;
    assert.equal((await agent.call(`/__sim/build?sha=${started}&patch=${changes.sha256}`, { method: 'POST', body: JSON.stringify({ patch: changes.bytes.toString('base64') }) })).status, 202);
    const refused = await settled(changes.sha256);
    assert.equal(refused.state === 'failed' ? refused.tail.split('\n').at(-1) : refused.state, `the uncommitted changes ${changes.sha256.slice(0, 12)} change e2e-tests/recipe.ts, and a session builds changes only to ${BUILD_INPUTS.join(', ')}. Commit and push that change, then run again.`);
    assert.equal(repo.git('status', '--porcelain'), '', 'nothing of the changes was applied');
    await agent.call('/__sim/stop', { method: 'POST' });
    await agent.exited;
  });
});
