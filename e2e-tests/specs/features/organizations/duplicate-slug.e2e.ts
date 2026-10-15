import { newOrganizationName, signInWithNewOrganization } from '../../compose.ts';
import { test, expect } from '../../fixtures.ts';

test('a taken slug shows under the slug field until the user changes it', async ({ host, screen }) => {
  const name = newOrganizationName(host, 'Verify');
  const { switcher } = await signInWithNewOrganization(host, screen, name);
  await host.tap(switcher);
  await host.tap(screen.getByTestId('clerk.organizationSwitcher.switchAccount'));
  await host.tap(screen.getByTestId('clerk.organization.accountList.createOrganization'));
  const nameField = screen.getByTestId('clerk.organization.profileForm.name');
  const slugField = screen.getByTestId('clerk.organization.profileForm.slug');
  const taken = slugField.getByText('That slug is taken. Please try another.');
  await host.evidence('a second organization with the same slug', async () => {
    await host.fill(nameField, name);
    await host.tap(screen.getByTestId('clerk.organization.profileForm.submit'));
    await expect(taken).toBeVisible({ timeout: 20_000 });
    await host.screenshot('slug-taken');
    await host.fill(nameField, `${name} 2`);
    await expect(taken).toHaveCount(0);
  });
});
