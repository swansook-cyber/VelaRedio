# VelaLab Android signing V2

V2 is the sole key for new VelaLab Android release builds. The key and credentials stay outside Git. The authoritative certificate and keystore hashes are in the private VelaLab Signing Registry; a release must match its certificate SHA-256 before distribution.

## Vela Radio 1.3.6

- Package: `net.velalab.radio`; version code: `20`; version name: `1.3.6`.
- Signing alias: `velalab-release`; keystore: `velalab-release-v2.jks`.
- Local signing uses `VELA_RELEASE_KEYSTORE_FILE`, `VELA_RELEASE_STORE_PASSWORD`, and `VELA_RELEASE_KEY_PASSWORD`. Supply them from the approved secret store in the build process; do not place them in a tracked file or shell history.
- The manual GitHub Actions workflow needs `VELA_RELEASE_KEYSTORE_BASE64`, `VELA_RELEASE_STORE_PASSWORD`, and `VELA_RELEASE_KEY_PASSWORD` secrets. It checks the signer against the public V2 certificate fingerprint embedded in the workflow. Configure secrets only through a secure CI secret transfer. Do not print or echo secret values.
- Run `apksigner verify --verbose --print-certs` and `aapt dump badging` on the output. Compare its signer certificate to the registry. QA should install and exercise startup, playback, background audio, and media controls on representative phone, TV, and car devices.

## Next-build migration inventory

The following local Android projects were found under `D:\Project AI` on 2026-11-01. This is an inventory for planning, not a declaration that these are every VelaLab app or that any is ready to release.

| Project | Package or variants | Next-build action |
| --- | --- | --- |
| VelaDrive | `net.velalab.veladrive` | Add V2 signing; verify artifact and update path. |
| VelaFishing | `net.velalab.velafishing` | Add V2 signing; verify artifact and update path. |
| VelaMovie | `net.velalab.velamovie` | Replace release signer configuration with V2; verify artifact and update path. |
| VelaStreamSports | `com.velastreamsports.poc` | Replace release signer configuration with V2; verify artifact and update path. |
| VelaStream-TV | `net.velalab.velastream.tv` | Replace release signer configuration with V2; verify artifact and update path. |
| VelaWebTV | `net.velalab.webtv` and product-flavor package IDs | Inventory each variant, add V2 signing, verify each artifact and update path. |

For each app, record its currently distributed signing certificate before changing release signing. Android generally rejects an in-place APK update signed by a different certificate. Decide the supported migration route for installed users before shipping V2, such as a platform-supported signing-key rotation or a fresh install with data migration. Then change only that app's next release build, verify package/version/certificate, and record the result in the registry. Do not rebuild other apps as part of the Vela Radio task.
