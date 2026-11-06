# SmartTube (uroge)

An independently signed, security-maintained fork of
[GeorgeHarbor/SmartTube](https://github.com/GeorgeHarbor/SmartTube), based on
[yuliskov/SmartTube](https://github.com/yuliskov/SmartTube).

**Android 6.0/API 23 or newer. Private testing until the release checklist passes.**

The app retains SmartTube playback and optional community services. This fork adds
local QR generation, explicit approval for new casting pairings, encrypted account
storage, settings-only backups, strict HTTPS, and verified updates from this
repository. App locks have no master-password bypass. Firebase crash reporting is
removed. See the [privacy policy](PRIVACY.md) for network services and permissions.

## Build

Clone the complete source, including pinned submodules:

```sh
git clone --recurse-submodules https://github.com/uroge/SmartTube.git
cd SmartTube
python3 security/check-source.py
./gradlew :smarttubetv:assembleStbetaRelease
```

Use JDK 17, the included Gradle wrapper, Android platform 34 and build-tools 30.0.3.
Configure your signing key before installing a release APK. See
[SECURITY_BUILD.md](SECURITY_BUILD.md) for test commands, signing, updates and
reproducibility details. Application IDs are `io.github.uroge.smarttube.beta`,
`.stable` and `.fdroid`; this installs separately from other maintainers' builds.
Sign in again after installation. Settings imports never import account credentials.

## Updates and maintenance

Beta and stable builds check only this repository's release manifests. Updates
must match the installed signing certificate, package ID, manifest version and
SHA-256. The F-Droid flavor has no in-app updater.

Upstream changes are proposed through a daily review branch. They do not merge
or publish automatically. CI checks source regressions, unit tests, lint, APK
contents and resolved dependency advisories. Release signing secrets are confined
to a maintainer-approved GitHub environment.

Complete the [device checklist](security/DEVICE_TEST_RESULTS.md) on the exact
release commit before enabling publication. In particular, compilation alone does
not verify real TV playback, phone casting or Android Keystore behavior.

Licensed under the [MIT license](LICENSE). Upstream contributors retain their
copyrights and attribution in the source.
