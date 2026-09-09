#!/usr/bin/env python3
"""Build device-targeted split APK sets and record bundletool download estimates."""
import argparse
import csv
import hashlib
import io
import json
import subprocess
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    parser.add_argument("--device-spec", type=Path, required=True)
    parser.add_argument("--aapt2", type=Path)
    args = parser.parse_args()
    output = args.output.resolve()
    directory = ROOT / "build/core-bundle-measurement"
    directory.mkdir(parents=True, exist_ok=True)
    device_path = args.device_spec.resolve()
    device = json.loads(device_path.read_text())
    if not all(device.get(key) for key in ("supportedAbis", "sdkVersion", "screenDensity", "supportedLocales")):
        parser.error("a complete device specification is required")

    def bundletool(arguments, label):
        argument_path = directory / (label + "-arguments.json")
        argument_path.write_text(json.dumps(arguments) + "\n")
        result = subprocess.run(
            [str(ROOT / "gradlew"), "--quiet", ":samples:core-footprint:bundletool",
             "-PfootprintBundletoolArgsFile=" + str(argument_path)],
            cwd=ROOT, capture_output=True, text=True,
        )
        (directory / (label + ".log")).write_text(result.stdout + result.stderr)
        if result.returncode:
            raise RuntimeError("bundletool failed; see " + str(directory / (label + ".log")))
        return result.stdout

    tool_version = bundletool(["version"], "version").strip()
    if tool_version != "1.18.3":
        parser.error("unexpected bundletool version: " + tool_version)
    aapt2_version = None
    if args.aapt2:
        version_result = subprocess.run([str(args.aapt2.resolve()), "version"], text=True, capture_output=True, check=True)
        aapt2_version = version_result.stdout.strip() or version_result.stderr.strip()
        if not aapt2_version:
            parser.error("aapt2 did not report its version")
    reports = {}
    for variant in ("baseline", "embedded"):
        bundle = ROOT / f"samples/core-footprint/build/outputs/bundle/{variant}Release/core-footprint-{variant}-release.aab"
        with zipfile.ZipFile(bundle) as contents:
            names = contents.namelist()
            fixture_hash = hashlib.sha256(contents.read("base/assets/fapi.json")).hexdigest()
            core = None
            if "base/assets/clerk-core.js" in names:
                manifest = json.loads(contents.read("base/assets/core-manifest.json"))
                core = {
                    "sha256": hashlib.sha256(contents.read("base/assets/clerk-core.js")).hexdigest(),
                    "revision": manifest["coreRevision"],
                    "contractHash": manifest["contractHash"],
                }
                if core["sha256"] != manifest["bundleSHA256"]:
                    parser.error("bundle contains a core hash mismatch")
            if (variant == "baseline") != (core is None):
                parser.error("only the embedded variant may contain the core")
        archive = directory / (variant + ".apks")
        command = ["build-apks", "--bundle=" + str(bundle), "--output=" + str(archive),
                   "--device-spec=" + str(device_path), "--overwrite"]
        if args.aapt2:
            command.append("--aapt2=" + str(args.aapt2.resolve()))
        bundletool(command, variant + "-build")
        text = bundletool(["get-size", "total", "--apks=" + str(archive),
                           "--device-spec=" + str(device_path)], variant + "-size")
        lines = text.splitlines()
        try:
            index = lines.index("MIN,MAX")
            row = next(csv.DictReader(io.StringIO("\n".join(lines[index:index + 2]))))
            minimum, maximum = int(row["MIN"]), int(row["MAX"])
        except (ValueError, KeyError, StopIteration) as error:
            raise RuntimeError("Expected bundletool MIN,MAX byte output: " + text) from error
        with zipfile.ZipFile(archive) as contents:
            splits = {item.filename: {"bytes": item.file_size,
                      "sha256": hashlib.sha256(contents.read(item.filename)).hexdigest()}
                      for item in contents.infolist() if item.filename.endswith(".apk")}
        reports[variant] = {
            "fixtureSHA256": fixture_hash, "core": core,
            "bundleSHA256": digest(bundle), "bundleBytes": bundle.stat().st_size,
            "apkSetSHA256": digest(archive), "splits": splits,
            "estimatedDownloadBytes": {"minimum": minimum, "maximum": maximum},
        }
    if reports["baseline"]["fixtureSHA256"] != reports["embedded"]["fixtureSHA256"]:
        parser.error("matched bundles must have identical fixture assets")
    # Subtract both ends conservatively if bundletool leaves a configuration range.
    delta = {
        "minimum": reports["embedded"]["estimatedDownloadBytes"]["minimum"] - reports["baseline"]["estimatedDownloadBytes"]["maximum"],
        "maximum": reports["embedded"]["estimatedDownloadBytes"]["maximum"] - reports["baseline"]["estimatedDownloadBytes"]["minimum"],
    }
    report = {
        "schemaVersion": 1, "bundletoolVersion": tool_version, "aapt2Version": aapt2_version,
        "deviceSpec": device, "deviceSpecSHA256": digest(device_path),
        "variants": reports, "incrementalEstimatedDownloadBytes": delta,
        "downloadBudgetBytes": 5 * 1024 * 1024,
        "withinBudgetForThisConfiguration": delta["maximum"] <= 5 * 1024 * 1024,
        "limitations": ["bundletool estimate, not an observed Play network transfer",
                        "matched API-only R8 harness for the recorded device configuration",
                        "does not establish physical-device latency, memory, upgrades or all ABI acceptance"],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(delta))


if __name__ == "__main__":
    main()
