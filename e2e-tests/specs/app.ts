import type { TestApp } from '@clerk/internal-mobile-e2e/specs/support/inputs';

export const APP_ID = 'com.clerk.e2e';

export const app: TestApp = {
  platforms: ['android'],
  id: () => APP_ID,
  entry: () => ({ kind: 'binary' }),
};
