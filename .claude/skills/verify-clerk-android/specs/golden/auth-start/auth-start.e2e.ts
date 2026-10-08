import { test, expect } from '../../fixtures.ts';
import { asShownByClerk } from '../../phone.ts';

test('the home full-screen sign-in button shows AuthView with no close button', async ({ host, screen }) => {
  await host.launch();
  await host.tap(host.app.signInFullScreen);
  await expect(screen.getByTestId('clerk.auth.start.identifier')).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByTestId('clerk.auth.start.continue').getByText('Next', { exact: true })).toBeVisible();
  await expect(screen.getByTestId('clerk.dismissButton')).toHaveCount(0);
  await host.screenshot('auth-full-screen');
});

test('AuthView opens on the phone field holding the number the app passes as initialIdentifier', async ({ host, screen }) => {
  const user = await host.seedUser({ phone: true, password: true });
  const phoneNumber = screen.getByTestId('clerk.auth.start.phoneNumber');
  await host.launch({ authMode: 'signIn', initialIdentifier: user.phone! });
  await host.tap(host.app.signInFullScreen);
  await expect(phoneNumber).toHaveText(asShownByClerk(user.phone!), { timeout: 20_000 });
  await expect(screen.getByTestId('clerk.auth.start.identifier')).toHaveCount(0);
  await host.screenshot('auth-initial-identifier');
  await host.tap(screen.getByTestId('clerk.auth.start.continue'));
  await expect(screen.getByTestId('clerk.auth.signIn.password')).toBeVisible({ timeout: 20_000 });
  await host.screenshot('auth-initial-identifier-accepted');
});
