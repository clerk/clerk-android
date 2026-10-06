import { test, expect, CLERK_TEST_CODE } from '../../fixtures.ts';

test('completes sign-up with a run password and the test code', { tags: ['form-entry'] }, async ({ host, screen }) => {
  const email = await host.newEmail();
  const launched = await host.launch({ screen: 'auth', authMode: 'signUp' });
  await expect(screen.getByTestId('clerk.auth.start.identifier')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByTestId('clerk.auth.start.identifier'), email);
  await host.tap(screen.getByTestId('clerk.auth.start.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.password')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByTestId('clerk.auth.signUp.password'), `Verify-${launched.runId}-Pw1!`);
  await host.tap(screen.getByTestId('clerk.auth.signUp.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.code')).toBeVisible({ timeout: 20_000 });
  await host.screenshot('signup-code');
  await host.fill(screen.getByTestId('clerk.auth.signUp.code'), CLERK_TEST_CODE);
  const state = await host.waitForState((s) => s.signedIn && s.sessionStatus === 'active', 30_000);
  expect(state.userId).not.toBeNull();
  await host.screenshot('signed-up');
});
