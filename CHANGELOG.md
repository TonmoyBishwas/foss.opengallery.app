# Changelog

## [1.1.0] — 2026-07-07

Large performance and bug-fix release: deep-scroll lag is gone, and a broad
audit fixed crashes, data-loss paths and dead UI across the app.

### Performance
- MediaStore page queries now use real SQL LIMIT/OFFSET (Bundle query API on
  Android 11+, sort-clause fallback on 8–10). Scrolling deep into a large
  album/timeline was O(n²) cursor walking before — this was the "lags like
  crazy" bug on big libraries
- MediaStore change notifications are debounced (700 ms); bursts of provider
  notifications no longer restart page loads and album scans mid-scroll
- Albums tab refresh dropped from ~18 MediaStore queries per change to 7,
  and bucket scanning no longer materializes a full row object per photo
- Fast scrubber no longer recomposes on every scrolled row (snapshotFlow)
- Timeline grid supplies contentType so headers/thumbs recycle correctly
- Viewer pre-composes neighbouring pages — no more loading flash per swipe
- Tab switches keep each tab's scroll position instead of resetting

### Fixed
- Paging refresh math could leave gaps or duplicate items after a
  mid-scroll library change (duplicate-key crash); page keys are now aligned
  and all sort orders end in a stable _ID tiebreaker
- Custom albums showed the entire library instead of their members (both in
  the album grid and the viewer opened from one)
- "Select all" only selected the ~240 already-loaded items; selection now
  covers the whole album/library, and share/delete act on all of it
- Pictures/Albums ⋮ menu was dead after scrolling (anchor lived inside the
  scrolled-away hero header)
- Deleting on Android 10 items not owned by the app crashed; the system
  consent prompt is now shown and the delete retried
- Favourite heart in the viewer updates immediately after toggling (and is
  hidden on Android 8–10 where system favourites don't exist)
- Deleting while zoomed no longer permanently locks viewer swiping
- Videos: background the app and playback now pauses; "Set as wallpaper"
  and "Print" are no longer offered for videos
- Android 8–10 deletes now really go through the app recycle bin
  (the 30-day bin existed but was never wired to the delete buttons), with
  the Android 10 per-item consent flow handled and retried correctly
- Recycle bin restore can no longer destroy the only copy of a photo when
  the write fails, and restoring a photo from Download/ no longer crashes
  on Android 10
- "Remove location data when sharing" is now fail-safe: if EXIF stripping
  isn't possible the app re-encodes the image without metadata, and never
  falls back to silently sharing the original with GPS intact
- Huge selections no longer crash share/delete (binder transaction limit):
  shares are capped and system-trash requests are batched
- Editor saves scale their working resolution to the device's memory
  instead of a fixed 4096 px cap (big-heap devices keep more detail;
  small-heap devices no longer risk out-of-memory crashes)
- Editor: crop presets now produce true pixel aspect ratios, straighten no
  longer bakes black wedges into the saved copy, drawn decorations land
  exactly where previewed on letterboxed photos, and one slider gesture is
  one undo step
- People/city grids with 1000+ photos keep a global newest-first order
- Locked Folder restore uses the correct MediaStore API on Android 8–9

## [1.0.2] — 2026-07-04

### Fixed
- Stories: "Explore your stories" no longer renders under the status bar
- Album, Recycle bin and All albums overflow menus now open anchored at
  the top right, under the ⋮ button, instead of the left edge

## [1.0.1] — 2026-07-04

UI polish release based on first-device feedback.

### Fixed
- Selection mode: Cancel / Share / Delete no longer render under the status
  bar — they are now a proper bottom action bar with icons
- Fast scrubber no longer travels into the status-bar area
- Viewer overflow menu now opens anchored under the ⋮ button (top right)
  instead of the left edge

### Changed
- Menu button in the bottom tabs is now a proper three-line glyph
- The ☰ drawer sheet entries now have icons (Videos, Favourites, Recent,
  Suggestions, Locations, Recycle bin, Settings, Locked folder)
- Viewer bottom actions are now icons (favourite, edit, info, share, delete)
  matching the reference design
- Editor redesigned to match the reference: circular undo/redo buttons,
  icon section tabs (transform, filters, tone, decorate), a tick-mark
  straighten ruler dial, and a flip / rotate / aspect pill

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
