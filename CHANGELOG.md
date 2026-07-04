# Changelog

## [1.0.0] — 2026-07-04

First release. A free, open-source, local-first gallery in a familiar
One UI-style design — no ads, no telemetry, no cloud, no API keys.

### Browse
- Pictures timeline grouped by day/month/year with pinch-to-zoom grid
  density (2–6 columns), grabbable fast scrubber with date bubble
- Albums tab with Essential albums, View all, album groups, custom albums
- Nested folder-tree browsing of your real directory structure
- Persistent per-album sort, including true "Date taken"
- Hide albums, multi-select everywhere with share/delete

### View
- Fullscreen viewer: pinch/double-tap zoom, filmstrip, favourites,
  details sheet with camera EXIF, video playback (Media3),
  copy to clipboard / set as wallpaper / print
- Registered as a system image/video viewer (ACTION_VIEW)

### Edit (always non-destructive)
- Transform: crop presets, rotate, flip, straighten dial
- 10 filters with intensity, full tone slider set (brightness, exposure,
  contrast, saturation, warmth, tint, highlights, shadows, vignette)
- Draw, stickers, text overlays
- Saves a copy with EXIF (incl. GPS) preserved — originals are never
  overwritten

### Organize & find
- Search: filenames, folders, text inside photos (OCR), objects (labels),
  and People — all indexed on-device, offline
- People face grouping via bundled MobileFaceNet embeddings
- Locations: OpenStreetMap view + country/city groups (on-device geocoding)
- Stories: automatic day/trip highlights with a real off switch
- Suggestions: exact-duplicate finder, large-video cleanup

### Privacy
- Encrypted Locked Folder (Keystore AES-GCM, biometric/PIN gate,
  excluded from backups, invisible to other apps)
- Recycle bin (system trash on Android 11+, app-managed 30-day bin on 8–10)
- Optional GPS stripping when sharing

### Known limitations (planned for next releases)
- Light theme (the app currently ships the reference dark design)
- Video trimming, motion-photo inline playback in the viewer
- Tags written to XMP, EXIF editor, batch rename
- Interactive crop handles (aspect presets available today)
