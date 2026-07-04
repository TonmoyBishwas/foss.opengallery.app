# Architecture

Single Gradle module (`:app`), Kotlin + Jetpack Compose, single activity, MVVM with
Kotlin Flow. No DI framework, no analytics, no network stack at all.

## Stack

| Concern | Choice |
|---|---|
| UI | Jetpack Compose + Navigation Compose, one activity (`MainActivity`) |
| DI | Hand-rolled `di/AppContainer.kt` — lazy graph owned by `OpenGalleryApp` |
| Media | MediaStore queries + Paging 3 (`MediaPagingSource`) |
| Local DB | Room (`data/db/OgDatabase.kt`) — metadata only, never the photos themselves |
| Images | Coil 3 (+ video frames, GIF); custom `MediaStoreThumbnailFetcher` |
| Video | Media3 ExoPlayer (`viewer/VideoPage.kt`) |
| ML (all on-device) | ML Kit text recognition / image labeling / face detection + bundled MobileFaceNet TFLite (`data/index/FaceEmbedder.kt`) |
| Background work | WorkManager (`data/index/IndexWorker.kt`) |
| Map | osmdroid (OpenStreetMap, no API key) |

minSdk 26 / targetSdk 36 / compileSdk 36. Release: R8 minify + resource shrink +
ABI splits. `androidResources { noCompress += "tflite" }` is load-bearing — the face
model is memory-mapped and crashes if compressed.

## Data flow

**MediaStore is the source of truth** for what photos/videos exist. Screens observe
it through repositories (Paging 3 for the timeline, Flows elsewhere). Room stores only
what MediaStore cannot: album preferences, custom albums, the search index, faces,
legacy trash/favourite records, Locked Folder entries. Nothing user-visible lives
*only* in Room except things we created ourselves (custom albums, people names).

```
MediaStore ──MediaQuery/MediaPagingSource──▶ Repositories ──Flow/PagingData──▶ ViewModels ──▶ Screens
     ▲                                            │
     └──── MediaActions (trash/favourite/write) ◀─┘        Room (OgDatabase) ◀── IndexWorker (ML)
```

ViewModels are created with `ogViewModel { c -> SomeViewModel(c.xRepository) }`
(`ui/OgViewModel.kt`), which hands them dependencies from the `AppContainer`.

## Package map (`app/src/main/java/foss/opengallery/app/`)

### `data/`
- `MediaQuery.kt` / `MediaRepository.kt` / `MediaPagingSource.kt` — MediaStore
  projections, paged timeline, bucket queries. API-level fork: Bundle query API on
  30+, sort-string fallback on 26–29.
- `MediaActions.kt` — write operations: `createTrashRequest` / `createDeleteRequest` /
  `createFavoriteRequest` (30+) with pre-30 fallbacks; all destructive ops return
  IntentSenders the UI must launch for user consent.
- `AlbumsRepository.kt` + `AlbumSort.kt` — buckets + Room metadata (hidden/pinned/
  per-album sort, encoded as an Int).
- `trash/TrashRepository.kt` — dual path: system trash on Android 11+
  (`usesSystemTrash`), app-managed 30-day trash dir + `TrashedItemEntity` rows on 26–29.
- `locked/LockedStore.kt` — Locked Folder. AES-GCM with a non-exportable Android
  Keystore key; blob format `[12-byte IV][ciphertext]` in `filesDir/locked`; rows in
  `LockedItemEntity`. Invisible to MediaStore and excluded from backups
  (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`).
- `index/` — `IndexWorker.kt` (WorkManager job: OCR + labels + geocoding + face
  detection into Room, incremental), `FaceEmbedder.kt` (MobileFaceNet TFLite →
  192-float embeddings, clustered into `PersonEntity` centroids).
- `settings/SettingsRepository.kt` — DataStore preferences.
- `viewer/ViewerSource.kt` — resolves what list the viewer pages through
  (bucket / virtual / custom album, given a sort).
- `db/OgDatabase.kt` — all Room entities + DAOs in one file: album meta, custom
  albums/groups, legacy trash, legacy favourites, locked items, `MediaIndexEntity`
  (+ FTS4 mirror for free-text search), faces, people.
- `model/MediaItem.kt` — the one media value type used everywhere.
- `thumbs/MediaStoreThumbnailFetcher.kt` — Coil fetcher using
  `ContentResolver.loadThumbnail` on 29+.

### `ui/`
- `AppRoot.kt` — NavHost + home scaffold wiring. `Nav.kt` — `Routes` object; albums
  and viewer routes are typed as `album/{type}/{id}` where type ∈ bucket | virtual |
  custom. `DrawerSheet.kt` — the ☰ bottom sheet.
- `theme/` — `OgColors` (dark, near-black One UI palette), `OgType`, `Theme.kt`.
- `components/` — the design system:
  - `OgIcons.kt` — **every icon in the app**, hand-drawn as
    `fun DrawScope.drawX(color, strokeWidth)` extension members on `object OgIcons`.
    Icon parameters are typed `DrawScope.(Color, Float) -> Unit` and rendered inside
    a `Canvas` composable. Add new icons here; never import image assets for icons.
  - `CollapsingHero.kt` (big title that collapses on scroll), `OneUiBottomTabs.kt`,
    `OneUiPopupMenu.kt` (DropdownMenu-based context menu), `FastScrubber.kt`,
    `SelectionBars.kt` (`SelectionAction(label, enabled, icon, onClick)`).
- `screens/` — one package per feature: `pictures/` (timeline + pinch grid),
  `albums/` (Albums tab, AllAlbums incl. folder tree, AlbumDetail, create/hide),
  `viewer/` (pager, `ZoomableImage`, `VideoPage`, `DetailsSheet`), `editor/`
  (`EditState` reducer + `EditorScreen` with Transform/Filters/Tone/Decorate
  sections), `search/` (FTS + People), `stories/`, `locations/`, `suggestions/`
  (duplicates, large videos), `recycle/`, `locked/`, `settings/`.
- `permissions/MediaAccess.kt` — the permission gate: `READ_EXTERNAL_STORAGE`
  (26–32), `READ_MEDIA_IMAGES/VIDEO` (33+), partial access
  (`READ_MEDIA_VISUAL_USER_SELECTED`, 34+).

### `util/`
- `SaveEdited.kt` — full-res render of an `EditState` + non-destructive save:
  new MediaStore row via the IS_PENDING two-phase pattern, **all EXIF copied from the
  source** (incl. GPS), orientation reset because rotation is baked into pixels.
- `MotionPhoto.kt` — Samsung/Google motion-photo container parsing.
- `StripShare.kt` — optional GPS-stripping share path.

## Editing pipeline

`EditorScreen` edits an `EditState` (crop rect in normalized 0..1 coords, rotate90,
flip, straighten degrees, tone values, filter, decorations). The live preview and
`SaveEdited.render` apply the *same* `ToneMatrix`, so preview == output. Order:
straighten → rotate/flip → crop → color matrix → decorations. Originals are never
touched.
