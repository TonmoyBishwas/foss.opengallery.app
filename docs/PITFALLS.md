# Pitfalls — bugs that already happened once

Every entry below is a real bug class from v1.0.0 → v1.0.2, or a known risk on the
road to the Play Store. Check new code against this list before shipping.

## Compose layout traps (the whole v1.0.1 bug batch)

### Bottom bars render at the TOP of the screen
A composable emitted as a *sibling* inside a parent `Box` defaults to `TopStart`.
The selection bottom bar was emitted next to the screen's `Column` inside
`HomeTabs`' Box — so Cancel/Share/Delete appeared under the status bar.

**Rule:** a bottom bar lives *inside* the screen's `Column`, after the content, and
the content gets `Modifier.weight(1f)`:

```kotlin
Column(Modifier.fillMaxSize()) {
    Header(...)
    Box(Modifier.fillMaxWidth().weight(1f)) { Grid(...) }   // content
    SelectionBottomBar(...)                                  // pinned bottom
}
```

### Popup menus open at the LEFT edge
`DropdownMenu` (our `OneUiPopupMenu`) anchors to its **nearest parent layout**, not
to the button that opened it. This bug shipped four times (Viewer, AlbumDetail,
RecycleBin, AllAlbums).

**Rule:** either wrap the ⋮ button and the menu in one shared `Box`, or — when the
menu is emitted after a header — anchor it at the right edge:

```kotlin
Box(Modifier.align(Alignment.End).padding(end = 8.dp)) { OneUiPopupMenu(...) }
```

Any **new screen with a ⋮ menu** must be checked for this.

### Content under the status bar
Full-screen scrolling content needs `statusBarsPadding()` (the Stories title was cut
off). Floating overlays (FastScrubber) need explicit top/bottom padding that clears
the status bar and navigation area — `top = 72.dp, bottom = 24.dp` is what shipped.

### Stale closures in drag gestures
Inside `detectHorizontalDragGestures` etc., never read a captured state value and
write back an absolute — the lambda holds a stale snapshot. Use delta-based updates
through the reducer: `onUpdate { it.copy(straighten = it.straighten + delta) }`
(this is how the straighten ruler works).

## Data & paging traps (the v1.1.0 perf batch)

### MediaStore queries MUST page in SQL
`Cursor.moveToPosition(offset)` on an unbounded query walks every skipped row
through 2 MB cursor windows — O(offset) per page, O(n²) over a scroll session.
This was the "lags like crazy on big albums" bug. `MediaQuery.queryPage` now
uses `QUERY_ARG_LIMIT/OFFSET` (30+) or a `LIMIT n OFFSET m` sort suffix
(26–29). Never query MediaStore without a limit unless you truly need every
row, and then use a narrow projection.

### Offset paging needs page-aligned keys and a total sort order
- `getRefreshKey` must return a multiple of `pageSize`, and `prevKey` must
  step by `pageSize` (not `params.loadSize`, which differs for the initial
  load) — otherwise a mid-scroll invalidation leaves a gap or an overlap, and
  overlapping ids crash `LazyVerticalGrid` with duplicate keys.
- Every sort order must end in `_ID` as a tiebreaker. LIMIT/OFFSET over a
  sort with ties (name, size, datetaken) is nondeterministic across pages.

### Debounce ContentObserver storms
MediaStore fires bursts of notifications (saves, downloads, its own thumbnail
writes). Invalidating pagers per-notification restarts loads mid-scroll.
`MediaRepository.changes` is debounced 700 ms — route all "refresh on media
change" logic through it instead of registering new observers.

### Anchors inside lazy items die when scrolled away
A `DropdownMenu` emitted inside a `LazyVerticalGrid` `item {}` stops existing
once that item scrolls out of the viewport — the ⋮ menu opened from the
pinned compact bar silently did nothing. Hoist popup menus out of lazy
content into the screen `Column` (see the anchoring rule above).

### Selection state must not depend on loaded pages
"Select all" iterated `LazyPagingItems` — i.e. only the ~240 loaded items.
Selection is now an id→uri map filled by a dedicated `_ID` projection query
(`MediaRepository.allUris`). Never derive bulk-action targets from what
paging happens to have loaded.

## Build & release traps

- **Java**: system Java is 1.8 → `Unsupported class file major version` style
  failures. Always `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"`.
- **TFLite**: `noCompress += "tflite"` in `app/build.gradle.kts` must never be
  removed — the face model is mmap'd and a compressed asset crashes at load.
- **R8**: test OCR, labels, face grouping, and video playback on a **release** build
  before tagging. ML Kit / TFLite keep rules are the most likely thing minification
  silently breaks; a debug-only test pass will not catch it.
- **versionCode** must increase every release (Play hard-requires it). Bump it
  together with `versionName` — forgetting the code bump while changing the name has
  nearly happened; consider a CI check.
- **Shell cwd resets between tool calls** (Claude Code specific): `gh release
  create` and file checks failed twice on relative paths. Use absolute paths.
- **Never stage** `opengallery-release.jks` / `keystore.properties` /
  `local.properties`. Losing the keystore after a Play launch (without Play App
  Signing) means losing the app — keep an off-machine backup.

## API-level forks — re-test both sides of each

| Feature | 30+ (Android 11+) | 26–29 fallback |
|---|---|---|
| Trash | `MediaStore.createTrashRequest` | app-managed dir + `TrashedItemEntity`, 30-day purge |
| Favourites | `IS_FAVORITE` / `createFavoriteRequest` | Room `FavoriteItemEntity` |
| Timeline query | Bundle query API | sort-string queries |
| Permissions | — | 33+: `READ_MEDIA_*`; 34+: partial access (`READ_MEDIA_VISUAL_USER_SELECTED`) |

The legacy (26–29) trash path and 34+ **partial access** are the least-tested code
paths in the app — the Play pre-launch report will exercise both. Test them before
launch.

## Device-testing gotchas (user's phone)

- Wireless debugging: pairing persists per-PC, but the **connect port changes every
  toggle**. Rediscover with `adb mdns services`. The pairing service
  (`_adb-tls-pairing._tcp`) only advertises while the pairing dialog is open on the
  phone.
- The connection drops when the phone sleeps or Wireless debugging times out —
  that's not an app bug.
- Never port-scan the user's network to find the phone.

## Suggestions / pre-release smoke checklist

Run before every release tag, on a release build:

1. Fresh install → permission flow: deny, then grant; on Android 14+, grant
   **partial** access and confirm the grid + re-selection UX.
2. Empty states: gallery with no media, empty album, empty recycle bin, empty search.
3. Delete → recycle bin → restore; favourite/unfavourite (both API paths if possible).
4. Edit a photo → save → verify a *copy* is created and EXIF (date, GPS) survived.
5. OCR/People/label search still return results (R8 check).
6. Rotate device / open from another app via ACTION_VIEW.
7. `git status` — no keystore/secrets staged; author is Tonmoy.

Longer-term ideas: a CI assert that versionCode changed whenever versionName did;
a Compose UI test for "menu opens under its anchor"; screenshot tests for the four
tab roots.
