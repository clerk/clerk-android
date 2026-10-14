import type { TestApp } from './support/inputs.ts';

export const APP_ID = 'com.clerk.e2e';

export const app: TestApp = {
  platforms: ['android'],
  id: () => APP_ID,
  entry: () => ({ kind: 'binary' }),
};
