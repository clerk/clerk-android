import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { describe, it } from 'node:test';

const config = readFileSync(new URL('../../.coderabbit.yaml', import.meta.url), 'utf8');
const template = readFileSync(new URL('../pull_request_template.md', import.meta.url), 'utf8');

function block(text, key, indent) {
  const matches = [...text.matchAll(new RegExp(`^ {${indent}}${key}:[ \\t]*(?:\\|[+-]?[ \\t]*)?\\r?\\n((?: {${indent + 1}}[^\\n]*\\n|\\r?\\n)*)`, 'gm'))];
  assert.equal(matches.length, 1, `expected one ${key} block at indentation ${indent}`);
  return matches[0][1];
}

function scalar(text, key, indent) {
  const matches = [...text.matchAll(new RegExp(`^ {${indent}}${key}:[ \\t]*(.+)$`, 'gm'))];
  assert.equal(matches.length, 1, `expected one ${key} setting at indentation ${indent}`);
  return matches[0][1].trim();
}

const reviews = block(config, 'reviews', 0);
const approvals = block(reviews, 'auto_approvals', 2);
const preMerge = block(reviews, 'pre_merge_checks', 2);
const autoReview = block(reviews, 'auto_review', 2);
const customChecks = block(preMerge, 'custom_checks', 4).split(/(?=^ {6}- name:)/m).filter(text => text.trim());

function check(name) {
  const matches = customChecks.filter(text => /^ {6}- name: "([^"]+)"$/m.exec(text)?.[1] === name);
  assert.equal(matches.length, 1, `expected one custom check named ${name}`);
  return matches[0];
}

const instructions = name => block(check(name), 'instructions', 8).replace(/\s+/g, ' ').trim();

describe('CodeRabbit automatic approvals', () => {
  it('enables automatic approvals through the current configuration key', () => {
    assert.equal(scalar(approvals, 'enabled', 4), 'true');
    assert.doesNotMatch(reviews, /^ {2}request_changes_workflow:/m);
  });

  it('prevents PR authors from approving their own changes after the key migration', () => {
    assert.equal(scalar(approvals, 'allow_author_approval', 4), 'false');
    assert.doesNotMatch(reviews, /^ {2}allow_author_approval:/m);
  });

  it('caps automatic approval at moderate risk so high and critical risk need a human', () => {
    assert.equal(scalar(approvals, 'max_merge_risk', 4), 'moderate');
  });
});

describe('CodeRabbit pre-merge checks', () => {
  it('restricts overrides to requested reviewers', () => {
    assert.equal(scalar(preMerge, 'override_requested_reviewers_only', 4), 'true');
  });

  for (const name of ['Testing, risk, and revert', 'Device evidence']) {
    it(`makes ${name} blocking rather than advisory`, () => {
      assert.equal(scalar(check(name), 'mode', 8), 'error');
      assert.ok(instructions(name).length > 0);
    });
  }
});

describe('the testing, risk, and revert check instructions', () => {
  const text = instructions('Testing, risk, and revert');

  it('requires the same three sections that the PR template provides', () => {
    for (const heading of ['Testing', 'Risk', 'How to revert']) {
      assert.match(template, new RegExp(`^### ${heading}\\s*$`, 'm'));
      assert.ok(text.includes(heading), `instructions must require ${heading}`);
    }
    assert.match(text, /each one says something specific to this PR/i);
  });

  it('rejects absent or unfinished sections, including the unchanged template', () => {
    assert.match(text, /FAIL if any of the three is missing, empty/i);
    assert.match(text, /a lone "-"/i);
    assert.match(text, /only the template's HTML comment/i);
  });

  it('rejects bare placeholders even when all three headings are present', () => {
    assert.match(text, /placeholder with no reason/i);
    for (const placeholder of ['TBD', 'N/A', 'none', 'n/a']) {
      assert.ok(text.includes(`"${placeholder}"`), `instructions must reject ${placeholder}`);
    }
  });

  it('accepts short answers that give a PR-specific reason', () => {
    assert.match(text, /PASS when each section names something about this PR/i);
    assert.match(text, /short answer with a reason passes/i);
  });
});

describe('the device evidence check instructions', () => {
  const text = instructions('Device evidence');

  it('exempts changes outside the three SDK and host paths', () => {
    const exemption = /^PASS if the diff changes no file under ([^.]+)\./.exec(text);
    assert.ok(exemption, 'the no-device-evidence exemption must depend on the changed paths');
    assert.deepEqual([...exemption[1].matchAll(/`([^`]+)`/g)].map(match => match[1]), ['source/api/', 'source/ui/', 'e2e/']);
    assert.match(text, /Otherwise PASS only if the PR description does one of these/i);
  });

  it('requires device screenshots or video from before and after the change', () => {
    assert.match(text, /Shows a before and an after/i);
    assert.match(text, /screenshots or video on an emulator or device/i);
  });

  it('covers every touched screen and state, including an unchanged state', () => {
    assert.match(text, /for every screen and state the diff touches/i);
    assert.match(text, /including a state that should look the same after the change/i);
  });

  it('requires before and after pairs side by side in a table', () => {
    assert.match(text, /pairs sit in a table, each before beside its after/i);
    assert.match(text, /FAIL if .*isn't laid out as a before and after table/i);
  });

  it('allows an explicit explanation when device verification could not run', () => {
    assert.match(text, /\(b\) Says why the change could not be run on one/i);
  });

  it('requires disclosure of what a state-forcing patch changed and why', () => {
    assert.match(text, /If a local patch forced a state, the description must also say what the patch changed and why/i);
    assert.match(text, /FAIL if .*forces a state without saying how/i);
  });

  it('rejects missing, after-only, and incomplete evidence', () => {
    assert.match(text, /FAIL if the evidence is missing, has no before, leaves out a screen or state the diff touches/i);
  });

  it('does not accept CI, test results, or snapshots as device evidence', () => {
    assert.match(text, /A link to CI, a test run, or snapshot images is not device evidence/i);
  });
});

describe('CodeRabbit review cadence and walkthrough', () => {
  it('keeps draft reviews disabled', () => {
    assert.equal(scalar(autoReview, 'drafts', 4), 'false');
  });

  it('pauses automatic reviews after three reviewed commits', () => {
    assert.equal(scalar(autoReview, 'auto_pause_after_reviewed_commits', 4), '3');
  });

  it('opens the walkthrough and includes the high-level summary', () => {
    assert.equal(scalar(reviews, 'collapse_walkthrough', 2), 'false');
    assert.equal(scalar(reviews, 'high_level_summary_in_walkthrough', 2), 'true');
  });

  it('asks for a short explanation of the old behavior, the change, and where to review', () => {
    const text = block(reviews, 'high_level_summary_instructions', 2).replace(/\s+/g, ' ').trim();
    assert.match(text, /teammate who doesn't know this part of the code/i);
    assert.match(text, /three to six plain sentences/i);
    assert.match(text, /how the touched area worked before this PR/i);
    assert.match(text, /what changes/i);
    assert.match(text, /where a reviewer should look first/i);
    assert.match(text, /Don't repeat the file list/i);
  });
});
