import { middleOf } from '../../compose.ts';
import { test, expect } from '../../fixtures.ts';

test('a ticket sign-in on an MFA-required instance stops on the setup-MFA task', async ({ host, screen }) => {
  await host.useSettings({
    config: { 'auth_multi_factor.required_for_sign_up': true },
    environment: { 'user_settings.sign_up.mfa.required': true },
  });
  const user = await host.seedUser();
  await host.launch({ signedInAs: user, landsOn: host.app.signedOut });
  await host.tap(host.app.signInFullScreen);
  await expect(screen.getByTestId('clerk.auth.sessionTask.setupMfa.authenticatorApp')).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByTestId('clerk.auth.sessionTask.setupMfa.smsCode')).toBeVisible();
  await host.screenshot('session-task');
  await host.tap(middleOf(screen.getByLabel('User avatar')));
  await expect(screen.getByText(user.email)).toBeVisible({ timeout: 20_000 });
  await host.screenshot('session-task-account');
});
