import { test, expect, CLERK_TEST_CODE } from '../../fixtures.ts';
import { totp } from '../../totp.ts';

test('a sign-up on an MFA-required instance moves straight into the setup-MFA task, and an authenticator code finishes it', async ({ host, screen }) => {
  const email = await host.newEmail();
  await host.launch({ authMode: 'signUp' });
  await host.tap(host.app.signInFullScreen);
  await host.fill(screen.getByTestId('clerk.auth.start.identifier'), email);
  await host.tap(screen.getByTestId('clerk.auth.start.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.password')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByTestId('clerk.auth.signUp.password'), `Verify-${host.runId}-Pw1!`);
  await host.tap(screen.getByTestId('clerk.auth.signUp.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.code')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByTestId('clerk.auth.signUp.code'), CLERK_TEST_CODE);
  const authenticatorApp = screen.getByTestId('clerk.auth.sessionTask.setupMfa.authenticatorApp');
  await expect(authenticatorApp).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByTestId('clerk.auth.sessionTask.setupMfa.smsCode')).toBeVisible();
  await host.screenshot('sign-up-session-task');
  await host.tap(authenticatorApp);
  const key = screen.getByText(/^[A-Z2-7]{16,}$/);
  await expect(key).toBeVisible({ timeout: 20_000 });
  const secret = (await key.textContent()) ?? '';
  await host.tap(screen.getByText('Continue'));
  await expect(screen.getByText('Verify authenticator app')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByRole('textbox'), totp(secret, Date.now() / 1000));
  await host.expectSignedInAs(email, 30_000);
  await host.screenshot('sign-up-task-complete');
});
