# VelaLab private APK archive

For each successful Android release build, validate the signed APK and stage the named APK, QA report, and SHA-256 manifest before archiving. In this repository, run `scripts/archive_release.py` with the expected package, version, code, and V2 certificate fingerprint. The script rejects mismatches and refuses to reuse an output directory.

Upload to the owner's private Google Drive path `VelaLab / APK Releases / VelaRadio / VERSION /`. Confirm the folder parent chain and owner-only permission. Read back the uploaded file, check its size, and download it through the authenticated connector to compare SHA-256. Preserve old APKs and never replace or delete a Drive file automatically. If Drive is unavailable, retain the local files and report that the archive is pending.

The signed-release GitHub Actions workflow stages these files as a GitHub artifact. The Codex Drive connection is not available to GitHub Actions; a connected Codex run performs the private Drive upload and verification. A separate, scoped Drive credential would be required for unattended CI upload.

Report the Drive APK link with branch, commit, version, CI result, and certificate. Keep signing keys and credentials out of the APK archive. Archive upload grants no permission to merge, deploy, or publish.
