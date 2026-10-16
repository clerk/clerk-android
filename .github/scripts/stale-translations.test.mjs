import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { parseStrings, staleTranslations } from './stale-translations.mjs';

const xml = entries => `<resources>${entries.join('')}</resources>`;
const strings = (...entries) => parseStrings(xml(entries));

describe('staleTranslations', () => {
  const english = {
    base: strings('<string name="advisory">Exists for %1$s and %2$s.</string>', '<string name="title">Profile Details</string>'),
    head: strings('<string name="advisory">Exists for %1$s and domain %2$s.</string>', '<string name="title">Profile details</string>'),
  };

  it('reports a locale whose translation did not change with the English text', () => {
    const de = strings('<string name="advisory">Existiert für %1$s und %2$s.</string>');
    const stale = staleTranslations({ english, locales: { 'values-de': { base: de, head: de } } });
    assert.deepEqual(stale.map(({ locale, name }) => `${locale}:${name}`), ['values-de:advisory']);
  });

  it('accepts a retranslated string and ignores casing-only English changes', () => {
    const before = strings('<string name="advisory">Existiert für %1$s und %2$s.</string>', '<string name="title">Profildetails</string>');
    const after = strings('<string name="advisory">Existiert für %1$s und Domain %2$s.</string>', '<string name="title">Profildetails</string>');
    assert.deepEqual(staleTranslations({ english, locales: { 'values-de': { base: before, head: after } } }), []);
  });

  it('skips strings marked translatable="false"', () => {
    const base = strings('<string name="brand" translatable="false">Clerk</string>');
    const head = strings('<string name="brand" translatable="false">Clerk Inc</string>');
    assert.deepEqual(staleTranslations({ english: { base, head }, locales: { 'values-de': { base, head } } }), []);
  });

  it('ignores punctuation written as an XML entity', () => {
    const base = strings('<string name="title">Profile details</string>');
    const head = strings('<string name="title">&quot;Profile details&quot; &amp;</string>');
    const de = strings('<string name="title">Profildetails</string>');
    assert.deepEqual(staleTranslations({ english: { base, head }, locales: { 'values-de': { base: de, head: de } } }), []);
  });

  it('reads a string whose name is not its first attribute', () => {
    const base = strings('<string translatable="true" name="advisory">Exists for %1$s.</string>', '<string translatable="false" name="brand">Clerk</string>');
    assert.deepEqual([...base.keys()], ['advisory']);
  });
});
