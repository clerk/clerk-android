import { tapMiddle } from '../../compose.ts';
import { test, expect } from '../../fixtures.ts';

test('deleting the account from the profile Security screen ends the session and removes the account', async ({ host, screen }) => {
  const user = await host.seedUser();
  await host.launch({ signedInAs: user, authMode: 'signIn' });
  await tapMiddle(screen.getByTestId('clerk.userButton.profile'));
  await host.tap(screen.getByTestId('clerk.userProfile.row.security'));
  const deleteAccount = screen.getByText('Delete account');
  await expect(deleteAccount).toBeVisible({ timeout: 20_000 });
  await host.tap(deleteAccount);
  await expect(screen.getByText('Are you sure you want to delete your account? This action is permanent and irreversible.')).toBeVisible({ timeout: 20_000 });
  await host.fill(screen.getByRole('textbox'), 'DELETE');
  await host.tap(deleteAccount.last());
  await host.expectSignedOut(30_000);
  await host.tap(host.app.signIn);
  await host.fill(screen.getByTestId('clerk.auth.start.identifier'), user.email);
  await host.tap(screen.getByTestId('clerk.auth.start.continue'));
  await expect(screen.getByText("Couldn't find your account.")).toBeVisible({ timeout: 20_000 });
  await host.screenshot('account-deleted');
});
