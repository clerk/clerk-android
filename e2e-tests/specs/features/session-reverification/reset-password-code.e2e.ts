import { runPassword } from '@clerk/internal-mobile-e2e/specs/support/clerk';
import { test, expect, CLERK_TEST_CODE } from '../../fixtures.ts';

test('reverifying with a reset password code sets the new password', async ({ host, screen }) => {
  const user = await host.seedUser({ password: true });
  const newPassword = runPassword(host.runId);
  await host.launch({ signedInAs: user });

  await host.evidence('reverify with an email reset code and a new password', async () => {
    await host.tap(screen.getByTestId('e2e.home.resetPasswordReverification'));
    await host.tap(screen.getByTestId('verify.resetPasswordReverification.sendCode'));
    await expect(screen.getByTestId('verify.resetPasswordReverification.sentTo')).toBeVisible({ timeout: 20_000 });
    await expect(screen.getByTestId('verify.resetPasswordReverification.error')).toHaveCount(0);
    await host.screenshot('reset-code-sent');
    await host.fill(screen.getByTestId('verify.resetPasswordReverification.code'), CLERK_TEST_CODE);
    await host.fill(screen.getByTestId('verify.resetPasswordReverification.newPassword'), newPassword);
    await host.tap(screen.getByTestId('verify.resetPasswordReverification.verify'));
    await host.expectSignedInAs(user, 30_000);
    await host.screenshot('reverified');
  });

  await host.tap(host.app.signOut);
  await host.expectSignedOut(20_000);

  await host.evidence('sign in again with the new password', async () => {
    await host.tap(host.app.signInFullScreen);
    await host.fill(screen.getByTestId('clerk.auth.start.identifier'), user.email);
    await host.tap(screen.getByTestId('clerk.auth.start.continue'));
    const password = screen.getByTestId('clerk.auth.signIn.password');
    await expect(password).toBeVisible({ timeout: 20_000 });
    await host.fill(password, newPassword);
    await host.tap(screen.getByTestId('clerk.auth.signIn.continue'));
    await host.expectSignedInAs(user, 30_000);
    await host.screenshot('signed-in-with-new-password');
  });
});
