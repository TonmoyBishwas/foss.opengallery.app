# Play Store launch roadmap

The main goal. Everything here is ordered — work top to bottom.

## 1. Build an AAB, not APKs

Play requires an Android App Bundle for new apps. The GitHub ABI-split APKs stay for
sideloaders/F-Droid-style users, but for Play:

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

The existing signing config applies to the bundle automatically.

## 2. Play App Signing + key safety

- On first upload, enroll in **Play App Signing** (default and recommended): Google
  holds the app signing key; `opengallery-release.jks` becomes the *upload key*,
  which is replaceable if lost.
- Until enrolled, losing `opengallery-release.jks` = losing the app identity
  permanently. Keep an encrypted off-machine backup of the keystore and
  `keystore.properties` **now**.

## 3. Console declarations that will block review if skipped

- **Photo and Video Permissions declaration**: `READ_MEDIA_IMAGES` /
  `READ_MEDIA_VIDEO` are restricted; the app must declare its core use case. A
  gallery is *the* canonical allowed case, but the form must be filled in App
  content → Photo and video permissions, or the release is rejected.
- **Data safety form**: truthfully declare *no data collected, no data shared* —
  everything is on-device. Note: ML Kit runs on-device via Play services and Google
  documents it as not requiring data-collection disclosure for on-device APIs, but
  re-verify the current ML Kit disclosure guidance when filling the form.
- **Privacy policy URL**: mandatory (the app uses sensitive permissions). A simple
  GitHub Pages page in the repo (e.g. `docs/privacy-policy.md` published via Pages)
  stating that no data leaves the device is enough.
- **Content rating questionnaire**: user-generated-content questions answer "no" —
  the app only displays the user's own local files.
- **Target API**: policy requires targeting a recent API level; targetSdk 36 already
  satisfies it. versionCode must increase with every upload.

## 4. Store listing

- **Title**: "OpenGallery" (check availability at submission; add a qualifier like
  "OpenGallery — Photo Gallery" if needed).
- **Never mention Samsung or One UI** anywhere in the listing — impersonation policy.
  "Familiar, polished design" is safe wording.
- Assets: 512×512 icon (the existing launcher art, exported hi-res), 1024×500
  feature graphic, 4–8 phone screenshots (our own UI — all original, safe to use).
- Category: **Photography**. 
- Short description ≤ 80 chars, e.g. "Fast, private, ad-free gallery. Your photos
  never leave your phone."
- Full description: lead with no-ads/no-cloud/offline, then features (editor,
  Locked Folder, People/OCR search, recycle bin, nested folders), then GPL/open
  source with the GitHub link.

## 5. Testing tracks before production

1. **Internal testing** track first: upload the AAB, install via Play on the user's
   phone — this also verifies Play-delivered signing + R8 behave identically.
2. Read the **pre-launch report**: it runs the app on real devices across API
   levels. Watch specifically for crashes on API 26–28 (legacy trash/favourites
   path) and Android 14+ partial media access — the least-tested paths
   (see docs/PITFALLS.md).
3. Fix, re-upload (versionCode +1), then promote to **closed/open testing** or
   straight to production.

## 6. Post-launch

- Keep GitHub releases in lockstep with Play releases (same tag, same changelog).
- Play "Production" rollouts can be staged (e.g. 20% → 100%) — use it once there are
  real users.
- Crash visibility with zero telemetry: there is deliberately no Crashlytics.
  Play Console's Android Vitals (opt-in OS-level reporting) is the only crash
  signal — check it after each release.
- Future F-Droid submission note: ML Kit is closed-source, so main-repo F-Droid
  would need a build flavor without it (features already degrade gracefully).

## Suggested pre-submission checklist

- [ ] Keystore + keystore.properties backed up off-machine
- [ ] `bundleRelease` AAB built and smoke-tested (docs/PITFALLS.md checklist, release build)
- [ ] Privacy policy page live, URL ready
- [ ] Photo/Video permissions declaration drafted
- [ ] Data safety answers drafted
- [ ] Listing text + screenshots + feature graphic ready (no Samsung mentions)
- [ ] Internal testing pass on the user's device via Play
