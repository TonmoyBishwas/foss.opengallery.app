# Android Gallery Apps — Feature Coverage & Technical Implementation Notes

Research reference for an open-source Android gallery developer. Two parts: **PART A** documents notable/unique features of gallery apps app-by-app; **PART B** covers the code-side APIs/libraries for building a gallery, topic-by-topic. Source URLs are inline.

---

# PART A — Feature Coverage (App by App)

## A1. MIUI / Xiaomi Gallery (HyperOS)

**Browsing**
- Year / Month / Day timeline views, chronological stream, and an Albums tab ([mi.com](https://www.mi.com/global/support/faq/details/KA-541886/)).
- "On this day" resurfacing of past photos on their anniversary ([mi.com](https://www.mi.com/global/support/faq/details/KA-541886/)).

**Organization**
- AI auto-categorization (via Xiaomi Cloud): groups by people (faces), places, objects ([privacy.mi.com](https://privacy.mi.com/xiaomicloud/en_US/), [thecelldesk](https://thecelldesk.com/fixed-xiaomi-private-album-without-sync/)).
- Free-up-storage tool ([mi.com](https://www.mi.com/global/support/faq/details/KA-541886/)).

**Search**
- OCR text recognition inside photos; object/people/place recognition powers search (cloud-backed) ([mi.com](https://www.mi.com/global/support/faq/details/KA-541886/), [thecelldesk](https://thecelldesk.com/fixed-xiaomi-private-album-without-sync/)).

**Editing tools (strongest area — heavily AI in recent HyperOS)**
- **AI Eraser 2.0** — object/person removal with generative fill ([androidguias](https://en.androidguias.com/hyperos-updates-its-gallery-app-and-adds-new-functions-to-edit-photos-with-AI-with-incredible-results/)).
- **AI Expansion 2.0** — generative outpainting beyond original borders ([androidguias](https://en.androidguias.com/hyperos-updates-its-gallery-app-and-adds-new-functions-to-edit-photos-with-AI-with-incredible-results/)).
- **Reflection/glare removal**, **AI image-quality enhancement** (deblur/denoise) ([androidguias](https://en.androidguias.com/hyperos-updates-its-gallery-app-and-adds-new-functions-to-edit-photos-with-AI-with-incredible-results/)).
- **Sky Replacement** (Sunny/Dynamic/Cloudy/Milky Way, auto light/color match, optional audio) ([smartprix](https://www.smartprix.com/bytes/8-hidden-features-in-the-hyperos-gallery-app-you-didnt-know/)).
- **Cutout / Photo Cutout** — subject isolation, background replace, sticker creation ([smartprix](https://www.smartprix.com/bytes/8-hidden-features-in-the-hyperos-gallery-app-you-didnt-know/), [mi.com post](https://new-ams.c.mi.com/global/post/687561)).
- **Bokeh/background blur** (0–100, multiple effect types); **art filters** (Low Poly, Etching, Frosted Glass, Sketch) ([smartprix](https://www.smartprix.com/bytes/8-hidden-features-in-the-hyperos-gallery-app-you-didnt-know/)).
- **Pro Video Editor** (trim, crop, stitch, brightness/effects, audio); **Collage**; Clip / Art / **AI Film** montage modes ([smartprix](https://www.smartprix.com/bytes/8-hidden-features-in-the-hyperos-gallery-app-you-didnt-know/), [mi.com FAQ](https://www.mi.com/my/support/faq/details/KA-512718/), [mi.com](https://www.mi.com/global/support/faq/details/KA-541886/)).

**Privacy / Vault**
- **Private / Hidden Album** locked by PIN/password/fingerprint; hidden from grid, timeline, and file managers ([drfone](https://drfone.wondershare.com/android-xiaomi/how-to-hide-photos-and-videos-privately-on-mi-phone.html), [mi.com](https://www.mi.com/global/discover/article?id=3585)). Caveat: cloud backup may still upload private items unless disabled ([thecelldesk](https://thecelldesk.com/fixed-xiaomi-private-album-without-sync/)).

**Sharing**
- Secure/private sharing strips metadata + location; optional protective text watermark ([smartprix](https://www.smartprix.com/bytes/8-hidden-features-in-the-hyperos-gallery-app-you-didnt-know/), [mi.com](https://www.mi.com/global/support/faq/details/KA-541886/)).

**Gimmicks / Cloud**
- **Moments** AI story/highlight cards (photos + location); Xiaomi Cloud + optional Google Photos sync ([thecelldesk](https://thecelldesk.com/fixed-xiaomi-private-album-without-sync/), [privacy.mi.com](https://privacy.mi.com/xiaomicloud/en_US/)).

**⚠️ Ads / Bloat (call-outs)**
- **The Gallery app itself injects ads/recommendations.** It is explicitly named among MIUI/HyperOS system apps that show banners and content suggestions, driven system-wide by **MSA (MIUI/Mobile System Ads)** ([androidauthority](https://www.androidauthority.com/remove-ads-xiaomi-miui-1019139/), [makeuseof](https://www.makeuseof.com/how-to-remove-ads-on-your-xiaomi-device/)). There is a per-app "Recommendations" toggle users must disable manually ([ximitime](https://ximitime.com/how-to-remove-ads-on-xiaomi-hyperos-in-all-ways-12008/)).
- **Bottom line:** the AI editing suite is real and substantial; the injected banners/recommendation surfaces are bloat, not features.

## A2. OnePlus Gallery (OxygenOS)

**Browsing**
- Three tabs: **Photos** (camera roll), **Collections** (all other images + curated albums), **Explore** (memories + face/place grouping); minimalist, one-handed layout ([gadgethacks](https://android.gadgethacks.com/how-to/get-oneplus-minimalist-gallery-app-any-phone-0184892/), [androidheadlines](https://www.androidheadlines.com/2020/09/new-oneplus-gallery-oxygenos-11-download-app.html)).

**Organization / Search**
- Auto-grouping by date and location; favorites and screenshots auto-added to Collections; face and place grouping surfaced in Explore (place grouping is effectively the map/place view) ([gadgethacks](https://android.gadgethacks.com/how-to/get-oneplus-minimalist-gallery-app-any-phone-0184892/)).

**Editing tools**
- Lighting/contrast/color adjustments + **16 filters** ([download.it](https://oneplus-gallery.en.download.it/android)).
- **AI Eraser** (object/person removal), **AI Unblur**, **AI Reflection Eraser**, **AI Detail Boost** (upscale) — improved in the Feb 2026 OxygenOS update ([techwiser](https://techwiser.com/ai-features-in-oneplus-oxygenos-15/), [androidauthority](https://www.androidauthority.com/oneplus-oxygen-os-15-ai-features-3493183/), [notebookcheck](https://www.notebookcheck.net/OnePlus-releases-new-February-2026-OxygenOS-update-with-improved-AI-Eraser-new-video-editing-tools-updated-AI-Writer-and-more.1225885.0.html)).
- Built-in **video editor** (cut/split/stitch, music, text overlays, speed, crop), expanded Feb 2026 ([download.it](https://oneplus-gallery.en.download.it/android), [notebookcheck](https://www.notebookcheck.net/OnePlus-releases-new-February-2026-OxygenOS-update-with-improved-AI-Eraser-new-video-editing-tools-updated-AI-Writer-and-more.1225885.0.html)).

**Privacy / Vault**
- In-Gallery **"Hide"** → "Hidden Collection" but **NOT secure** (no password/biometric) ([gadgethacks](https://oneplus.gadgethacks.com/how-to/hide-photos-gallery-app-your-oneplus-phone-for-extra-privacy-0216129/)).
- The actually-secure option is **Lockbox** (PIN vault) in the Files/File Manager app, outside Gallery ([webtrickz](https://webtrickz.com/hide-photos-in-oneplus-using-lockbox/)).

**Gimmicks**
- **Memories / Story** highlights in Explore ([gadgethacks](https://android.gadgethacks.com/how-to/get-oneplus-minimalist-gallery-app-any-phone-0184892/)).

**⚠️ Ads / Bloat**
- The Gallery app itself is **not** an ad-injector; OxygenOS ads are OS-level (Lock Screen Glance, setup-time app-install prompts, App Picks, Theme Store) ([techpp](https://techpp.com/2025/02/23/oneplus-phone-settings-guide/), [community.oneplus.com](https://community.oneplus.com/thread/1989514635316822024)). Comparatively clean gallery.

## A3. F-Stop Gallery (power-user metadata tool)

Desktop-grade organization; reads/writes the same metadata standards as Lightroom/Picasa.

**Tagging / keywords**
- Tag-centric: one image can carry many tags and appear in many tag folders without duplication; "deep scanning" reads embedded tags/ratings/date so keywords from desktop tools appear automatically ([fstopapp.com/tutorial](https://www.fstopapp.com/tutorial/), [fstopapp.com](https://www.fstopapp.com/)).

**Nested / hierarchical**
- **Nested folders** and **nested albums** (paid); album view distinguishes Standard, Smart, and Nested albums ([fstopapp.com/tutorial](https://www.fstopapp.com/tutorial/), [Play](https://play.google.com/store/apps/details?id=com.fstop.photo&hl=en_US)).

**Smart albums (rule-based)**
- Auto-populating rule albums, e.g. "tags containing Family" or "rating > 4 stars"; multiple criteria combinable (date range AND tag combos) ([fstopapp.com](https://www.fstopapp.com/), [fstopapp.com/tutorial](https://www.fstopapp.com/tutorial/)).

**Ratings / favorites / filtering**
- Star ratings with a dedicated ratings view; separate Favorites system; navigation-drawer bookmarks for any folder/tag/rating/album ([fstopapp.com/tutorial](https://www.fstopapp.com/tutorial/), [fstopapp.com](https://www.fstopapp.com/)).

**Metadata (EXIF / IPTC / XMP) + XMP sidecars**
- Supports EXIF, IPTC, and XMP ([fstopapp.com/metadata](https://www.fstopapp.com/metadata/)).
- **Reading:** embedded metadata from JPGs on scan; for non-JPG formats, reads tags/ratings from **XMP sidecar files** (`.XMP` file with same basename in the same directory).
- **Writing (F-Stop Pro):** writes embedded metadata into JPGs and writes tags/ratings into XMP sidecars for all supported formats; "Update metadata" panel to save-to-disk / refresh-from-disk ([fstopapp.com/metadata](https://www.fstopapp.com/metadata/)).

## A4. 1Gallery (vault-focused, AES encryption)

Package `app.galleryx` — normal gallery + encrypted vault ([Play](https://play.google.com/store/apps/details?id=app.galleryx&hl=en)).

**Vault / encryption**
- Hidden photos/videos **AES-encrypted** (stronger than move/hide apps); can store IDs, licenses, cards ([XDA](https://xdaforums.com/t/app-1gallery-photo-gallery-vault-aes-encryption.4701829/), [makeuseof](https://www.makeuseof.com/android-gallery-vault-apps-hide-private-photos/)).
- Lock modes: **PIN, Pattern, Fingerprint, and Calculator (disguise) mode** ([makeuseof](https://www.makeuseof.com/android-gallery-vault-apps-hide-private-photos/)).
- Encrypted files live in a `.1Gallery` folder; Google Drive backup/restore supported ([blackmod listing](https://blackmod.net/threads/18870/)).
- **Caveat:** a "fake password / decoy vault" is *not confirmed* for 1Gallery specifically — the makeuseof roundup attributes decoy passcodes to other apps (Sgallery, PhotoGuard). 1Gallery's confirmed disguise is Calculator mode.

**Normal gallery features**
- Browse/organize/search, create folders, move/copy with SD support; photo editor (crop/rotate/resize/filters) + video editor (trim, subtitles); pinch to change column count; photo widget; views RAW, SVG, panoramas, GIF; Auto/Light/Dark themes ([makeuseof](https://www.makeuseof.com/android-gallery-vault-apps-hide-private-photos/), [blackmod listing](https://blackmod.net/threads/18870/)).

## A5. Aves Gallery (open-source, `deckerst/aves`)

Flutter-built "gallery and metadata explorer"; treats filters/albums/countries/tags as browsable **collections** ([GitHub](https://github.com/deckerst/aves)).

**Exotic / rare formats**
- HEIC/HEIF, AVIF, **DNG (RAW, with JPEG previews)**, multi-page TIFF, SVG, old AVI ([GitHub](https://github.com/deckerst/aves), [gigazine](https://gigazine.net/gsc_news/en/20260329-aves/)).
- Auto-detects/categorizes **motion photos, panoramas (photo spheres), 360° videos, GeoTIFF**, and screenshots ([GitHub](https://github.com/deckerst/aves), [makeuseof](https://www.makeuseof.com/use-aves-gallery-app-android/)).
- **Motion photos** from Apple/Samsung/Pixel play inline ([slashdot](https://slashdot.org/software/p/Aves-Gallery/)).

**Statistics view**
- Per-album or whole-library **Stats page**: photo/video counts, **file formats**, total storage; habit insights (e.g. % taken in the evening, most-used camera model); count of geotagged items **broken down by country**; breakdowns by file type, country, tag, rating, date ([makeuseof](https://www.makeuseof.com/use-aves-gallery-app-android/), [Play](https://play.google.com/store/apps/details?id=deckers.thibault.aves&hl=en_US)).

**Interactive map**
- Geotagged items on an interactive map with **per-country counts and clustering**; a **Countries** collection; custom raster-tile providers + geocoding ([makeuseof](https://www.makeuseof.com/use-aves-gallery-app-android/), [Aves Wiki: Custom maps](https://github.com/deckerst/aves/wiki/Custom-maps)).

**Tags / XMP / ratings**
- Add/bulk-edit tags; metadata editing (date, orientation, location, title/description, **rating**, **tags**) for JPEG, GIF, PNG, WEBP, TIFF, HEIC, MP4; hide items by tag/rating/path/day-of-week ([Aves Wiki: Metadata editing](https://github.com/deckerst/aves/wiki/Support:-Metadata-editing), [GitHub](https://github.com/deckerst/aves)).

**Metadata depth / integration**
- Swipe-up per-item inspection down to camera settings, color profile, JPEG quantization table; widgets, app shortcuts, screen saver, global search, picker, Android TV; open-source on Play/F-Droid/IzzyOnDroid/GitHub ([makeuseof](https://www.makeuseof.com/use-aves-gallery-app-android/), [GitHub](https://github.com/deckerst/aves)).

## A6. Fossify Gallery / Simple Gallery Pro (deep dive)

Simple Gallery Pro (`com.simplemobiletools.gallery.pro`) was discontinued in late 2023; the lead dev forked it into **Fossify Gallery** (`org.fossify.gallery`), the maintained successor with a near-identical feature set (claims below verified against the app's own `strings.xml`).

**Deep-zoom / formats**
- "Allow deep zooming images" + "Allow one finger zoom at fullscreen media"; high-res images without significant quality loss ([strings.xml](https://raw.githubusercontent.com/FossifyOrg/Gallery/master/app/src/main/res/values/strings.xml), [HowToGeek](https://www.howtogeek.com/every-android-user-should-check-out-this-free-open-source-app-collection/)).
- Formats: JPEG, JPEG XL, PNG, MP4, MKV, RAW, SVG, GIF, AVIF, panoramas ([README](https://github.com/FossifyOrg/Gallery)).

**Group-by / sorting**
- Group by **Folder, Date taken (daily/monthly), Last modified (daily/monthly), File type, Extension, or none**; full-subfolder grouping mode; sort folders and media by name/date-taken/date-modified/size/extension, asc/desc, random; manual folder reorder ([strings.xml](https://raw.githubusercontent.com/FossifyOrg/Gallery/master/app/src/main/res/values/strings.xml), [issue #119](https://github.com/FossifyOrg/Gallery/issues/119)).

**Customization**
- Material You dynamic themes + custom themes, customizable toolbar buttons/colors ([F-Droid](https://f-droid.org/en/packages/org.fossify.gallery/), [README](https://github.com/FossifyOrg/Gallery)).
- **Hide folder** adds a `.nomedia` (hides system-wide); **Exclude folders** hides in-app only, with "exclude a parent" option; **Included folders** surfaces folders the media scanner missed; **Pin folder** to top ([strings.xml](https://raw.githubusercontent.com/FossifyOrg/Gallery/master/app/src/main/res/values/strings.xml)).

**Protection / recycle bin / slideshow**
- **PIN, pattern, or fingerprint**; three granular checkpoints — opening the app, viewing hidden items, and manipulating (move/delete) files ([F-Droid](https://f-droid.org/en/packages/org.fossify.gallery/), [Android Police](https://www.androidpolice.com/hands-on-fossify-gallery/)).
- Built-in **recycle bin** (hidden by default via "Show recycle bin"); slideshow with interval/random/loop/animation ([strings.xml](https://raw.githubusercontent.com/FossifyOrg/Gallery/master/app/src/main/res/values/strings.xml), [gHacks](https://www.ghacks.net/2021/05/17/simple-gallery-pro-for-android-is-a-local-google-photos-alternative/)).

**Editor / integrations**
- Editor: crop, resize, rotate, flip, draw, filters, text/stickers, video trimming — all offline ([F-Droid](https://f-droid.org/en/packages/org.fossify.gallery/), [Android Police](https://www.androidpolice.com/hands-on-fossify-gallery/)).
- Responds to Edit / Set As / view intents; usable as system image picker; **strips GPS/EXIF metadata on share** ([strings.xml](https://raw.githubusercontent.com/FossifyOrg/Gallery/master/app/src/main/res/values/strings.xml), [MakeUseOf](https://www.makeuseof.com/replaced-default-gallery-app-with-fossify-phone-feels-cleaner/)).

## A7. Smaller & self-hosted apps — standout features

> Note: the "Sylph/Photofox" lead was a red herring — the correct app is **Focus / Focus Go by Francisco Franco**. "Photo-Fox" (lopcode) is an unrelated WIP self-hosted app; "Firefox Focus" is a browser.

- **Piktures (Diune):** unified sources (internal/SD/USB + Google Drive/Photos/OneDrive in one view); layouts (grid/mosaic/list/calendar); search by places/dates/tags; encrypted offline PIN **"Secure Space"** vault; editor, **GIF creator**, HD video player, EXIF-strip on share ([Play](https://play.google.com/store/apps/details?id=com.diune.pictures&hl=en_US), [piktures.app](https://www.piktures.app/)).
- **Focus Go (Francisco Franco):** lightweight, 100% offline, ad-free, minimal permissions; **on-device natural-language smart search** ("beach photos from July"), motion/live photo playback, PiP video, non-destructive editor ([Play](https://play.google.com/store/apps/details?id=com.franco.focus.lite&hl=en_US), [Android Authority](https://www.androidauthority.com/focus-go-free-photo-gallery-app-3434495/)).
- **Memories (Nextcloud, `pulsejet/memories`):** tested past 1M photos; EXIF date-taken timeline, "Rewind"/on-this-day, geocoded map w/ reverse geocoding, **people/object grouping delegated to Nextcloud Recognize + Face Recognition plugins**, external album sharing to non-Nextcloud users, archive, bulk metadata/date edit, HLS video transcoding, Google Takeout import ([GitHub](https://github.com/pulsejet/memories)).
- **LibrePhotos:** self-hosted; **face recognition** (`face_recognition` + HDBSCAN), object/scene detection via **Places365**, semantic search, auto-captioning via **im2txt**, timeline, reverse-geocoded MapLibre GL map, auto event-albums clustered by time+place ("Thursday in Berlin") ([GitHub](https://github.com/LibrePhotos/librephotos)).
- **PhotoPrism:** facial recognition + similarity grouping, label/caption generation via **pluggable TensorFlow/Ollama/OpenAI backends**, NSFW detection, six interactive world maps, Moments timeline, calendar view, **automatic stacking of related/duplicate images**, rich search filters, secret-link album sharing with expiry, PWA ([features](https://photoprism.app/features)).

## A8. Google Photos — non-AI / everyday features

> Google now frames Memories, face grouping, and cinematic effects as algorithm-driven ("lightly AI"). Purely deterministic: Locked Folder, Archive, Favorites, timeline scrubber, Map, Motion playback, Partner sharing, Shared albums, Collages, Free up space.

- **Locked Folder** — passcode/biometric space; items excluded from grid, Memories, search, albums, and other apps; historically local-only/not synced ([support 10694388](https://support.google.com/photos/answer/10694388), [XDA](https://www.xda-developers.com/google-photos-locked-folder-limitation/)).
- **Archive** — hides from main grid without deleting; still in albums/search/device folders; does not free storage ([support 7362432](https://support.google.com/photos/answer/7362432)).
- **Favorites** — star → auto-saved to Favorites album ([Android Authority](https://www.androidauthority.com/google-photos-favorites-867852/)).
- **People & pets (face grouping)** — groups similar faces, user-labelable, manual add/remove; region/account-limited ([support 6128838](https://support.google.com/photos/answer/6128838)).
- **Timeline scrubber** — edge scrollbar to jump by month/year ([GTricks](https://www.gtricks.com/google-photos/simple-trick-to-take-complete-control-over-scrolling-in-google-photos/)).
- **Map / Places** — interactive map of geotagged photos (Collections › Places); location from EXIF/manual/estimation ([support 6153599](https://support.google.com/photos/answer/6153599)).
- **Motion/Live photo playback** — plays the mini-clip via a Motion toggle; Top Shot on Pixel ([Android Police](https://www.androidpolice.com/android-motion-photo-disable/)).
- **Partner sharing** — continuous auto-share to one person, scoped by people (face groups) and/or start date ([support 7378858](https://support.google.com/photos/answer/7378858)).
- **Shared albums** — share via link to anyone (incl. non-users); collaborative ([support 6131416](https://support.google.com/photos/answer/6131416)).
- **Collages** (up to 6 on Android), **Cinematic photos** (3D-motion on a still), **Memories/Stories** carousel ([support 12637115](https://support.google.com/photos/answer/12637115), [support 6128862](https://support.google.com/photos/answer/6128862), [support 9454489](https://support.google.com/photos/answer/9454489)).
- **Free up space** — deletes local copies already backed up ([support 6128843](https://support.google.com/photos/answer/6128843)).

## A9. Samsung Gallery — non-AI / everyday features

> Menus vary by device and One UI version.

- **Secure Folder** — Knox-backed isolated encrypted container (More › Move to Secure Folder) ([Samsung ANS10001401](https://www.samsung.com/us/support/answer/ANS10001401/), [Android Police](https://www.androidpolice.com/samsung-secure-folder-samsung-gallery-app-find/)).
- **Hide Albums**; newer **Private Album** (One UI 8.5) protected by fingerprint/screen lock ([How-To Geek](https://www.howtogeek.com/hide-photos-and-videos-on-samsung-galaxy-phone/), [SamMobile](https://www.sammobile.com/news/samsung-one-ui-8-5-private-album-feature-explained/)).
- **Stories** — auto-curated slideshows (events/trips/faces) with auto music+filter, stored locally, in Collections tab ([Samsung Gulf](https://www.samsung.com/ae/support/mobile-devices/samsung-gallery-creating-stories/)).
- **Collage** — up to 6 items, auto layout/shuffle/per-photo edit; freeform collages ([TechWiser](https://techwiser.com/make-collage-samsung-galaxy-phones/), [Samsung India](https://www.samsung.com/in/support/mobile-devices/how-to-make-freeform-collages-in-galaxy-devices/)).
- **Motion Photo** — capture via viewfinder icon, configurable clip length, "View motion photo" playback ([Samsung ANS10004858](https://www.samsung.com/us/support/answer/ANS10004858/)).
- **Timeline scrubber / pinch-to-zoom** — pinch changes grouping (day↔month↔year); month/year headers; sort by created/modified/name ([MakeUseOf](https://www.makeuseof.com/samsung-gallery-app/), [Samsung India](https://www.samsung.com/in/support/mobile-devices/organise-pictures-and-videos-in-samsung-galaxy-devices/)).
- **Map / Locations** — geotagged photos on a map (hamburger › Locations) ([AndroidSIS](https://en.androidsis.com/how-to-view-photos-on-a-map/)).
- **People & Pets** grouping with manual naming; tag/album/details **search** with bulk actions ([XDA](https://xdaforums.com/t/manually-tag-people-in-photos-in-gallery.4432795/), [Samsung ANS10002535](https://www.samsung.com/us/support/answer/ANS10002535/)).
- **Shared / Family albums** — sync across a family Samsung account; **link sharing** for non-Samsung users ([Samsung Gulf](https://www.samsung.com/ae/support/mobile-devices/samsung-gallery-creating-a-shared-album/), [Android Police](https://www.androidpolice.com/how-to-create-shared-family-album-samsung-gallery/)).
- **Trash / Recycle Bin** — deleted items kept **30 days** ([Samsung CA](https://www.samsung.com/ca/support/mobile-devices/use-the-trash-feature-of-the-gallery-app-on-your-samsung-galaxy-device/)).
- **Album types** — standard, **auto-updating** (auto-collects a person's photos), group, shared; Favorites; **near-duplicate/similar-image grouping** with ungroup ([Samsung ANS10002535](https://www.samsung.com/us/support/answer/ANS10002535/)).

---

# PART B — Technical Implementation Notes

## B1. Media enumeration & fast loading of huge libraries

- **Core query.** `ContentResolver.query()` against `MediaStore` collection URIs — `MediaStore.Images.Media`, `MediaStore.Video.Media`, or `MediaStore.Files`. On API 29+ use `MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)` to read all volumes ([developer.android.com media](https://developer.android.com/training/data-storage/shared/media)).
- **Projection.** Request only needed columns: `_ID` (build item URI via `ContentUris.withAppendedId`), `DISPLAY_NAME`, `DATE_ADDED`, `DATE_MODIFIED`, `SIZE`, `MIME_TYPE`, `WIDTH`, `HEIGHT`, `BUCKET_ID`, `BUCKET_DISPLAY_NAME`, `RELATIVE_PATH` (API 29+, avoid deprecated `DATA`), `DURATION` (video). Cache `getColumnIndexOrThrow` outside the loop; query off the main thread; close with `.use{}`.
- **Pagination (API 30+).** Use `query(Uri, projection, Bundle, CancellationSignal)` with `ContentResolver.QUERY_ARG_LIMIT` / `QUERY_ARG_OFFSET`, plus `QUERY_ARG_SQL_SELECTION`/`_ARGS` and `QUERY_ARG_SORT_COLUMNS`+`QUERY_ARG_SORT_DIRECTION`. This replaces the pre-30 hack of appending `LIMIT`/`OFFSET` to the sort string (throws "Invalid token LIMIT" on Android 10/11) ([media page](https://developer.android.com/training/data-storage/shared/media), [ContentResolver paging](https://code.luasoftware.com/tutorials/android/android-contentresolver-query-with-paging/)).
- **Paging 3.** Implement `androidx.paging.PagingSource<Int, MediaItem>` whose `load()` maps `params.key`→offset, `params.loadSize`→limit; configure `PagingConfig(pageSize, initialLoadSize)`; call `invalidate()` on store changes ([Paging overview](https://developer.android.com/topic/libraries/architecture/paging/v3-overview), [Paging 3 + ContentProvider](https://davidwong.com.au/blog/2020/07/paging-library-3-and-content-provider/)).
- **CursorWindow.** ~2 MB shared-memory cap; wide projections/blobs can throw `Row too big to fit into CursorWindow`/`SQLiteBlobTooBigException`. Keep projections narrow (never `SELECT *`/blobs) and paginate.
- **Change detection.** `registerContentObserver(uri, true, observer)` → refresh/`invalidate()`; `MediaStore.getVersion(context, volume)` (full re-scan on change); `MediaStore.getGeneration(context, volume)` (monotonic, more reliable than DATE_ADDED/MODIFIED for incremental sync — persist last-seen generation) ([media page — Check for updates](https://developer.android.com/training/data-storage/shared/media)).

## B2. Thumbnail loading & caching

- **Platform API (API 29+).** `ContentResolver.loadThumbnail(uri, Size(w,h), signal): Bitmap` — replaces the **deprecated** `MediaStore.Images.Thumbnails` / `MediaStore.Video.Thumbnails` tables and `Thumbnails.getThumbnail(...)` (all deprecated in API 29) ([media page — Load file thumbnails](https://developer.android.com/training/data-storage/shared/media)).
- **Coil** (`io.coil-kt`) — Kotlin/coroutines-first, smaller APK, Compose support, OkHttp disk cache + LRU memory cache. `imageView.load(uri){ crossfade(true); placeholder(...); size(w,h) }` ([Coil](https://coil-kt.github.io/coil/getting_started/)).
- **Glide** (`com.github.bumptech.glide`) — mature, fast from cache. `Glide.with(v).load(uri).thumbnail(0.1f).transition(withCrossFade()).into(iv)`. Crossfade is **not** default in v4. Use `MediaStoreSignature(mime, dateModified, orientation)` as cache key so edits invalidate; `RecyclerViewPreloader` must use identical size/options to the bind request ([Glide caching](https://muyangmin.github.io/glide/doc/caching.html), [Glide RecyclerView](https://bumptech.github.io/glide/int/recyclerview.html)).
- Both handle background decode, memory+disk cache, **request de-duplication**, and cancellation on ViewHolder recycle. Trade-off: Glide often faster from cache/battle-tested; Coil smaller/cleaner Kotlin but can use more memory in heavy scroll ([comparison](https://medium.com/healthify-tech/coil-vs-glide-3f488f4de72a)).

## B3. Folder / album grouping

- **Bucket columns.** `MediaStore.MediaColumns.BUCKET_ID` (stable hash of parent dir) + `BUCKET_DISPLAY_NAME` (label). Group by BUCKET_ID ([media page](https://developer.android.com/training/data-storage/shared/media)).
- **GROUP BY emulation.** `query()` has no real GROUP BY. Portable approach: query all rows sorted by BUCKET_ID + DATE_ADDED DESC, collapse to distinct buckets in code (first item = cover, count members). The old `) GROUP BY (...` selection-string hack is fragile and unsupported on Android 10+ (validated selection tokens).
- **Paths.** Use `RELATIVE_PATH` (API 29+, e.g. `DCIM/Camera/`) not deprecated absolute `DATA`.
- **`.nomedia`.** A dir with an empty `.nomedia` file is excluded from the Media Scanner and won't appear in MediaStore at all — to show hidden folders you must scan the filesystem directly (needs broad storage access, see B5). Nudge rescans via `MediaScannerConnection.scanFile(...)`.

## B4. EXIF read/write + location

- **Library.** AndroidX `androidx.exifinterface:exifinterface` (`androidx.exifinterface.media.ExifInterface`) — supports far more formats/tags than framework `android.media.ExifInterface` ([reference](https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface)).
- **Read.** `getAttribute/Int/Double(tag)`, `getLatLong(): DoubleArray?`; constants `TAG_ORIENTATION`, `TAG_DATETIME_ORIGINAL`, `TAG_GPS_LATITUDE(_REF)`, `TAG_GPS_LONGITUDE(_REF)`, `TAG_MAKE`, `TAG_MODEL`, `TAG_F_NUMBER`, `TAG_EXPOSURE_TIME`, `TAG_ISO_SPEED`.
- **Write.** `setAttribute(tag, value)`, `setLatLong(lat, lng)`, then `saveAttributes()`. Needs a seekable writable source (file path or `"rw"` FileDescriptor, not a plain InputStream). Writable containers: **JPEG, PNG, WebP**; many others read-only.
- **Location (API 29+).** MediaStore **redacts** GPS EXIF unless you (1) hold `ACCESS_MEDIA_LOCATION` (declare + request at runtime) and (2) request the original via `MediaStore.setRequireOriginal(uri)` before `openInputStream`. For video, use `MediaMetadataRetriever.extractMetadata(METADATA_KEY_LOCATION)` (no extra permission) ([media page — location](https://developer.android.com/training/data-storage/shared/media)).

## B5. Photo Picker vs full storage access

- **Photo Picker (no permission).** `MediaStore.ACTION_PICK_IMAGES`, wrapped by AndroidX `ActivityResultContracts.PickVisualMedia()` / `PickMultipleVisualMedia(max)`; launch with `PickVisualMediaRequest(ImageAndVideo | ImageOnly | VideoOnly | SingleMimeType)`. `PickVisualMedia.isPhotoPickerAvailable()`, `MediaStore.getPickImagesMaxLimit()`. Native on API 30+/built-in on 12+, falls back to `ACTION_OPEN_DOCUMENT`. Persist grants with `takePersistableUriPermission` ([Photo picker](https://developer.android.com/training/data-storage/shared/photopicker)).
- **Play services backport** for API 19–29 via the `com.google.android.gms.metadata.ModuleDependencies` service + `photopicker_activity:0:required` meta-data ([same page](https://developer.android.com/training/data-storage/shared/photopicker)).
- **Runtime media permissions** (for broad, non-picker access): `READ_EXTERNAL_STORAGE` (≤API 32); granular `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO`/`READ_MEDIA_AUDIO` (API 33+); `READ_MEDIA_VISUAL_USER_SELECTED` partial "Selected photos" access (API 34+).
- **Scoped Storage** since API 29; `requestLegacyExternalStorage` ignored once targeting API 30+; `DATA` paths unreliable — use MediaStore URIs ([Storage updates in Android 11](https://developer.android.com/about/versions/11/privacy/storage)).
- **MANAGE_EXTERNAL_STORAGE ("All files access") — use sparingly.** Needed only for `.nomedia`-hidden folders / arbitrary non-media / bulk file management; routed via `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`. **Google Play restricts it** to apps whose core purpose requires it (file managers, backup, antivirus); a typical gallery does **not** qualify and risks removal — prefer Photo Picker / granular `READ_MEDIA_*` ([Play policy](https://support.google.com/googleplay/android-developer/answer/10467955)).

## B6. Face detection & people grouping (on-device)

- **⚠️ ML Kit does DETECTION, not RECOGNITION.** `com.google.mlkit:face-detection` detects presence/location/geometry of faces; it **cannot** tell who a person is or match the same person across photos. Returns per `Face`: bounding box, landmarks (`FaceLandmark`: eyes/ears/nose/cheeks/mouth), contours (faces ≥200×200px), smiling/eyes-open probabilities, and a tracking ID that persists **within a video stream only** (not identity across separate photos) ([ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection), [Android guide](https://developers.google.com/ml-kit/vision/face-detection/android)). Classes: `FaceDetection`, `FaceDetector`, `FaceDetectorOptions`, `Face`.
- **People grouping pipeline (3 stages beyond ML Kit):**
  1. **Detect & crop** faces (ML Kit).
  2. **Embed** each cropped face with a face-recognition CNN via **TensorFlow Lite/LiteRT** — ML Kit does NOT provide this. Options: **FaceNet** (128-dim embedding; [davidsandberg/facenet](https://github.com/davidsandberg/facenet)), **MobileFaceNet** (~4 MB, 192-dim, real-time; [tutorial](https://medium.com/@estebanuri/real-time-face-recognition-with-android-tensorflow-lite-14e9c6cc53a5)). Same-person embeddings cluster close by **cosine similarity / L2 distance**.
  3. **Cluster** embeddings into people: **DBSCAN / agglomerative** (unknown #people), k-means (needs k), or **Chinese Whispers** graph clustering ([njordsir/Clustering-faces-android](https://github.com/njordsir/Clustering-faces-android)).
- **Reference implementations:** [shubham0204/OnDevice-Face-Recognition-Android](https://github.com/shubham0204/OnDevice-Face-Recognition-Android), [FaceRecognition_With_FaceNet_Android](https://github.com/shubham0204/FaceRecognition_With_FaceNet_Android), [droidcon writeup](https://www.droidcon.com/2024/07/26/building-on-device-face-recognition-in-android/).
- **MediaPipe Tasks** (`com.google.mediapipe:tasks-vision`) offers `FaceDetector`/`FaceLandmarker` (detection) and an **Image Embedder** task for hosting embedding models — but still separates detection from any identity clustering you build.

## B7. On-device search / labeling / OCR

- **Image Labeling** — `com.google.mlkit:image-labeling`; default model recognizes **400+ entities** (objects/places/activities/species) each with a confidence score; supports custom TFLite models. Classes `ImageLabeling`, `ImageLabeler`, `ImageLabelerOptions`, `ImageLabel` (`getText/getConfidence/getIndex`). Auto-tags photos for keyword search ([ML Kit Image Labeling](https://developers.google.com/ml-kit/vision/image-labeling)).
- **Object Detection & Tracking** — `com.google.mlkit:object-detection`; bounding boxes + optional classification ([codelab](https://codelabs.developers.google.com/mlkit-android-odt)).
- **Text Recognition v2 (OCR)** — `com.google.mlkit:text-recognition` (+per-script deps: chinese/devanagari/japanese/korean); on-device, API 21+; returns `TextBlock→Line→Element→Symbol` with bounding boxes — enables searching text inside photos ([ML Kit Text Recognition v2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)).
- **Semantic / natural-language search (CLIP-style).** Fixed labels aren't enough for "sunset on the beach" — use image+text **embeddings**. **TensorFlow Lite Searcher** provides a CLIP-inspired dual encoder (image + text → 128-dim L2-normalized space) + **ScaNN** approximate-nearest-neighbor index (~6ms/search over millions) ([TF blog: on-device text-to-image search](https://blog.tensorflow.org/2022/05/on-device-text-to-image-search-with.html)). **MediaPipe Image Embedder** ([guide](https://ai.google.dev/edge/mediapipe/solutions/vision/image_embedder/android)); **MobileCLIP** TFLite for mobile.
- **Recommended index:** run Image Labeling + Text Recognition at ingest → keyword/full-text index (SQLite FTS); optionally store CLIP embeddings in a vector index (ScaNN) for semantic queries.

## B8. Video playback & frame extraction

- **Playback — AndroidX Media3 ExoPlayer** (`androidx.media3:media3-exoplayer` + `-ui` + `-common`), the successor to the **deprecated** `com.google.android.exoplayer2` ([Media3 releases](https://developer.android.com/jetpack/androidx/releases/media3)).
- **Supported codecs/formats:** H.263, H.264/AVC, H.265/HEVC, VP8, VP9, AV1 (via device `MediaCodec`); ExoPlayer bundles software decoder extensions for AV1 (libgav1), VP9, FLAC, Opus, FFmpeg, etc. Containers MP4/MOV/WebM/MKV/MPEG-TS/fMP4 + DASH/HLS/SmoothStreaming ([supported formats](https://developer.android.com/media/media3/exoplayer/supported-formats)).
- **Frame extraction — `MediaMetadataRetriever`:** `getFrameAtTime(timeUs, option)` (options `OPTION_CLOSEST`/`OPTION_CLOSEST_SYNC`); **`getScaledFrameAtTime(timeUs, option, w, h)`** — preferred for thumbnails (API 27/28+, memory-efficient); `getFramesAtIndex()`. `setDataSource` accepts path, `FileDescriptor`+offset/length (relevant for motion photos, B9), or content URI ([reference](https://developer.android.com/reference/android/media/MediaMetadataRetriever), [thumbnails guide](https://developer.android.com/social-and-messaging/guides/media-thumbnails)). Higher-level alt: `ThumbnailUtils.createVideoThumbnail(File, Size, signal)` / `ContentResolver.loadThumbnail()`.

## B9. Motion / Live photo parsing

A motion photo is a still JPEG/HEIC with a short **MP4 appended to the same file**; parsing = find the byte offset where the video begins and slice it (no re-encode).

- **Google Motion Photo Format 1.0** (Pixel `PXL_*.MP.jpg`) — [spec](https://developer.android.com/media/platform/motion-photo-format). XMP namespaces:
  - **Camera** (`ns.google.com/photos/1.0/camera/`): `Camera:MotionPhoto=1`, `Camera:MotionPhotoVersion`, `Camera:MotionPhotoPresentationTimestampUs`.
  - **Container** (`.../container/`): a `Container:Directory` array of `Item`s with `Item:Mime` (`image/jpeg`, `video/mp4`), `Item:Semantic` (`Primary`/`MotionPhoto`), `Item:Length`, `Item:Padding` (must be **8** for HEIC/AVIF = `mpvd` box header).
  - **Extract (JPEG):** verify MotionPhoto=1 → parse directory → video offset = primary JPEG length + Padding, length = video item's `Item:Length`; equivalently seek back `Item:Length` from EOF (video is last). Feed offset+length to ExoPlayer/`MediaMetadataRetriever` via `setDataSource(FileDescriptor, offset, length)`. For HEIC/AVIF: primary ends with ISOBMFF `mpvd` box, video offset = image length + 8.
- **Older Google "MicroVideo"** (`MVIMG_*.jpg`): `Xmp.GCamera.MicroVideo=1`, `MicroVideoVersion`, and **`MicroVideoOffset`** (bytes from **end** of file to the MP4 start). Extract: length = MicroVideoOffset, start = fileSize − offset ([Timo Jyrinki writeup](https://timojyrinki.gitlab.io/hugo/post/2021-03-30-pixel-motionphoto-microvideo-file-formats/), [Working with Motion Photos](https://medium.com/android-news/working-with-motion-photos-da0aa49b50c)).
- **Samsung Motion Photo** (trailer marker, no XMP offset): search bytes for the **`MotionPhoto_Data`** marker — everything before = JPEG, after = MP4; ExifTool exposes `EmbeddedVideoFile`/`EmbeddedVideoType=MotionPhoto_Data` ([joemck/ExtractMotionPhotos](https://github.com/joemck/ExtractMotionPhotos), [g0ddest/sm_motion_photo](https://github.com/g0ddest/sm_motion_photo)).
- **Recommendation:** detect vendor per file (check Google XMP first, then scan for Samsung marker) and hand the computed (offset, length) directly to Media3 via a FileDescriptor data source — avoid temp-file extraction.

## B10. GPU image filters

- **android-gpuimage** — `jp.co.cyberagent.android:gpuimage` ([wasabeef/android-gpuimage](https://github.com/wasabeef/android-gpuimage)). **OpenGL ES 2.0** with **GLSL fragment shaders** (ports directly from GPUImage iOS). Classes: `GPUImage`, `GPUImageView`, `GPUImageFilter` (holds GLSL source), `GPUImageFilterGroup` (chain), concrete filters (`GPUImageSepiaToneFilter`, `GPUImageGaussianBlurFilter`, …). Subclass `GPUImageFilter` with a custom fragment-shader string for your own effect.
- **⚠️ RenderScript is DEPRECATED as of Android 12 / API 31.** Official: "RenderScript APIs are deprecated starting in Android 12… support is expected to be removed entirely in a future release"; AGP 7.2+ deprecates the RenderScript APIs. **Do not build new filter code on RenderScript** ([migration guide](https://developer.android.com/guide/topics/renderscript/migrate)).
- **RenderEffect** — `android.graphics.RenderEffect`, **API 31+**, applied to Views via `View.setRenderEffect(...)`. Factories: `createBlurEffect`, `createColorFilterEffect`, `createShaderEffect`, `createRuntimeShaderEffect`, `createBitmapEffect`, `chainEffect`. Recommended replacement for the old RenderScript blur intrinsic ([reference](https://developer.android.com/reference/android/graphics/RenderEffect)).
- **AGSL (Android Graphics Shading Language)** — **API 33+**, via `android.graphics.RuntimeShader`. GLSL-like syntax running in Skia; `RuntimeShader(src)` → `setFloatUniform`/`setInputBuffer` → `Paint.setShader(...)` or `RenderEffect.createRuntimeShaderEffect(...)`. Uses Canvas (top-left origin) coords vs GLSL bottom-left ([AGSL](https://developer.android.com/develop/ui/views/graphics/agsl), [RuntimeShader](https://developer.android.com/reference/android/graphics/RuntimeShader)).
- **Strategy:** android-gpuimage for broad compatibility + rich prebuilt filters; RenderEffect (31+) for cheap live blur; AGSL (33+) for modern custom shaders. Never RenderScript.

## B11. RAW image decoding

- **DNG + system decoders (limited).** `BitmapFactory` / `ImageDecoder` (NDK ImageDecoder needs target API 30+) have **limited** DNG support — typically render the **embedded JPEG preview**, varies by device; proprietary formats (NEF/CR2/CR3/ARW/ORF/RW2…) generally **not** system-decodable ([NDK ImageDecoder](https://developer.android.com/ndk/guides/image-decoder)).
- **DngCreator (camera2)** — `android.hardware.camera2.DngCreator` **writes** DNG from `RAW_SENSOR` buffers (capture-side, not decode) ([reference](https://developer.android.com/reference/android/hardware/camera2/DngCreator)).
- **LibRaw for broad decode** — native C++ via JNI reads virtually all RAW (CR2/CR3, NEF, RAF, DNG, ARW, ORF, RW2, PEF, SRW…). Android binding [dburckh/AndroidLibRaw](https://github.com/dburckh/AndroidLibRaw) (API 24+); [LibRaw/LibRaw](https://github.com/LibRaw/LibRaw).
- **Two-tier gallery approach:** fast path extracts the **embedded JPEG preview** (LibRaw `unpack_thumb`/RawPreviewExtractor) for grid/scroll; slow path does full Bayer decode (`dcraw_process`) → RGB bitmap on open. Caveat: some files (Cinema-DNG, some phones/action cams) lack an embedded preview.

## B12. Trash / recycle bin

- **API 30+ batch requests** — `android.provider.MediaStore` static methods returning a **`PendingIntent`** for user consent ([media page](https://developer.android.com/training/data-storage/shared/media), [MediaStore ref](https://developer.android.com/reference/android/provider/MediaStore)):
  - **`createTrashRequest(resolver, uris, shouldTrash)`** — move to system trash / restore; sets `IS_TRASHED=1`, retained until `DATE_EXPIRES` (~30 days) then auto-deleted. **Recommended recycle bin.**
  - `createDeleteRequest(resolver, uris)` — permanent delete.
  - `createWriteRequest(resolver, uris)` — grant batch write, then update `IS_FAVORITE`/`IS_TRASHED` or delete without further prompts.
  - `createFavoriteRequest(resolver, uris, isFavorite)`.
  - Flow: call → `startIntentSenderForResult(pi.intentSender, code, ...)` → check `RESULT_OK`.
- **MANAGE_MEDIA** (Android 12+, via `Settings.ACTION_REQUEST_MANAGE_MEDIA`) lets subsequent trash/delete batches skip the per-op dialog; direct `IS_TRASHED` writes without a dialog are OEM-preinstalled-gallery only.
- **App-managed trash (<API 30):** move files to an app-owned dir, record original path/metadata in Room, hide from gallery, restore/delete on action. On API 29 you may hit `RecoverableSecurityException` for files you don't own (see B13).

## B13. Non-destructive editing & saving

- **IS_PENDING two-phase write.** Insert `ContentValues` with `MediaStore.MediaColumns.IS_PENDING=1` (hides + exclusive write) → get URI from `resolver.insert(...)` → write via `openFileDescriptor(uri, "w")` → flip `IS_PENDING=0` via `update(...)` to publish; prevents other apps seeing a half-written file ([media page](https://developer.android.com/training/data-storage/shared/media)).
- **Editing others' media.** API 29 throws `android.app.RecoverableSecurityException` — catch it, launch `exception.userAction.actionIntent.intentSender`. API 30+: prefer `MediaStore.createWriteRequest()` up front.
- **Preserve EXIF on re-save.** Decode→filter→re-encode loses EXIF — copy it with `ExifInterface`: read original tags, write onto the new file, `saveAttributes()`. For unredacted location, use `ACCESS_MEDIA_LOCATION` + `MediaStore.setRequireOriginal(uri)`. Don't reuse an InputStream after building an `ExifInterface` from it.
- **Save-as-copy vs overwrite.** Non-destructive default: insert a **new** MediaStore row (copy) — no write grant needed on the original. Overwrite needs a write grant and destroys the source — reserve for an explicit "Overwrite" action. Key columns in `MediaStore.MediaColumns`: `DISPLAY_NAME`, `MIME_TYPE`, `RELATIVE_PATH`, `IS_PENDING`, `IS_TRASHED`, `IS_FAVORITE`, `DATE_EXPIRES`.

## B14. Duplicate detection

- **Exact dups.** Compare file **size** then a content hash (**MD5**/SHA-256); identical hash = byte-identical. Misses re-encoded/resized copies → use perceptual hashing.
- **Perceptual hashing (near-dups)** ([imagededup](https://idealo.github.io/imagededup/methods/hashing/), [pHash howto](https://www.phash.org/docs/howto.html)):
  - **aHash (average):** shrink to 8×8 grayscale, bit = pixel ≥ mean. Fastest, least robust.
  - **dHash (difference):** 9×8 grayscale, bit = compare each pixel to right neighbor. Resilient to scale/brightness/contrast/aspect; good balance.
  - **pHash (perceptual):** **DCT**, keep low-frequency coefficients, threshold vs median. Most robust for photos, slower.
- **Comparison:** **Hamming distance** (differing bits); for a 64-bit hash in a `long`, `Long.bitCount(a XOR b)`; pick a threshold (e.g. dHash <10 ≈ likely dup) ([phash in Java](https://ssojet.com/hashing/phash-in-java)).
- **Libraries:** [KilianB/JImageHash](https://github.com/KilianB/JImageHash) (pure-Java aHash/dHash/pHash — best drop-in), [apaz-cli/Image-Hashing-Tools](https://github.com/apaz-cli/Image-Hashing-Tools), [pHash.org](https://www.phash.org/) (C/C++ via JNI).
- **Strategy:** (1) group exact dups by size+MD5; (2) compute dHash/pHash on the decoded **thumbnail** (cheap), store the 64-bit hash in a local DB, cluster by Hamming threshold. Note: "Thumbhash" is a placeholder-image encoding, **unrelated** to duplicate detection.
