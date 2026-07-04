# CLAUDE.md — read this before touching anything

OpenGallery (`foss.opengallery.app`) — a free, GPL-3.0, local-first Android gallery
with a One UI-style design. Repo: https://github.com/TonmoyBishwas/foss.opengallery.app
Current release: **v1.0.2** (versionCode 3). The end goal is a **Play Store launch**.

Deep docs live in `docs/`:

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — codebase map, data flow, key subsystems
- [docs/PHILOSOPHY.md](docs/PHILOSOPHY.md) — what this app is and deliberately is not
- [docs/PITFALLS.md](docs/PITFALLS.md) — bugs that already happened once; do not repeat them
- [docs/PLAY-STORE.md](docs/PLAY-STORE.md) — the launch roadmap

## Build

The system Java on this machine is 1.8 and **cannot build the project**. Always use
Android Studio's JBR (Java 21):

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew assembleRelease
```

Release builds are R8-minified with ABI splits (arm64-v8a / armeabi-v7a / x86_64 /
universal); the arm64 APK is ~13 MB. Signing reads the untracked `keystore.properties`
in the repo root.

## Git identity — critical

Commits MUST be authored as **Tonmoy Bishwas &lt;ttonmoy46@gmail.com&gt;**. The repo-local
`git config user.name/user.email` is already set — do not override it and never commit
with any other email (the entire history had to be rewritten once to fix this).

**Never commit:** `opengallery-release.jks`, `keystore.properties`, `local.properties`.
They are gitignored, but check `git status` before any broad `git add`.

## Release checklist

1. Bump `versionCode` (+1 every release, Play requires it) and `versionName` in
   `app/build.gradle.kts`.
2. Add a `CHANGELOG.md` entry.
3. `assembleRelease`, then copy APKs from `app/build/outputs/apk/release/` to the repo
   root as `OpenGallery-X.Y.Z-{arm64-v8a,armeabi-v7a,universal}.apk`.
4. Commit, tag `vX.Y.Z`, push branch and tag.
5. `gh release create vX.Y.Z <apks> --title ... --notes ...` — run from the repo root
   with absolute paths (the shell cwd resets between tool calls; this has broken
   release uploads twice).

For Play Store: build an AAB with `bundleRelease` instead — see docs/PLAY-STORE.md.

## Testing on the user's phone

Android 11+ Wireless debugging. Pairing survives, but the connect port changes every
time the toggle flips: discover with `adb mdns services`, then `adb connect ip:port`.
If never paired on this PC, ask the user for the pairing code while they keep the
pairing dialog open (the pairing mDNS service only advertises while it's visible).
**Never port-scan the user's network.**

## Hard rules (non-negotiable)

- No ads, telemetry, analytics, cloud services, accounts, or API keys — ever.
- Only original assets. All icons are hand-drawn Canvas code in
  `ui/components/OgIcons.kt`. Never copy Samsung/One UI icons, fonts, or branding,
  and never mention Samsung in user-facing text or store listings.
- The editor never overwrites originals (save-as-copy, EXIF preserved).
- Locked Folder stays app-private and excluded from backups.
- Deletes go through the recycle bin, not straight to permanent deletion.
