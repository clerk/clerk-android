import { test, expect } from '../../fixtures.ts';

test('UserProfileView shows the seeded user', async ({ host, screen }) => {
  const user = await host.seedUser();
  const state = await host.launch({ signedInAs: user, screen: 'userProfile' });
  expect(state.userId).toBe(user.id);
  expect(state.sessionStatus).toBe('active');
  await expect(screen.getByText('Edit profile')).toBeVisible({ timeout: 20_000 });
  await host.tap(screen.getByTestId('clerk.userProfile.row.manageAccount'));
  await expect(screen.getByText(user.email)).toBeVisible({ timeout: 20_000 });
  await host.screenshot('profile');
});

test('the Add account row opens sign-in for a second account', async ({ host, screen }) => {
  const user = await host.seedUser();
  const state = await host.launch({ signedInAs: user, screen: 'userProfile' });
  expect(state.userId).toBe(user.id);
  await host.tap(screen.getByTestId('clerk.userProfile.row.addAccount'));
  await expect(screen.getByTestId('clerk.auth.start.continue')).toBeVisible({ timeout: 20_000 });
  await host.screenshot('add-account');
});

test('the home UserButton opens the profile', async ({ host, screen }) => {
  const user = await host.seedUser();
  const state = await host.launch({ signedInAs: user, screen: 'home' });
  expect(state.userId).toBe(user.id);
  await host.tap(screen.getByTestId('clerk.userButton.profile'));
  await expect(screen.getByTestId('clerk.userProfile.row.manageAccount')).toBeVisible({ timeout: 20_000 });
  await host.screenshot('user-button-profile');
});

test('the home sign-out button ends the session', async ({ host, screen }) => {
  const user = await host.seedUser();
  await host.launch({ signedInAs: user, screen: 'home' });
  await host.tap(screen.getByTestId('verify.signOut'));
  const state = await host.waitForState((s) => !s.signedIn, 10_000);
  expect(state.userId).toBeNull();
  await expect(screen.getByText('Prebuilt UI Sign In')).toBeVisible();
});

test('the profile Sign out row ends the session', async ({ host, screen }) => {
  const user = await host.seedUser();
  const state = await host.launch({ signedInAs: user, screen: 'userProfile' });
  expect(state.userId).toBe(user.id);
  await host.tap(screen.getByTestId('clerk.userProfile.row.signOut'));
  const signedOut = await host.waitForState((s) => !s.signedIn, 10_000);
  expect(signedOut.sessionId).toBeNull();
});
