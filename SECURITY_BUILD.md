# Building and maintaining the uroge fork

This fork targets Android 6/API 23 and newer. Its application IDs are
`io.github.uroge.smarttube.beta`, `.stable` and `.fdroid`. It installs separately
from upstream and GeorgeHarbor builds. Sign in again; do not transfer credentials.

## Reproduce

1. Clone with `git clone --recurse-submodules https://github.com/uroge/SmartTube.git`.
2. Use JDK 17 and the checked-in Gradle 7.5 wrapper. AGP remains 7.4.2;
   standalone D8/R8 is patched to 8.3.37 to handle the security-fixed dependencies. Install Android platform 34
   and build-tools 30.0.3. Set `ANDROID_HOME` to that SDK.
3. Run `python3 security/check-source.py`.
4. Run `./gradlew :smarttubetv:lintStbetaRelease :smarttubetv:testStbetaDebugUnitTest
   :common:testStbetaDebugUnitTest :sharedutils:testDebugUnitTest
   :appupdatechecker2:testStbetaDebugUnitTest :smarttubetv:assembleStbetaRelease`.
5. Repeat lint and assembly for `Ststable` and `Stfdroid`. Use
   `security/inspect-apk.py` to inspect every output. Without signing properties,
   release outputs are unsigned and cannot be installed.

Pinned root submodules are the build inputs; sibling directories are ignored.
No GitHub request runs during Gradle configuration. Do not update submodule
pointers without reviewing their code and running the security tests.

## Signing

Keep the release keystore and password recovery material outside this repository
and copy the recovery material to a secure offline location. Losing this key means
existing users cannot install your future updates over the same application.
The local ignored `keystore.properties` uses `storeFile`, `storePassword`,
`keyAlias` and `keyPassword`.

GitHub Actions uses `SIGNING_KEY` (base64 keystore), `KEY_STORE_PASSWORD`, `ALIAS`
and `KEY_PASSWORD`. Secrets are used only by the protected signed-release job,
never pull-request builds. The `release` environment is restricted to master and
version tags. Source changes are reviewed through pull requests.

## Release gate

First deliver a signed private test build. Complete `security/DEVICE_TEST_RESULTS.md`
against the exact release source, including actual TV and phone testing, traffic
capture and a two-version update test. Then set repository variable
`DEVICE_CHECKS_APPROVED_SHA` to that commit. Until then, publication is blocked.
Do not set this variable based only on successful compilation.

Create a three-part version tag or manually dispatch the release workflow with a
three-part version. CI computes an increasing version code, reruns verification,
signs all flavors, and publishes beta/stable APKs to immutable versioned URLs.
The rolling release contains only manifests, updated after all checks succeed.
The F-Droid flavor is built and inspected but is not advertised in the updater.

Every release contains APK hashes, source/submodule provenance and the signing
certificate fingerprint. Invalid signers, package IDs, hashes and downgrades fail
closed. Rollbacks use a higher-version corrective release; never restore plaintext
credentials.

## Dependency and device checks

`security/dependency-audit.py` queries OSV using the resolved Maven production
dependency tree. It fails on unresolved advisories. A reachability exception needs
an exact package/version/advisory, reviewed commit and concrete rationale in
`security/dependency-exceptions.json`. Do not add blanket exceptions.

Device credential tests live in SharedModules/sharedutils/src/androidTest. Run
`:sharedutils:connectedDebugAndroidTest` on API 23/24 and current Android. Java
unit tests do not prove that the TV's Keystore, TLS provider or casting sender work.
