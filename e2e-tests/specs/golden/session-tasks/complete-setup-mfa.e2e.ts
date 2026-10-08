import { test, expect } from '../../fixtures.ts';
import { totp } from '../../totp.ts';

test('enrolls an authenticator app to finish the setup-MFA task', async ({ host, screen }) => {
  const user = await host.seedUser();
  const authenticatorApp = screen.getByTestId('clerk.auth.sessionTask.setupMfa.authenticatorApp');
  await host.launch({ signedInAs: user, landsOn: host.app.signedOut });
  await host.tap(host.app.signInFullScreen);
  await expect(authenticatorApp).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByTestId('clerk.auth.sessionTask.setupMfa.smsCode')).toBeVisible();
  await host.screenshot('session-task');
  await host.tap(authenticatorApp);
  const key = screen.getByText(/^[A-Z2-7]{16,}$/);
  await expect(key).toBeVisible({ timeout: 20_000 });
  const secret = (await key.textContent()) ?? '';
  await host.screenshot('totp-secret');
  await host.tap(screen.getByText('Continue'));
  await expect(screen.getByText('Verify authenticator app')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByRole('textbox'), totp(secret, Date.now() / 1000));
  await host.expectSignedInAs(user, 30_000);
  await host.screenshot('task-complete');
});
