#!/usr/bin/env python3
"""Compare matched release APKs; transfer estimates are not Play delivery measurements."""
import argparse
import gzip
import hashlib
import json
import struct
from pathlib import Path
import zipfile


def elf_sections(raw):
    if raw[:4] != b"\x7fELF" or raw[5] != 1:
        raise ValueError("expected a little-endian ELF library")
    if raw[4] == 2:
        section_offset = struct.unpack_from("<Q", raw, 40)[0]
        entry_size, count, names_index = struct.unpack_from("<HHH", raw, 58)
        offset_field, size_field, scalar = 24, 32, "<Q"
    elif raw[4] == 1:
        section_offset = struct.unpack_from("<I", raw, 32)[0]
        entry_size, count, names_index = struct.unpack_from("<HHH", raw, 46)
        offset_field, size_field, scalar = 16, 20, "<I"
    else:
        raise ValueError("unsupported ELF class")
    names_header = section_offset + names_index * entry_size
    names_offset = struct.unpack_from(scalar, raw, names_header + offset_field)[0]
    names_size = struct.unpack_from(scalar, raw, names_header + size_field)[0]
    names = raw[names_offset:names_offset + names_size]
    return [names[struct.unpack_from("<I", raw, section_offset + i * entry_size)[0]:].split(b"\0", 1)[0].decode("ascii") for i in range(count)]


def inspect(path):
    raw = path.read_bytes()
    with zipfile.ZipFile(path) as archive:
        entries = archive.infolist()
        groups = {}
        for entry in entries:
            name = entry.filename
            group = (
                "native" if name.startswith("lib/") else
                "dex" if name.endswith(".dex") else
                "core" if name == "assets/clerk-core.js" else
                "other"
            )
            values = groups.setdefault(group, {"compressedBytes": 0, "expandedBytes": 0})
            values["compressedBytes"] += entry.compress_size
            values["expandedBytes"] += entry.file_size
        native = [entry.filename for entry in entries if entry.filename.startswith("lib/") and entry.filename.endswith(".so")]
        debug_sections = {name: [section for section in elf_sections(archive.read(name)) if section.startswith((".debug_", ".zdebug_"))] for name in native}
        core = archive.read("assets/clerk-core.js") if "assets/clerk-core.js" in archive.namelist() else None
        return {
            "apk": str(path),
            "sha256": hashlib.sha256(raw).hexdigest(),
            "apkBytes": len(raw),
            "gzipApkBytes": len(gzip.compress(raw, compresslevel=9, mtime=0)),
            "expandedEntryBytes": sum(entry.file_size for entry in entries),
            "groups": groups,
            "nativeDebugSections": debug_sections,
            "abis": sorted({entry.filename.split("/")[1] for entry in entries if entry.filename.startswith("lib/")}),
            "fixtureSHA256": hashlib.sha256(archive.read("assets/fapi.json")).hexdigest(),
            "core": None if core is None else {
                "sha256": hashlib.sha256(core).hexdigest(),
                "bytes": len(core),
                "gzipBytes": len(gzip.compress(core, compresslevel=9, mtime=0)),
            },
        }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("embedded", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--abi", required=True)
    parser.add_argument("--installed-log", type=Path, help="Fresh-run ClerkCoreFootprint log containing one size sample per variant")
    args = parser.parse_args()
    baseline, embedded = inspect(args.baseline), inspect(args.embedded)
    if baseline["core"] is not None or baseline["abis"]:
        parser.error("baseline unexpectedly contains the core or a native library")
    if embedded["core"] is None or embedded["abis"] != [args.abi]:
        parser.error("embedded APK must contain the core and exactly the requested ABI")
    if any(embedded["nativeDebugSections"].values()):
        parser.error("release APK contains unstripped native debug sections")
    if baseline["fixtureSHA256"] != embedded["fixtureSHA256"]:
        parser.error("the matched APKs must contain identical fixture assets")
    report = {
        "schemaVersion": 1,
        "abi": args.abi,
        "baseline": baseline,
        "embedded": embedded,
        "delta": {key: embedded[key] - baseline[key] for key in ("apkBytes", "gzipApkBytes", "expandedEntryBytes")},
        "compressedBundleBudget": {"maximumBytes": 384 * 1024, "passed": embedded["core"]["gzipBytes"] <= 384 * 1024},
        "limitations": [
            "gzip APK bytes estimate compression, not Play delivery or an App Bundle split install",
            "expanded ZIP entry bytes are not installed application size; they exclude runtime compilation and package-manager storage",
            "minimal R8 release app; retained default platform hosts, generated local reset, fixture startup; no prebuilt Compose UI assets",
            "no startup latency, memory or physical-device acceptance claim",
        ],
    }
    if args.installed_log:
        installed = {}
        for line in args.installed_log.read_text().splitlines():
            marker = line.find('{"package":')
            if marker < 0:
                continue
            sample = json.loads(line[marker:])
            package = sample.get("package")
            if package not in ("com.clerk.footprint.baseline", "com.clerk.footprint.embedded"):
                continue
            if package in installed:
                parser.error("installed log contains repeated samples; provide only one fresh run")
            installed[package] = sample
        if len(installed) != 2:
            parser.error("installed log must contain a size sample from each variant")
        first, second = (installed["com.clerk.footprint." + variant] for variant in ("baseline", "embedded"))
        if first["abi"] != args.abi or second["abi"] != args.abi or first["sdk"] != second["sdk"]:
            parser.error("installed samples must match the requested ABI and Android API level")
        delta = second["appBytes"] - first["appBytes"]
        report["installedSize"] = {
            "baseline": first,
            "embedded": second,
            "deltaAppBytes": delta,
            "budgetBytes": 8 * 1024 * 1024,
            "withinBudgetForThisSample": delta <= 8 * 1024 * 1024,
            "limitation": "one device's own-UID appBytes after first launch; excludes data/cache, is not all device or Play split-install acceptance",
        }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({"delta": report["delta"], "core": embedded["core"]}, indent=2))


if __name__ == "__main__":
    main()
