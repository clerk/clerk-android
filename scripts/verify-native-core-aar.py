#!/usr/bin/env python3
"""Check the release AAR's packaging contract; does not prove device execution."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import xml.etree.ElementTree as ET
from zipfile import ZipFile


ABIS = {"arm64-v8a": (2, 183), "armeabi-v7a": (1, 40), "x86_64": (2, 62)}
ANDROID = "{http://schemas.android.com/apk/res/android}"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def inspect_elf(raw, abi):
    elf_class, machine = ABIS[abi]
    require(raw[:6] == b"\x7fELF" + bytes((elf_class, 1)), f"{abi}: incorrect ELF class or byte order")
    require(struct.unpack_from("<HH", raw, 16) == (3, machine), f"{abi}: incorrect library type or machine")
    if elf_class == 2:
        offset = struct.unpack_from("<Q", raw, 32)[0]
        entry_size, count = struct.unpack_from("<HH", raw, 54)
        layout = "<IIQQQQQQ"
    else:
        offset = struct.unpack_from("<I", raw, 28)[0]
        entry_size, count = struct.unpack_from("<HH", raw, 42)
        layout = "<IIIIIIII"
    require(entry_size >= struct.calcsize(layout), f"{abi}: invalid program header size")
    segments = []
    for index in range(count):
        fields = struct.unpack_from(layout, raw, offset + index * entry_size)
        if fields[0] != 1:  # PT_LOAD
            continue
        file_offset, address = fields[2:4] if elf_class == 2 else fields[1:3]
        alignment = fields[-1]
        require(alignment >= 16384 and alignment & (alignment - 1) == 0,
                f"{abi}: load segment lacks 16 KB alignment")
        require(file_offset % 16384 == address % 16384, f"{abi}: load segment has incompatible offsets")
        segments.append(alignment)
    require(segments, f"{abi}: missing load segments")
    return {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "loadSegmentAlignments": segments}


def inspect(path):
    with ZipFile(path) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)), "Duplicate AAR entries")
        manifest = ET.fromstring(archive.read("AndroidManifest.xml"))
        sdk = manifest.find("uses-sdk")
        require(sdk is not None and sdk.get(ANDROID + "minSdkVersion") == "24", "Expected minSdk 24")
        permissions = {node.get(ANDROID + "name") for node in manifest.findall("uses-permission")}
        required_permissions = {"android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.USE_BIOMETRIC"}
        require(required_permissions <= permissions, f"Missing permissions: {sorted(required_permissions - permissions)}")
        expected = {f"jni/{abi}/libclerk_quickjs.so" for abi in ABIS}
        require({name for name in names if name.startswith("jni/") and name.endswith(".so")} == expected,
                "Expected exactly arm64-v8a, armeabi-v7a and x86_64 QuickJS libraries")
        libraries = {abi: inspect_elf(archive.read(f"jni/{abi}/libclerk_quickjs.so"), abi) for abi in ABIS}
        core = json.loads(archive.read("assets/core-manifest.json"))
        bundle = archive.read("assets/clerk-core.js")
        require(hashlib.sha256(bundle).hexdigest() == core["bundleSHA256"], "Packaged core hash mismatch")
        require(len(bundle) == core["bundleBytes"], "Packaged core size mismatch")
        require(hashlib.sha256(archive.read("assets/THIRD_PARTY_NOTICES.txt")).hexdigest() == core["thirdPartyNoticesSHA256"],
                "Packaged third-party notice hash mismatch")
        require(bool(archive.read("assets/QuickJS-LICENSE.txt").strip()), "Missing QuickJS license")
    return {"aar": str(path), "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "minSdk": 24, "requiredPermissions": sorted(required_permissions), "libraries": libraries,
            "coreRevision": core["coreRevision"], "bundleSHA256": core["bundleSHA256"]}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("aar", type=Path)
    args = parser.parse_args()
    print(json.dumps(inspect(args.aar), indent=2))
