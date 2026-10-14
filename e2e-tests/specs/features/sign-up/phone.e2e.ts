import { test, expect, CLERK_TEST_CODE } from '../../fixtures.ts';
import { asShownByClerk, nationalDigits } from '../../phone.ts';

test('signs up with a phone number, then gives the email and password the instance requires', async ({ host, screen }) => {
  const phone = await host.newPhone();
  const email = await host.newEmail();
  const code = screen.getByTestId('clerk.auth.signUp.code');
  await host.launch({ authMode: 'signUp' });
  await host.tap(host.app.signInFullScreen);
  await host.tap(screen.getByTestId('clerk.auth.start.identifierSwitcher'));
  await host.fill(screen.getByTestId('clerk.auth.start.phoneNumber'), nationalDigits(phone));
  await host.tap(screen.getByTestId('clerk.auth.start.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.emailAddress')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByTestId('clerk.auth.signUp.emailAddress'), email);
  await host.tap(screen.getByTestId('clerk.auth.signUp.continue'));
  await expect(screen.getByTestId('clerk.auth.signUp.password')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByTestId('clerk.auth.signUp.password'), `Verify-${host.runId}-Pw1!`);
  await host.tap(screen.getByTestId('clerk.auth.signUp.continue'));
  await expect(screen.getByText('Check your email')).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByText(email)).toBeVisible();
  await host.screenshot('signup-phone-email-code');
  await host.fill(code, CLERK_TEST_CODE);
  await expect(screen.getByText('Check your phone')).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByText(asShownByClerk(phone))).toBeVisible();
  await host.screenshot('signup-phone-code');
  await host.fill(code, CLERK_TEST_CODE);
  await host.expectSignedInAs(email, 30_000);
  await host.screenshot('signed-up-with-phone');
});
