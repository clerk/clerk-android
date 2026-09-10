#!/usr/bin/env python3
"""Require executed live scenarios; a successful Gradle exit is not sufficient."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET
from android_test_results import verify as verify_cases


EXPECTED = {
    ("com.clerk.api.integration.AuthIntegrationTests", "signUpAndSignInWithEmailCodes"),
    ("com.clerk.api.integration.EnvironmentIntegrationTests", "fetchesEnvironmentThroughPackagedCore"),
}


def verify(directory):
    verify_cases(directory, EXPECTED)


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
