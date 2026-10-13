import type { Locator, Screen } from 'e2e';
import { ASSERTION_TIMEOUT_MS } from './support/config.ts';

export async function tapMiddle(target: Locator): Promise<void> {
  await target.waitFor({ state: 'visible', timeout: ASSERTION_TIMEOUT_MS });
  const box = await target.boundingBox();
  if (box === null) throw new Error('tap target has no box on screen');
  await target.tap({ position: { x: box.width / 2, y: box.height / 2 } });
}

export const customSignInLink = (screen: Screen): Locator => screen.getByTestId('e2e.home.customSignIn');
