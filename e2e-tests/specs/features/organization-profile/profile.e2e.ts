import { signInWithNewOrganization, tapMiddle } from '../../compose.ts';
import { test, expect } from '../../fixtures.ts';
import type { HostFixture } from '../../support/types.ts';
import type { Screen } from 'e2e';

async function openManageOrganization(host: HostFixture, screen: Screen, name: string): Promise<void> {
  const { switcher } = await signInWithNewOrganization(host, screen, name);
  await host.tap(switcher);
  await host.tap(screen.getByTestId('clerk.organizationSwitcher.manageOrganization'));
  await expect(screen.getByText('Update profile', { exact: true })).toBeVisible({ timeout: 20_000 });
}

async function openRow(screen: Screen, label: string): Promise<void> {
  const row = screen.getByText(label, { exact: true }).first();
  await expect(row).toBeVisible({ timeout: 20_000 });
  await tapMiddle(row);
}

test('the creator of an organization sees every row of its profile', async ({ host, screen, device }) => {
  const name = `Profile ${host.runId}`;
  await openManageOrganization(host, screen, name);
  await expect(screen.getByText(name, { exact: true })).toBeVisible();
  for (const row of ['Members', 'Verified domains', 'Leave organization', 'Delete organization']) {
    await expect(screen.getByText(row, { exact: true })).toBeVisible();
  }
  await host.screenshot('org-profile');

  await openRow(screen, 'Verified domains');
  await expect(screen.getByText('Add domain', { exact: true })).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByText('No verified domains.')).toHaveCount(0);
  await host.screenshot('verified-domains');
  await device.back();

  await openRow(screen, 'Members');
  await expect(screen.getByText('You', { exact: true })).toBeVisible({ timeout: 20_000 });
  await host.screenshot('members');
  await openRow(screen, 'Invitations');
  await expect(screen.getByText('No invitations sent')).toBeVisible({ timeout: 20_000 });
  await openRow(screen, 'Requests');
  await expect(screen.getByText('No membership requests')).toBeVisible({ timeout: 20_000 });
});

test('an invitation sent from Members shows on the Invitations tab', async ({ host, screen }) => {
  await openManageOrganization(host, screen, `Invite ${host.runId}`);
  await openRow(screen, 'Members');
  await openRow(screen, 'Invite');
  const invitee = await host.newEmail();
  await host.fill(screen.getByText('Enter email addresses'), invitee);
  await openRow(screen, 'Send invitations');
  await openRow(screen, 'Invitations');
  await expect(screen.getByText(invitee)).toBeVisible({ timeout: 20_000 });
  await host.screenshot('invitation-sent');
});

test('Update profile renames the organization', async ({ host, screen }) => {
  await openManageOrganization(host, screen, `Rename ${host.runId}`);
  await openRow(screen, 'Update profile');
  const field = screen.getByTestId('clerk.organization.profileForm.name');
  await expect(field).toBeVisible({ timeout: 20_000 });
  await field.clear();
  const renamed = `Renamed ${host.runId}`;
  await host.fill(field, renamed);
  await host.tap(screen.getByTestId('clerk.organization.profileForm.submit'));
  await expect(field).toHaveCount(0, { timeout: 20_000 });
  await expect(screen.getByText(renamed, { exact: true })).toBeVisible({ timeout: 20_000 });
  await host.screenshot('org-renamed');
});
