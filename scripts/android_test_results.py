#!/usr/bin/env python3
"""Require the exact expected Android instrumentation cases to execute and pass."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET


def verify(directory, expected):
    cases = []
    for report in sorted(directory.rglob("TEST-*.xml")):
        cases.extend(ET.parse(report).iter("testcase"))
    found = {(case.get("classname"), case.get("name")) for case in cases}
    if len(cases) != len(expected) or found != expected:
        raise ValueError(f"Expected {len(expected)} named scenarios exactly once; found {len(cases)} cases")
    for case in cases:
        if any(case.find(tag) is not None for tag in ("skipped", "failure", "error")):
            raise ValueError(f"Scenario did not pass: {case.get('classname')}.{case.get('name')}")


def scenario(value):
    parts = value.split("#")
    if len(parts) != 2 or not all(parts):
        raise argparse.ArgumentTypeError("Expected CLASS#METHOD")
    return tuple(parts)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--results", type=Path, default=Path(__file__).resolve().parents[1]
                        / "source/api/build/outputs/androidTest-results/connected/debug")
    parser.add_argument("--expected", type=scenario, action="append", required=True)
    args = parser.parse_args()
    try:
        verify(args.results, set(args.expected))
    except (OSError, ET.ParseError, ValueError) as error:
        raise SystemExit(f"Android test evidence incomplete: {error}") from None
    print("All expected Android instrumentation scenarios executed and passed.")
