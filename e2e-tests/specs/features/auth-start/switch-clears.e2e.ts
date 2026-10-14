import { test, expect } from '../../fixtures.ts';
import { asShownByClerk, nationalDigits } from '../../phone.ts';

test('switching between the email and phone fields clears the field the user leaves', async ({ host, screen }) => {
  const identifier = screen.getByTestId('clerk.auth.start.identifier');
  const phoneNumber = screen.getByTestId('clerk.auth.start.phoneNumber');
  const switcher = screen.getByTestId('clerk.auth.start.identifierSwitcher');
  const email = await host.newEmail();
  const phone = await host.newPhone();

  await host.launch();
  await host.tap(host.app.signIn);
  await expect(identifier).toBeVisible({ timeout: 20_000 });
  await host.fill(identifier, email);
  await expect(identifier).toHaveText(email);
  await host.screenshot('email-typed');

  await host.tap(switcher);
  await expect(phoneNumber).toBeVisible({ timeout: 10_000 });
  await host.fill(phoneNumber, nationalDigits(phone));
  await expect(phoneNumber).toHaveText(asShownByClerk(phone));
  await host.screenshot('phone-typed');

  await host.tap(switcher);
  await expect(identifier).toBeVisible({ timeout: 10_000 });
  await host.screenshot('back-on-email');
  await expect(screen.getByText(email)).toHaveCount(0);

  await host.tap(switcher);
  await expect(phoneNumber).toBeVisible({ timeout: 10_000 });
  await host.screenshot('back-on-phone');
  await expect(screen.getByText(asShownByClerk(phone))).toHaveCount(0);
});
