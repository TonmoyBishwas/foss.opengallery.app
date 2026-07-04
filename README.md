# OpenGallery

**A free, open-source, local-first gallery app for Android with a familiar One UI–style design.**

OpenGallery is a lightweight gallery that keeps everything on your device: no ads, no telemetry, no cloud account, no upsell notifications, no API keys. It aims to give every Android phone (8.0 → 16+) the polished gallery experience of a flagship — plus the power features stock galleries never ship.

> Package: `foss.opengallery.app` · License: GPL-3.0 · Status: **in development, pre-1.0**

## Why another gallery?

Most gallery apps on the Play Store are heavy, ad-riddled, or cloud-locked. OpenGallery is built from a [research pass](docs/research/) over what users actually complain about in stock galleries (Samsung, Google Photos, MIUI, OnePlus) and what they beg for but never get:

- **Zero ads / zero telemetry / fully offline** — your photos never leave your phone.
- **Non-destructive editing** — edits save a copy by default and preserve all EXIF metadata.
- **Real nested folder browsing** — the single most requested gallery feature.
- **On-device People, OCR and object search** — no cloud, powered by on-device ML.
- **Persistent per-album sort** including true *Date taken*.
- **Tags written to file metadata (XMP)** — portable, not trapped in a private database.
- **A recycle bin, a truly hidden Locked Folder, duplicate finder, EXIF editor, batch rename.**

## Feature overview (v1.0 scope)

| Area | Highlights |
|---|---|
| Browse | Timeline (day/month/year pinch grouping), adjustable grid, fast scrubber, albums & album groups, nested folders, favourites, videos |
| View | Deep-zoom viewer, filmstrip, video & motion-photo playback, details with full EXIF + map |
| Edit | Crop/rotate/straighten/perspective, tone sliders + auto, GPU filters with intensity, draw/stickers/text, spot colour, lasso cutout, object eraser — always save-as-copy |
| Organize | Move/copy, hide albums, merge by name, pin, per-album sort, tags (XMP), batch rename, EXIF/date/GPS editor |
| Search | Filename/date/type/location + offline People (faces), OCR text-in-photo, object labels |
| Privacy | Encrypted Locked Folder (biometric/PIN), strip location on share, recycle bin |
| Extra | Stories (on-device, with a real off switch), Locations map (OpenStreetMap), duplicate finder, stats |

Deliberately **excluded**: cloud sync/shared accounts, generative AI edits, anything needing a server or subscription.

## Building

Requirements: JDK 17+ and the Android SDK (compileSdk 36).

```bash
git clone https://github.com/TonmoyBishwas/foss.opengallery.app.git
cd foss.opengallery.app
./gradlew assembleDebug
```

Release builds are signed from an untracked `keystore.properties` in the repo root:

```properties
storeFile=../your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then `./gradlew assembleRelease`.

## Design reference

The UI replicates the layout and interaction patterns of a modern OEM gallery (see [docs/design-reference/](docs/design-reference/)). All icons, artwork and fonts in this app are original or system-provided — no proprietary assets are used.

**Note on ML Kit:** the People/OCR/label search features use Google ML Kit, which runs fully on-device and is free, but is not open-source and requires Google Play services. Everything else in the app works without it, and these features degrade gracefully when Play services is absent.

## Contributing

Issues and PRs welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[GPL-3.0](LICENSE) — if you distribute a modified version, you must publish your source too.
