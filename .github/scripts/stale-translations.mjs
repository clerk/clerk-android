import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const RES = 'source/ui/src/main/res';
const STRING = /<string\b([^>]*)>([\s\S]*?)<\/string>/g;

export function parseStrings(xml) {
  const strings = new Map();
  for (const [, attributes, text] of xml.matchAll(STRING)) {
    const name = /\bname\s*=\s*"([^"]+)"/.exec(attributes)?.[1];
    if (name && !/translatable\s*=\s*"false"/.test(attributes)) strings.set(name, text);
  }
  return strings;
}

const wording = text => text.toLowerCase().replace(/[^\p{L}\p{N}%$]/gu, '');

export function staleTranslations({ english, locales }) {
  const stale = [];
  for (const [name, before] of english.base) {
    const after = english.head.get(name);
    if (after === undefined || wording(after) === wording(before)) continue;
    for (const [locale, { base, head }] of Object.entries(locales)) {
      if (head.has(name) && base.get(name) === head.get(name)) {
        stale.push({ locale, name, before, after });
      }
    }
  }
  return stale;
}

function read(ref, file) {
  if (ref === undefined) return fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '';
  try {
    return execFileSync('git', ['show', `${ref}:${file}`], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
  } catch {
    return '';
  }
}

export function main([baseRef, headRef]) {
  const load = file => ({ base: parseStrings(read(baseRef, file)), head: parseStrings(read(headRef, file)) });
  const localeDirs = fs.readdirSync(RES).filter(dir => dir.startsWith('values-') && fs.existsSync(path.join(RES, dir, 'strings.xml')));
  const stale = staleTranslations({
    english: load(`${RES}/values/strings.xml`),
    locales: Object.fromEntries(localeDirs.map(dir => [dir, load(`${RES}/${dir}/strings.xml`)])),
  });
  for (const { locale, name, before, after } of stale) {
    console.error(`${RES}/${locale}/strings.xml: "${name}" still translates the old English text.`);
    console.error(`  English changed from "${before}" to "${after}". Update this translation to say the same thing.`);
  }
  return stale.length === 0 ? 0 : 1;
}

if (process.argv[1] !== undefined && fs.realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  process.exit(main(process.argv.slice(2)));
}
