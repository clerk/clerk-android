#!/usr/bin/env python3
"""Regression checks for the release gate, not substitutes for Android execution."""
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
import xml.etree.ElementTree as ET

from android_test_results import verify


class AndroidTestResultsTest(unittest.TestCase):
    expected = {("example.CoreTest", "core"), ("example.PlatformTest", "platform")}

    def check_report(self, cases, passes):
        with TemporaryDirectory() as root:
            suite = ET.Element("testsuite")
            for classname, name, outcome in cases:
                case = ET.SubElement(suite, "testcase", classname=classname, name=name)
                if outcome:
                    ET.SubElement(case, outcome)
            directory = Path(root)
            ET.ElementTree(suite).write(directory / "TEST-fixture.xml")
            if passes:
                verify(directory, self.expected)
            else:
                with self.assertRaises(ValueError):
                    verify(directory, self.expected)

    def test_accepts_all_expected_passes(self):
        self.check_report([(c, m, None) for c, m in self.expected], True)

    def test_rejects_empty_missing_duplicate_and_unexpected_cases(self):
        complete = [(c, m, None) for c, m in sorted(self.expected)]
        for cases in ([], complete[:1], complete + complete[:1],
                      complete + [("example.Unexpected", "extra", None)]):
            with self.subTest(cases=cases):
                self.check_report(cases, False)

    def test_rejects_failures_errors_and_skips(self):
        for outcome in ("failure", "error", "skipped"):
            with self.subTest(outcome=outcome):
                self.check_report([("example.CoreTest", "core", None),
                                   ("example.PlatformTest", "platform", outcome)], False)

    def test_rejects_absent_reports(self):
        with TemporaryDirectory() as root, self.assertRaises(ValueError):
            verify(Path(root), self.expected)

    def test_rejects_malformed_xml(self):
        with TemporaryDirectory() as root:
            directory = Path(root)
            (directory / "TEST-broken.xml").write_text("<testsuite>")
            with self.assertRaises(ET.ParseError):
                verify(directory, self.expected)


if __name__ == "__main__":
    unittest.main()
