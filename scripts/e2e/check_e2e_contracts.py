#!/usr/bin/env python3
import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
TRAILS_DIR = REPO_ROOT / "trails" / "e2e"
HOST_TAGS_SOURCE = REPO_ROOT / "e2e" / "src" / "main" / "java" / "com" / "clerk" / "e2e" / "E2ETags.kt"
HOST_SOURCES = REPO_ROOT / "e2e" / "src"
SHARD_RUNNER = REPO_ROOT / "scripts" / "run-e2e-shard.sh"
RECORDED_TRAIL = "android-phone.trail.yaml"
SOURCE_TRAIL = "blaze.yaml"

APPROVED_PHONE_RANGE = range(5_555_550_100, 5_555_550_200)
PHONE_PATTERN = re.compile(r"(?<!\d)\+?1?[\s().-]*555[\s().-]*555[\s().-]*\d{4}(?!\d)")
RESOURCE_ID_PATTERN = re.compile(r"resourceIdRegex:\s*(\S+)")
HOST_TAG_PATTERN = re.compile(r'const val \w+ = "(e2e\.[^"]+)"')
PROMPT_PATTERN = re.compile(r"^    - (step|verify): (.+)$", re.MULTILINE)

failures = []


def fail(path, line, message):
    relative = path.relative_to(REPO_ROOT)
    if os.environ.get("GITHUB_ACTIONS") == "true":
        print(f"::error file={relative},line={line}::{message}")
    else:
        print(f"error: {relative}:{line}: {message}", file=sys.stderr)
    failures.append(message)


def line_number(text, offset):
    return text.count("\n", 0, offset) + 1


def check_host_selectors(host_tags):
    for trail in sorted(TRAILS_DIR.rglob("*.yaml")):
        text = trail.read_text()
        for match in RESOURCE_ID_PATTERN.finditer(text):
            selector = match.group(1).replace("\\.", ".")
            if selector.startswith("e2e.") and selector not in host_tags:
                fail(trail, line_number(text, match.start()), f"Selector '{selector}' is not defined in E2ETags.kt.")


def check_phone_numbers():
    sources = list(HOST_SOURCES.rglob("*.kt")) + list(TRAILS_DIR.rglob("*.yaml"))
    for source in sorted(sources):
        text = source.read_text()
        for match in PHONE_PATTERN.finditer(text):
            digits = re.sub(r"\D", "", match.group(0))
            if len(digits) == 11 and digits.startswith("1"):
                digits = digits[1:]
            if len(digits) != 10 or int(digits) not in APPROVED_PHONE_RANGE:
                fail(
                    source,
                    line_number(text, match.start()),
                    f"E2E phone number '{match.group(0).strip()}' is outside the approved 5555550100...5555550199 range.",
                )


def check_trail_pairs_and_shards():
    runner = SHARD_RUNNER.read_text()
    for scenario in sorted(path for path in TRAILS_DIR.iterdir() if path.is_dir()):
        recorded = scenario / RECORDED_TRAIL
        source = scenario / SOURCE_TRAIL
        for required in (recorded, source):
            if not required.exists():
                fail(scenario, 1, f"Missing {required.name} next to the other trail file.")
        if recorded.exists() and source.exists():
            recorded_prompts = PROMPT_PATTERN.findall(recorded.read_text())
            source_prompts = PROMPT_PATTERN.findall(source.read_text())
            if recorded_prompts != source_prompts:
                fail(recorded, 1, f"Prompts differ from {SOURCE_TRAIL}; keep the recorded steps and the source trail in sync.")
        if f"trails/e2e/{scenario.name}" not in runner:
            fail(SHARD_RUNNER, 1, f"trails/e2e/{scenario.name} is not mapped to a shard, so CI would never run it.")


def main():
    host_tags = set(HOST_TAG_PATTERN.findall(HOST_TAGS_SOURCE.read_text()))
    check_host_selectors(host_tags)
    check_phone_numbers()
    check_trail_pairs_and_shards()
    if failures:
        print(f"E2E contract check failed with {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print(f"E2E contract check passed: {len(host_tags)} host tags, approved phone range 5555550100...5555550199.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
