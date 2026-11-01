# VelaLab APK release archive policy

Apply this to every VelaLab Android release build in this repository. Extend it to another VelaLab Android repository when working on that repository; do not modify other apps merely to roll out this policy.

1. Before any upload, verify package name, version name, version code, APK signature, expected signing certificate SHA-256, and APK SHA-256. Stop on any mismatch.
2. Stage an APK named `AppName-vVERSION-codeCODE-release.apk`, `QA-Report.md`, and `SHA256SUMS.txt`. `scripts/archive_release.py` performs the checks and stages these files for Vela Radio.
3. When the owner's private Google Drive connection is available and authorized, upload the three files to `VelaLab / APK Releases / AppName / VERSION /`. Reuse existing folders after checking their exact parent and name. Create new files only; never overwrite or delete older artifacts. Do not upload a duplicate name without inspecting the existing file and its hash.
4. Read back Drive metadata to confirm the expected folder, filename, size, and owner-only permissions. Download the uploaded APK through the authenticated Drive connector and compare its SHA-256 to the local staged file. Report archive success only after that check. If upload or readback fails, keep the local staged files and report the failure.
5. Include the private Drive APK link, branch, commit, package, version, CI result, APK SHA-256, and signing certificate SHA-256 in the release report. Do not enable public or link sharing automatically.
6. Never upload keystores, private keys, signing passwords, or GitHub secrets to `APK Releases`. Archive upload does not authorize merging main, production deployment, or Vela App Store publication.

GitHub Actions cannot access the owner's Codex Google Drive connection. Its signed release workflow validates and packages the three files as a GitHub Actions artifact. When an authorized Codex session handles the completed build, it should upload and verify the files on private Drive automatically. A future unattended CI-to-Drive uploader requires its own scoped credentials and must be configured separately.
