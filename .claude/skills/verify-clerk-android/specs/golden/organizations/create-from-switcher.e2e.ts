import { tapMiddle } from '../../compose.ts';
import { test, expect } from '../../fixtures.ts';

test('creating an organization from OrganizationSwitcher makes it active', async ({ host, screen }) => {
  const user = await host.seedUser();
  await host.launch({ signedInAs: user });
  const switcher = screen.getByTestId('clerk.organizationSwitcher.trigger');
  await expect(switcher.getByText(user.email)).toBeVisible();
  await host.tap(switcher);
  await host.tap(screen.getByTestId('clerk.organization.accountList.createOrganization'));
  const name = `Verify ${host.runId}`;
  await host.fill(screen.getByTestId('clerk.organization.profileForm.name'), name);
  await host.tap(screen.getByTestId('clerk.organization.profileForm.submit'));
  const close = screen.getByLabel('Close');
  await expect(close).toBeVisible({ timeout: 20_000 });
  await tapMiddle(close);
  await host.expectSignedInAs(user, 30_000);
  await expect(switcher.getByText(name)).toBeVisible({ timeout: 20_000 });
  await expect(switcher.getByText(user.email)).toHaveCount(0);
  await host.screenshot('org-created');
});
