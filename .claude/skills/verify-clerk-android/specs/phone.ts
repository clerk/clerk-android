import type { TestPhone } from '../src/core/types.ts';

export const nationalDigits = (phone: TestPhone): string => phone.slice(2);

export const asShownByClerk = (phone: TestPhone): string => `+1 ${phone.slice(2, 5)}-${phone.slice(5, 8)}-${phone.slice(8)}`;
