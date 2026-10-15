import { randomBytes } from 'node:crypto';
import { expect, type Locator, type Screen } from 'e2e';
import { ASSERTION_TIMEOUT_MS } from './support/config.ts';
import type { HostFixture, SeededUser } from './support/types.ts';

export function middleOf(target: Locator): Locator {
  const tap = async (): Promise<void> => {
    await target.waitFor({ state: 'visible', timeout: ASSERTION_TIMEOUT_MS });
    const box = await target.boundingBox();
    if (box === null) throw new Error('tap target has no box on screen');
    await target.tap({ position: { x: box.width / 2, y: box.height / 2 } });
  };
  return Object.assign(Object.create(target) as Locator, { tap });
}

export const customSignInLink = (screen: Screen): Locator => screen.getByTestId('e2e.home.customSignIn');

// A retry keeps host.runId, and Clerk refuses a second organization with the same slug.
export const newOrganizationName = (host: HostFixture, prefix: string): string => `${prefix} ${host.runId} ${randomBytes(3).toString('hex')}`;

export async function signInWithNewOrganization(host: HostFixture, screen: Screen, name: string): Promise<{ user: SeededUser; switcher: Locator }> {
  const user = await host.seedUser();
  await host.launch({ signedInAs: user });
  const switcher = screen.getByTestId('clerk.organizationSwitcher.trigger');
  await host.tap(switcher);
  await host.tap(screen.getByTestId('clerk.organization.accountList.createOrganization'));
  await host.fill(screen.getByTestId('clerk.organization.profileForm.name'), name);
  await host.tap(screen.getByTestId('clerk.organization.profileForm.submit'));
  const close = screen.getByLabel('Close');
  await expect(close).toBeVisible({ timeout: 20_000 });
  await host.tap(middleOf(close));
  await host.expectSignedInAs(user, 30_000);
  await expect(switcher.getByText(name)).toBeVisible({ timeout: 20_000 });
  return { user, switcher };
}
