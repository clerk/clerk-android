import { test, expect } from '../../fixtures.ts';

const E2E_APP_VERSION_NAME = '1.0';
const E2E_APP_VERSION_CODE = '1';
const ABOVE_E2E_APP_VERSION_CODE = '2';
const UPDATE_URL = 'https://play.google.com/store/apps/details?id=com.clerk.e2e';

test('a signed-in user on a build below the minimum sees the update screen, and is still signed in once the minimum is lowered', async ({ host, screen }) => {
  const title = screen.getByText('Update required');
  const user = await host.seedUser();
  await host.launch({ signedInAs: user });
  const sessionId = (await host.app.sessionId.textContent()) ?? '';
  expect(sessionId).not.toBe('');
  await host.evidence('the update screen covers the app, and the same session returns once the minimum is lowered', async () => {
    await host.useMinimumAppVersion({ minimumVersion: ABOVE_E2E_APP_VERSION_CODE, updateUrl: UPDATE_URL });
    await host.launch({ keepStorage: true, landsOn: title });
    await expect(screen.getByText(E2E_APP_VERSION_NAME)).toBeVisible();
    await expect(screen.getByText('Go to Google Play')).toBeVisible();
    await host.screenshot('update-required');

    await host.useMinimumAppVersion({ minimumVersion: E2E_APP_VERSION_CODE, updateUrl: UPDATE_URL });
    await host.launch({ keepStorage: true, landsOn: host.app.signedIn });
    await host.expectSignedInAs(user);
    await expect(host.app.sessionId).toHaveText(sessionId);
    await expect(title).toHaveCount(0);
    await host.screenshot('signed-in-after-lowering');
  });
});

test('a signed-out user on a build below the minimum sees the update screen', async ({ host, screen }) => {
  await host.useMinimumAppVersion({ minimumVersion: ABOVE_E2E_APP_VERSION_CODE, updateUrl: UPDATE_URL });
  await host.launch({ landsOn: screen.getByText('Update required') });
  await expect(screen.getByText(E2E_APP_VERSION_NAME)).toBeVisible();
  await host.screenshot('update-required-signed-out');
});

test('a build at the minimum opens as usual', async ({ host, screen }) => {
  await host.useMinimumAppVersion({ minimumVersion: E2E_APP_VERSION_CODE, updateUrl: UPDATE_URL });
  const user = await host.seedUser();
  await host.launch({ signedInAs: user });
  await expect(screen.getByText('Update required')).toHaveCount(0);
});
