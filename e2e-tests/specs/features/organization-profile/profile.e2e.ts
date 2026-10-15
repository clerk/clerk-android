import { newOrganizationName, signInWithNewOrganization, middleOf } from '../../compose.ts';
import { test, expect, CLERK_TEST_CODE } from '../../fixtures.ts';
import type { HostFixture } from '../../support/types.ts';
import type { Screen } from 'e2e';

async function openManageOrganization(host: HostFixture, screen: Screen, name: string): Promise<void> {
  const { switcher } = await signInWithNewOrganization(host, screen, name);
  await host.tap(switcher);
  await host.tap(screen.getByTestId('clerk.organizationSwitcher.manageOrganization'));
  await expect(screen.getByText('Update profile', { exact: true })).toBeVisible({ timeout: 20_000 });
}

async function openRow(host: HostFixture, screen: Screen, label: string): Promise<void> {
  const row = screen.getByText(label, { exact: true }).first();
  await expect(row).toBeVisible({ timeout: 20_000 });
  await host.tap(middleOf(row));
}

test('the creator of an organization sees every row of its profile', async ({ host, screen, device }) => {
  const name = newOrganizationName(host, 'Profile');
  await openManageOrganization(host, screen, name);
  await expect(screen.getByText(name, { exact: true })).toBeVisible();
  for (const row of ['Members', 'Verified domains', 'Leave organization', 'Delete organization']) {
    await expect(screen.getByText(row, { exact: true })).toBeVisible();
  }
  await host.screenshot('org-profile');

  await openRow(host, screen, 'Verified domains');
  await expect(screen.getByText('Add domain', { exact: true })).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByText('No verified domains.')).toHaveCount(0);
  await host.screenshot('verified-domains');
  await device.back();

  await openRow(host, screen, 'Members');
  await expect(screen.getByText('You', { exact: true })).toBeVisible({ timeout: 20_000 });
  await host.screenshot('members');
  await openRow(host, screen, 'Invitations');
  await expect(screen.getByText('No invitations sent')).toBeVisible({ timeout: 20_000 });
  await openRow(host, screen, 'Requests');
  await expect(screen.getByText('No membership requests')).toBeVisible({ timeout: 20_000 });
});

test('an invitation sent from Members shows on the Invitations tab', async ({ host, screen }) => {
  await openManageOrganization(host, screen, newOrganizationName(host, 'Invite'));
  await openRow(host, screen, 'Members');
  await openRow(host, screen, 'Invite');
  const invitee = await host.newEmail();
  await host.fill(screen.getByText('Enter email addresses'), invitee);
  await openRow(host, screen, 'Send invitations');
  await openRow(host, screen, 'Invitations');
  await expect(screen.getByText(invitee)).toBeVisible({ timeout: 20_000 });
  await host.screenshot('invitation-sent');
});

test('Update profile renames the organization', async ({ host, screen }) => {
  await openManageOrganization(host, screen, newOrganizationName(host, 'Rename'));
  await openRow(host, screen, 'Update profile');
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

test('a member whose role reads domains and cannot manage them sees No verified domains and no Add domain', async ({ host, screen }) => {
  const admin = await host.seedUser();
  const reader = await host.seedUser();
  const org = await host.seedOrganization({ creator: admin, members: [{ user: reader, role: 'org:e2e_domains_reader' }] });
  await host.launch({ signedInAs: reader });
  const switcher = screen.getByTestId('clerk.organizationSwitcher.trigger');
  await expect(switcher.getByText(org.name)).toBeVisible({ timeout: 20_000 });
  await host.tap(switcher);
  await host.tap(screen.getByTestId('clerk.organizationSwitcher.manageOrganization'));
  await openRow(host, screen, 'Verified domains');
  await expect(screen.getByText('No verified domains.')).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByText('Add domain', { exact: true })).toHaveCount(0);
  await host.screenshot('org-domains-empty');
});

test('a user who signs up at a verified domain of the organization asks to join, and an admin sees the request on the Requests tab', async ({ host, screen }) => {
  const admin = await host.seedUser();
  const org = await host.seedOrganization({ creator: admin, suggestDomain: true });
  const requester = await host.newEmail({ domain: org.domain });
  await host.launch({ authMode: 'signUp' });
  await host.tap(host.app.signInFullScreen);
  await host.fill(screen.getByTestId('clerk.auth.start.identifier'), requester);
  await host.tap(screen.getByTestId('clerk.auth.start.continue'));
  await host.fill(screen.getByTestId('clerk.auth.signUp.password'), `Verify-${host.runId}-Pw1!`);
  await host.tap(screen.getByTestId('clerk.auth.signUp.continue'));
  await host.fill(screen.getByTestId('clerk.auth.signUp.code'), CLERK_TEST_CODE);
  await host.expectSignedInAs(requester, 30_000);
  await host.tap(screen.getByTestId('clerk.organizationSwitcher.trigger'));
  await openRow(host, screen, 'Request to join');
  await expect(screen.getByText('Pending approval', { exact: true })).toBeVisible({ timeout: 20_000 });

  await host.launch({ signedInAs: admin });
  const switcher = screen.getByTestId('clerk.organizationSwitcher.trigger');
  await expect(switcher.getByText(org.name)).toBeVisible({ timeout: 20_000 });
  await host.tap(switcher);
  await host.tap(screen.getByTestId('clerk.organizationSwitcher.manageOrganization'));
  await openRow(host, screen, 'Members');
  await openRow(host, screen, 'Requests');
  await expect(screen.getByText(requester).first()).toBeVisible({ timeout: 20_000 });
  await expect(screen.getByText('Accept', { exact: true })).toBeVisible();
  await expect(screen.getByText('Reject', { exact: true })).toBeVisible();
  await expect(screen.getByText('No membership requests')).toHaveCount(0);
  await host.screenshot('org-requests');
});
