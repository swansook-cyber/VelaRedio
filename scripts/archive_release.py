#!/usr/bin/env python3
"""Validate a signed APK and stage immutable private-archive deliverables."""

import argparse
import hashlib
import re
import shutil
import subprocess
from pathlib import Path


def run(*args: str) -> str:
    return subprocess.run(args, check=True, text=True, capture_output=True).stdout


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--app", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--code", required=True, type=int)
    parser.add_argument("--certificate-sha256", required=True)
    parser.add_argument("--branch", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--ci-url", default="Local verified build")
    parser.add_argument("--apksigner", default="apksigner")
    parser.add_argument("--aapt", default="aapt")
    args = parser.parse_args()

    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9]*", args.app):
        parser.error("app must be one simple folder/filename component")
    if not re.fullmatch(r"[0-9]+(?:\.[0-9]+)*", args.version):
        parser.error("version must be numeric dot-separated")
    if not args.apk.is_file():
        parser.error("APK does not exist")
    expected_cert = re.sub(r"[^0-9a-fA-F]", "", args.certificate_sha256).lower()
    if not re.fullmatch(r"[0-9a-f]{64}", expected_cert):
        parser.error("certificate SHA-256 must contain 64 hex digits")

    signature = run(args.apksigner, "verify", "--verbose", "--print-certs", str(args.apk))
    match = re.search(r"^Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})$", signature, re.M)
    if not match or match.group(1).lower() != expected_cert:
        raise SystemExit("APK signing certificate does not match expected VelaLab certificate")
    if "Number of signers: 1" not in signature or "Verified using v2 scheme (APK Signature Scheme v2): true" not in signature:
        raise SystemExit("APK signature verification did not confirm one v2 signer")

    badging = run(args.aapt, "dump", "badging", str(args.apk))
    package = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging, re.M)
    if not package or package.groups() != (args.package, str(args.code), args.version):
        raise SystemExit("APK package, version code, or version name mismatch")

    name = f"{args.app}-v{args.version}-code{args.code}-release.apk"
    if args.output_dir.exists():
        raise SystemExit("Archive staging directory already exists; refusing overwrite")
    args.output_dir.mkdir(parents=True)
    staged = args.output_dir / name
    with args.apk.open("rb") as source, staged.open("xb") as destination:
        shutil.copyfileobj(source, destination)
    apk_hash = sha256(args.apk)
    if sha256(staged) != apk_hash:
        raise SystemExit("Staged APK hash mismatch")

    manifest = args.output_dir / "SHA256SUMS.txt"
    with manifest.open("x", encoding="utf-8", newline="\n") as file:
        file.write(f"{apk_hash}  {name}\n")
    report = args.output_dir / "QA-Report.md"
    with report.open("x", encoding="utf-8", newline="\n") as file:
        file.write(
            f"# {args.app} v{args.version} internal QA\n\n"
            f"- APK: `{name}`\n"
            f"- Package: `{args.package}`\n"
            f"- Version: `{args.version}` (code `{args.code}`)\n"
            f"- Certificate SHA-256: `{expected_cert.upper()}`\n"
            f"- APK SHA-256: `{apk_hash}`\n"
            f"- Branch: `{args.branch}`\n"
            f"- Commit: `{args.commit}`\n"
            f"- CI: {args.ci_url}\n"
            "- Validation: apksigner verified one v2 signer; aapt confirmed package and version.\n"
            "- Distribution: internal QA only; no main merge or store publication.\n"
        )
    print(f"Validated and staged {name}; SHA-256 {apk_hash}")


if __name__ == "__main__":
    main()
