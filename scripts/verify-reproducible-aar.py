#!/usr/bin/env python3
"""Build committed Android sources twice in fresh paths and require identical release AARs."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import tarfile
import tempfile
from zipfile import ZipFile


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def entry_hashes(path):
    with ZipFile(path) as archive:
        return {name: hashlib.sha256(archive.read(name)).hexdigest() for name in archive.namelist()}


def verify(repo, output):
    # Archives deliberately include only the committed source. Reject modified tracked inputs.
    subprocess.run(["git", "diff", "--quiet", "HEAD", "--"], cwd=repo, check=True)
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=repo, text=True).strip()
    output.mkdir(parents=True, exist_ok=True)
    if any(output.iterdir()):
        raise ValueError(f"Evidence directory must be empty: {output}")
    spec = importlib.util.spec_from_file_location("aar_contract", repo / "scripts/verify-native-core-aar.py")
    contract = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(contract)
    environment = os.environ.copy()
    environment.pop("CLERK_INTEGRATION_TEST_PK", None)
    report = {"sourceRevision": revision, "host": platform.platform(), "builds": [],
              "method": "Two git archives in different fresh paths; task and configuration caches disabled"}
    try:
        with tempfile.TemporaryDirectory(prefix="clerk-aar-repro-") as temporary:
            workspace = Path(temporary)
            archive = workspace / "source.tar"
            subprocess.run(["git", "archive", "--format=tar", "HEAD", "--output", str(archive)], cwd=repo, check=True)
            report["sourceArchiveSHA256"] = digest(archive)
            for index, name in enumerate(("first", "second-longer-path"), start=1):
                source = workspace / name
                source.mkdir()
                with tarfile.open(archive) as contents:
                    contents.extractall(source, filter="data")
                log = output / f"build-{index}.log"
                command = [str(source / "gradlew"), ":source:api:assembleRelease", "--no-build-cache",
                           "--no-configuration-cache", "--rerun-tasks"]
                print(f"Building clean source {index}/2; log: {log}", flush=True)
                with log.open("w") as stream:
                    subprocess.run(command, cwd=source, env=environment, stdout=stream,
                                   stderr=subprocess.STDOUT, check=True)
                artifact = output / f"api-release-{index}.aar"
                shutil.copyfile(source / "source/api/build/outputs/aar/api-release.aar", artifact)
                inspected = contract.inspect(artifact)
                inspected["aar"] = artifact.name
                report["builds"].append(inspected)
                if index == 1:
                    report["gradleVersion"] = subprocess.check_output(
                        [str(source / "gradlew"), "--version"], cwd=source, env=environment, text=True).strip()
            first, second = (output / f"api-release-{index}.aar" for index in (1, 2))
            left, right = entry_hashes(first), entry_hashes(second)
            report["differentEntries"] = sorted(name for name in left.keys() | right.keys()
                                                if left.get(name) != right.get(name))
            report["identical"] = digest(first) == digest(second)
            if not report["identical"]:
                raise ValueError("Release AAR bytes differ; inspect differentEntries in report.json")
    finally:
        (output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    print(f"Both clean release AARs are byte-identical: {report['builds'][0]['sha256']}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path, help="Fresh directory for both AARs, logs and report")
    args = parser.parse_args()
    try:
        verify(Path(__file__).resolve().parents[1], args.output.resolve())
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        raise SystemExit(f"Android reproducibility verification failed: {error}") from None
