import { newOrganizationName, signInWithNewOrganization } from '../../compose.ts';
import { test, expect } from '../../fixtures.ts';

test('the switcher moves from the organization to the personal account and back', async ({ host, screen }) => {
  const name = newOrganizationName(host, 'Switch');
  const { user, switcher } = await signInWithNewOrganization(host, screen, name);
  await host.launch({ keepStorage: true, landsOn: host.app.signedIn });
  await expect(switcher.getByText(name)).toBeVisible({ timeout: 20_000 });
  await host.tap(switcher);
  await host.tap(screen.getByTestId('clerk.organizationSwitcher.switchAccount'));
  await expect(screen.getByTestId('clerk.organization.accountList.membership')).toBeVisible({ timeout: 20_000 });
  await host.screenshot('account-list');
  await host.tap(screen.getByTestId('clerk.organization.accountList.personalAccount'));
  await expect(switcher.getByText(user.email)).toBeVisible({ timeout: 20_000 });
  await expect(switcher.getByText(name)).toHaveCount(0);
  await host.screenshot('personal-active');
  await host.tap(switcher);
  await host.tap(screen.getByTestId('clerk.organization.accountList.membership'));
  await expect(switcher.getByText(name)).toBeVisible({ timeout: 20_000 });
  await host.screenshot('org-active-again');
});
