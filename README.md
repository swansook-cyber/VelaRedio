# Vela Radio

Private-use Thai music internet radio for Android phones and car head units.

## Status
Vela Radio 1.3.7 (version code 21) is an internal QA release candidate. No production deployment or store publication is authorized.

## Plan
1. Build release APKs only with the VelaLab V2 keystore and alias `velalab-release`.
2. Set `VELA_RELEASE_KEYSTORE_FILE`, `VELA_RELEASE_STORE_PASSWORD`, and `VELA_RELEASE_KEY_PASSWORD` in the local process or the corresponding GitHub Actions secrets. Never commit the keystore or passwords.
3. Verify the APK package, version, and signer certificate against the VelaLab Signing Registry before giving it to QA.
4. Test background playback and hardware media keys on physical devices.

Do not merge into main or publish to Vela App Store without explicit approval.
