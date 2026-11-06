# Release device checks — pending

No device result may be inferred from compilation or unit tests.

- [ ] Android 6: strict TLS, Keystore migration, restart, sign-in and playback
- [ ] Android 7: reject untrusted and wrong-host certificates; playback
- [ ] Current Android TV emulator: install and main navigation flows
- [ ] Actual TV box: playback formats, seeking, subtitles and voice search
- [ ] YouTube phone sender: approval, denial, timeout, background rejection, existing session
- [ ] Credentials: account switching, refresh, removal, invalidated key recovery
- [ ] Local and Drive settings backup/restore, with credentials and passwords excluded
- [ ] Release network capture and logs: no external QR/Firebase/credential disclosure
- [ ] Signed update between two private versions; wrong signer/package/downgrade rejected

Record device model, Android version, source SHA, APK SHA-256, date and result for each check.
After independently completing these checks, set repository variable
`DEVICE_CHECKS_APPROVED_SHA` to the exact release commit SHA. The release workflow
fails closed unless this matches its source SHA. Do not set it for untested builds.
