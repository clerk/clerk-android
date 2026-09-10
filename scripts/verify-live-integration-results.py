#!/usr/bin/env python3
"""Require executed live scenarios; a successful Gradle exit is not sufficient."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET


EXPECTED = {
    ("com.clerk.api.integration.AuthIntegrationTests", "signUpAndSignInWithEmailCodes"),
    ("com.clerk.api.integration.EnvironmentIntegrationTests", "fetchesEnvironmentThroughPackagedCore"),
}


def verify(directory):
    cases = []
    for report in sorted(directory.rglob("TEST-*.xml")):
        for case in ET.parse(report).iter("testcase"):
            if case.get("classname", "").startswith("com.clerk.api.integration."):
                cases.append(case)
    found = {(case.get("classname"), case.get("name")) for case in cases}
    if len(cases) != len(EXPECTED) or found != EXPECTED:
        raise ValueError(f"Expected both live integration scenarios exactly once; found {len(cases)} cases")
    for case in cases:
        if any(case.find(tag) is not None for tag in ("skipped", "failure", "error")):
            raise ValueError(f"Live scenario did not pass: {case.get('classname')}.{case.get('name')}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--results", type=Path, default=Path(__file__).resolve().parents[1]
                        / "source/api/build/outputs/androidTest-results/connected/debug")
    args = parser.parse_args()
    try:
        verify(args.results)
    except (OSError, ET.ParseError, ValueError) as error:
        raise SystemExit(f"Live integration evidence incomplete: {error}") from None
    print("Both expected live integration scenarios executed and passed.")


if __name__ == "__main__":
    main()
