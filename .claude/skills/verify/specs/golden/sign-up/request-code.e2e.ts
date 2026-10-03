import { test, expect } from '../../fixtures.ts';

test('a new test email and a run password reach the sign-up code screen', { tags: ['form-entry'] }, async ({ host, screen }) => {
  const email = await host.newEmail('with-email-codes');
  const launched = await host.launch({ instance: 'with-email-codes', screen: 'auth', authMode: 'signUp' });
  await expect(screen.getByTestId('clerk.auth.start.identifier')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByTestId('clerk.auth.start.identifier'), email);
  await host.tap(screen.getByTestId('clerk.auth.start.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.password')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByTestId('clerk.auth.signUp.password'), `Verify-${launched.runId}-Pw1!`);
  await host.tap(screen.getByTestId('clerk.auth.signUp.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.code')).toBeVisible({ timeout: 20_000 });
  const state = await host.waitForState((s) => s.signUpStatus === 'missing_requirements');
  expect(state.signedIn).toBe(false);
  await host.screenshot('signup-code');
});
