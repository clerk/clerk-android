import { test, expect } from '../../fixtures.ts';

test('the home Prebuilt UI Sign In button opens AuthView', async ({ host, screen }) => {
  const state = await host.launch({ instance: 'with-email-codes', screen: 'home' });
  expect(state.environmentLoaded).toBe(true);
  expect(state.signedIn).toBe(false);
  await host.tap(screen.getByText('Prebuilt UI Sign In'));
  await expect(screen.getByTestId('clerk.auth.start.continue')).toBeVisible({ timeout: 20_000 });
  await host.screenshot('auth-start');
});

test('verifyScreen auth opens AuthView without a tap', async ({ host, screen }) => {
  const state = await host.launch({ instance: 'with-email-codes', screen: 'auth' });
  expect(state.screen).toBe('auth');
  expect(state.lastError).toBeNull();
  await expect(screen.getByTestId('clerk.auth.start.identifier')).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByTestId('clerk.auth.start.continue')).toBeVisible();
  await host.screenshot('auth-start-identifier');
});
