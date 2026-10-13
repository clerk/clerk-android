export const PLATFORM_CREDENTIAL_VARIABLES = ['CLERK_PLATFORM_API_KEY', 'CLERK_PLATFORM_API_KEY_FILE', 'VERIFY_PLATFORM_KEY_REFERENCE'];
export const AGENT_CREDENTIAL_VARIABLES = ['AI_GATEWAY_API_KEY', 'AI_GATEWAY_API_KEY_FILE'];
export const INSTANCE_SECRET_VARIABLES = ['CLERK_SECRET_KEY'];

export function supportsNode(version) {
  const [major, minor] = version.split('.').map(Number);
  return major === 24 && minor >= 8;
}

export async function ensureRuntime() {
  if (!supportsNode(process.versions.node)) {
    const message = `this CLI needs Node 24.8.0 or newer on 24 and this is Node ${process.versions.node}`;
    const fix = 'install Node 24.8.0 or newer on 24 (nvm install 24 && nvm use 24) and rerun';
    if (process.argv.includes('--json')) console.log(JSON.stringify({ ok: false, error: { code: 'NOT_READY', message, fix, retryable: false } }));
    else console.error(`FAIL  node  ${message}\n      fix: ${fix}`);
    process.exit(3);
  }
}
