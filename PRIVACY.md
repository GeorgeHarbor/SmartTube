# Privacy — SmartTube (uroge)

This policy covers the independently signed `io.github.uroge.smarttube` flavors.
The fork contains no Firebase/Crashlytics telemetry. QR images are generated locally;
activation codes are never sent to an external QR-image service.

YouTube and Google receive normal playback, search, authentication and account
requests, including your IP address. Signed-in activity may be associated with your
Google account. Sign-in uses Google/YouTube activation flows; this fork does not
collect your Google password. Account records are encrypted with Android Keystore
keys and kept in app-private storage excluded from backups.

Optional SponsorBlock, DeArrow and Return YouTube Dislike features keep their
existing defaults. When used, their operators receive requests containing video
IDs or related video metadata, and see network metadata such as your IP address.
These are third-party services with their own policies. Their requests do not carry
Google Authorization or Cookie headers. Disable them in settings if undesired.

Google Drive backup is optional and uses Google authentication. Both Drive and
local backups contain allowlisted settings only, excluding account credentials,
app passwords and casting identities. Settings restore does not restore sign-ins.
Old backups created by other versions may still contain credentials; this fork
cannot remove copies you have already exported or uploaded.

DIAL discovery, when enabled, advertises the TV device on its LAN. Every new DIAL
pairing needs approval on the foreground TV screen; existing approved sessions
continue through YouTube's remote-control service. Casting requires a trusted LAN.

The beta and stable updaters contact GitHub releases controlled by `uroge`.
GitHub sees ordinary request metadata. Updates are checked for package identity,
version, SHA-256 and the installed app's signing certificate before installation.
The F-Droid flavor has no in-app update feed. Public documentation, donations and
external links contact their destination only when opened.

Microphone access supports voice search; the device's speech recognition provider
may process audio under its own policy. Storage and overlay access are used only
by features that require them. Release builds disable verbose diagnostic logging.

Encryption cannot protect a running app from a compromised operating system.
Local app locks are convenience access controls, not an OS security boundary.
