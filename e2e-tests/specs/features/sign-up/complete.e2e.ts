import { test, expect, CLERK_TEST_CODE } from '../../fixtures.ts';

test('completes sign-up with a run password and the test code', async ({ host, screen }) => {
  const email = await host.newEmail();
  await host.launch({ authMode: 'signUp' });
  await host.tap(host.app.signInFullScreen);
  await host.fill(screen.getByTestId('clerk.auth.start.identifier'), email);
  await host.tap(screen.getByTestId('clerk.auth.start.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.password')).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByTestId('clerk.auth.signUp.continue')).toBeVisible();
  await host.screenshot('signup-password');
  await host.fill(screen.getByTestId('clerk.auth.signUp.password'), `Verify-${host.runId}-Pw1!`);
  await host.tap(screen.getByTestId('clerk.auth.signUp.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.code')).toBeVisible({ timeout: 20_000 });
  await host.screenshot('signup-code');
  await host.fill(screen.getByTestId('clerk.auth.signUp.code'), CLERK_TEST_CODE);
  await host.expectSignedInAs(email, 30_000);
  await host.screenshot('signed-up');
});
