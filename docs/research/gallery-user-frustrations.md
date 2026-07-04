# Android Gallery/Photos Apps: User Frustrations & Unmet Feature Requests

Research for an open-source gallery developer. Every point is a real complaint or repeatedly-requested-but-missing feature drawn from Reddit, Google Photos Help forums, Samsung Community, xiaomi.eu, OnePlus Community, XDA, GitHub issues, and tech press covering user sentiment. Each bullet names the app and links a source. Nothing here is invented.

Apps covered: Google Photos, Pixel default, Samsung Gallery (One UI), MIUI/Xiaomi (HyperOS) Gallery, OnePlus Gallery. Open-source galleries (Aves, Fossify/Simple Gallery, Immich) are cited for the missing-feature signal they carry.

> Sourcing caveat: reddit.com and several JS-rendered forum pages (XDA, community.oneplus.com, c.mi.com, some Samsung Community pages) are not directly fetchable by the research tooling. Where a raw Reddit thread couldn't be retrieved, the equivalent complaint is cited from an official help/community forum thread or tech-press coverage that reproduces it. All URLs are real thread/article locations.

---

## 1. Ads & promotional content injected into gallery apps

- Xiaomi system apps inject ads via a background service, **MSA ("MIUI System Ads")**, plus per-app "Show recommended content" toggles that must each be disabled separately — a whack-a-mole users resent. (MIUI/Xiaomi Gallery) https://www.androidauthority.com/remove-ads-xiaomi-miui-1019139/ , https://www.gizmochina.com/2026/02/17/how-to-turn-off-ads-in-xiaomis-hyperos-system-apps/
- Ads/recommendations surface across File Manager, Downloads, Security, Themes, ShareMe, App Vault, lock screen, etc.; even after disabling MSA you must hunt down each app's own recommendation toggle. (MIUI/Xiaomi) https://www.gizmochina.com/2025/08/26/how-to-remove-ads-in-xiaomi-phones-miui-and-hyperos/
- Dedicated user guides exist titled "How to properly set up Xiaomi Gallery and remove all ads" — users specifically want the Gallery de-cluttered of promoted content. (MIUI/Xiaomi Gallery) https://www.gamingdeputy.com/how-to-properly-set-up-xiaomi-gallery-and-remove-all-ads/
- OnePlus users report ads/promoted content creeping into OxygenOS system apps and global search, against the OS's "clean" reputation. (OnePlus) https://community.oneplus.com/thread/1228173 , https://community.oneplus.com/thread/1732743241607938051
- Unsolicited ads/promotional images appearing inside Samsung Gallery; the sentiment of "ads invading my Gallery" recurs (often traced to third-party overlay apps, but experienced as gallery ads). (Samsung Gallery) https://forums.androidcentral.com/threads/ads-appearing-in-gallery-on-galaxy-s5-please-help-how-do-i-stop-them.582165/

## 2. Forced cloud / cloud-centric design & features locked behind upload

- Core photo search, Memories, face grouping and most AI organization only work on photos **backed up to the cloud** — there is no comparable local-only search, pushing users to upload everything. (Google Photos / Pixel) https://www.androidauthority.com/google-gallery-app-existence-surprise-3555934/
- **Magic Editor and enhanced AI edits are gated**: all users get only 10 Magic Editor saves/month; unlimited requires a Pixel device or paid Google One Premium (2TB+). (Google Photos) https://blog.google/products-and-platforms/products/photos/google-photos-editing-features-availability/
- Extended editing tools (Portrait Blur/Light, Color Pop, etc.) are locked behind Google One on non-Pixel devices while free on Pixel. (Google Photos) https://support.google.com/photos/thread/238493142/
- Stop paying for Google One and you lose premium editing and risk losing over-quota photos — memories become hostage to a subscription. (Google Photos / Google One) https://www.pocket-lint.com/if-you-stop-paying-for-google-one/
- Samsung is **killing OneDrive Gallery sync (ends Sept 30, 2026)**, pushing users toward paid Samsung Cloud (free tier only 15 GB); users call it egregious top-down control. (Samsung Gallery) https://us.community.samsung.com/t5/Samsung-Apps-and-Services/DISCONTINUED-Samsung-Gallery-OneDrive-sync-feature-is-ending/td-p/3567282/page/2 , https://www.androidcentral.com/samsung-kill-cloud-gallery-sync-and-drive-support
- Repeated, hard-to-silence **"storage space running out" Xiaomi Cloud notifications** pressure users to buy more cloud storage; the only fix is killing the notification channel. (MIUI/Xiaomi Gallery) https://itigic.com/remove-notice-when-there-is-no-space-left-in-xiaomi-cloud/
- Xiaomi discontinued Gallery cloud sync and funnels users into a Google Photos / Google One migration that upsells paid storage when free space runs out — felt as coerced. (MIUI/Xiaomi Gallery) https://xiaomiui.net/xiaomi-cloud-will-stop-syncing-photos-on-your-gallery-dont-forget-to-back-up-36792/
- Xiaomi's **private/hidden album is tied to a Mi Cloud account**; users linked only to Google find it doesn't work, and some lost 100+ hidden photos after a Gallery update. (MIUI/Xiaomi Gallery) https://xiaomi.eu/community/threads/really-important-hidden-album-gone-vanished-on-xiaomi.69200/

## 3. Features REMOVED over time

- **Google Photos removed unlimited free "High quality" storage** (effective June 1, 2021); all uploads now count against the shared 15 GB quota — a major reversal of the headline feature. (Google Photos) https://blog.google/products/photos/storage-changes/
- Previously-free editing tools were **moved behind the Google One paywall (2020–2021)**, triggering backlash. (Google Photos) https://www.techtimes.com/articles/253953/20201106/google-photos-makes-editing-tools-exclusive-one-subscribers-triggers.htm
- **Google cut back print/physical products**: discontinued the auto monthly print subscription (2020) and an earlier print-products service (2018); users complained it picked random photos and was overpriced. (Google Photos) https://www.forbes.com/sites/paulmonckton/2020/06/20/google-photos-featue-cancellation-auto-print-selection/
- **Samsung removed video filters in One UI 8.5**; the only workaround re-compresses the video. (Samsung Gallery) https://www.androidcentral.com/phones/samsung-galaxy/after-waiting-for-one-ui-8-5-users-say-samsung-axed-video-features
- **Rename removed from Samsung Gallery** after a One UI update; users asking for it back. (Samsung Gallery) https://r1.community.samsung.com/t5/galaxy-a/rename-on-gallery-is-gone/td-p/9699318
- **"Auto create stories" no longer available** on recent One UI builds. (Samsung Gallery) https://eu.community.samsung.com/t5/galaxy-a-series/auto-create-stories/td-p/5495413
- **Face Group Search removed** from Gallery on recent One UI 8.5 devices. (Samsung Gallery) https://www.sammyfans.com/2025/09/27/samsungs-hidden-one-ui-8-gallery-feature-to-become-official-in-one-ui-8-5/
- Gallery date-search behavior degraded since the One UI 7.0 update. (Samsung Gallery) https://us.community.samsung.com/t5/Galaxy-S23/Search-Gallery-by-Date-since-7-0-update/td-p/3216700
- Broad "why do updates keep removing useful features" thread calls out One UI 7 Gallery regressions. (Samsung Gallery) https://forums.androidcentral.com/threads/one-ui-7-and-just-updates-in-general-why-consistently-remove-useful-features.1078941/

## 4. Local-only workflow pain (folders, sort, no cloud-free organization)

- **No nested/recursive sub-album support** — every directory is a flat album; reorganizing folders on a PC doesn't nest them. Long-standing request. (Samsung Gallery) https://r1.community.samsung.com/t5/galaxy-note/nested-albums/td-p/567868 , https://xdaforums.com/t/solved-gallery-app-with-albums-and-subalbums-subfolders.2315707/
- **No way to hide/exclude specific folders**; users note the app is ~10 years old and still lacks per-folder exclusion. (Samsung Gallery) https://us.community.samsung.com/t5/Samsung-Apps-and-Services/Exclude-folders-from-the-Gallery-app/td-p/542654
- **OnePlus Gallery missing folders/photos** — DCIM/camera images don't appear though present in File Manager; the old "folders" view is gone, buried under Albums > More Albums > Other. (OnePlus Gallery) https://xdaforums.com/t/missing-folder-and-photos-in-oneplus-gallery-app.4064981/
- OnePlus users ask for Samsung-like organization (move photos between folders, create/label/customize folders) which the app lacks. (OnePlus Gallery) https://community.oneplus.com/threads/photo-gallery-organiser.795230/
- MIUI Gallery: folders visible in file managers don't show in Gallery; "ghost" folders that can't be deleted. (MIUI/Xiaomi Gallery) https://xiaomi.eu/community/threads/problems-with-miui-gallery.4853/
- Aves flattens deep folder trees — users managing 500+ topic-organized albums want to navigate through nested subfolders like F-Stop does. (Aves / general) https://github.com/deckerst/aves/issues/511
- Request for a true file-system Explorer view showing folders alongside photos instead of a flat album list. (Aves / general) https://github.com/deckerst/aves/issues/592
- Nested-folder view buggy in Fossify: folders with subfolders still appear at the top level when subfolder grouping is on. (Fossify Gallery) https://github.com/FossifyOrg/Gallery/issues/150
- Immich mobile users repeatedly ask for a folder-navigation view matching the web ("can't see the folders within the app"). (Immich) https://github.com/immich-app/immich/discussions/15318

## 5. Sorting & custom/manual order

- **Google Photos has no folder hierarchy and no persistent per-album custom sort** — manual drag-to-reorder is lost when the album is downloaded or added under another account. (Google Photos) https://support.google.com/photos/thread/205409/albums-will-not-stay-in-order?hl=en
- Google Photos frequently displays photos in the **wrong chronological order** (relies on EXIF OffsetTime); no sort-by-filename, limited manual control. (Google Photos) https://support.google.com/photos/thread/935761/how-do-we-sort-photos-by-date-taken?hl=en
- Samsung "Recent"/Pictures view **can't be sorted** — it's an algorithmic view with no Sort option, unlike albums. (Samsung Gallery) https://us.community.samsung.com/t5/Galaxy-S22/RECENT-folder-in-gallery-are-not-in-date-order/td-p/2562299
- Samsung: **no "Date taken" sort** on some devices (only Date created/modified), so scanned/transferred photos sort wrongly. (Samsung Gallery) https://us.community.samsung.com/t5/Legacy-S-Phones/Photo-albums-out-of-order/td-p/2293749
- **OnePlus Gallery sorted only by capture date for years**, burying downloaded/received images; "date added" wasn't added until v3.7.19. (OnePlus Gallery) https://www.xda-developers.com/oneplus-gallery-3-7-19-sort-photos-added/
- **No manual/custom sort within a folder** on OnePlus — you can reorder collections but not the items inside them. (OnePlus Gallery) https://community.oneplus.com/threads/gallery-app-should-have-sorting-feature.531534/
- OnePlus updates **randomly re-sorted/shuffled** photos and lost their order (e.g., v4.0.297), with duplicates appearing. (OnePlus Gallery) https://community.oneplus.com/thread/1514461

## 6. Local face grouping / people & content search without cloud

- "Make or break" request for on-device content-based classification (faces/objects); dev filed but deprioritized (P3). (Aves) https://github.com/deckerst/aves/issues/66
- Request for face recognition that runs locally without exposing ML data even in shared albums. (Immich) https://github.com/immich-app/immich/discussions/24290
- Comparison coverage confirms the standout gap: people leaving Google Photos miss on-device people/place/thing grouping; recommended offline apps (Aves, Fossify) don't offer Google-Photos-style local face grouping. (general) https://www.howtogeek.com/these-x-open-source-gallery-apps-are-completely-private-and-offline/
- Request to integrate an offline model so users can search photos by content ("photos from last month", "pictures of a friend") entirely offline. (Fossify Gallery) https://github.com/FossifyOrg/Gallery/issues/19

## 7. Tags / keywords / labels (and metadata portability)

- Tagging is the single most-upvoted Fossify Gallery feature request. (Fossify Gallery) https://github.com/FossifyOrg/Gallery/issues/26
- Long-standing request to create/edit/delete XMP tags for keyword organization. (Aves) https://github.com/deckerst/aves/issues/1
- Request for XMP sidecar read/write so tags/ratings can be added without modifying the original file. (Aves) https://github.com/deckerst/aves/issues/994
- Samsung: **request to write Gallery tags into the file's actual metadata** — tags live only in Samsung's private DB, lost on transfer. (Samsung Gallery) https://us.community.samsung.com/t5/Samsung-Apps-and-Services/Write-Gallery-Tags-on-Files-Metadata/m-p/3545734
- Samsung tag search is **flooded by AI auto-scan tags** with no way to exclude keywords, and there's no bulk tag removal (one photo at a time). (Samsung Gallery) https://us.community.samsung.com/t5/Samsung-Apps-and-Services/Gallery-tag-search-flooded-by-AI-scans/m-p/3557306 , https://r2.community.samsung.com/t5/Others/Gallery-Multiple-photos-remove-tag/td-p/15400254
- Google Photos **descriptions/captions are stored only in Google's database, not written to file metadata** — they vanish on download/share. (Google Photos) https://picasageeks.com/tag/captions-on-photos/

## 8. EXIF editing, bulk rename, duplicate detection

- Google Photos **cannot edit EXIF except date/time**; location is only an "estimated" value; no keyword/tag system. (Google Photos) https://support.google.com/photos/thread/110841/
- Request for EXIF-based **batch rename** (e.g., rename by created date to YYYY-MM-DD_HH-MM-SS) with templates and undo. (Aves) https://github.com/deckerst/aves/issues/183
- Feature request for a dedicated **duplicate-finder** across multiple storages to free space. (Aves) https://github.com/deckerst/aves/issues/1103
- Samsung **duplicate handling is weak** — face recognition creates duplicate/mismatched people with no clean reset. (Samsung Gallery) https://eu.community.samsung.com/t5/mobile-apps-services/gallery-face-recognition-duplicates-amp-mismatched-unable-to/td-p/1830876
- EXIF editing, bulk rename, and duplicate detection are commonly satisfied only via third-party apps — absent from all stock galleries. (general) https://play.google.com/store/apps/details?id=net.xnano.android.photoexifeditor

## 9. Metadata stripped / mangled on edit, move & share

- Editing a photo in Fossify/Simple Gallery **strips ALL metadata** (date taken, camera, GPS), unlike Google Photos which preserves it. (Fossify Gallery / Simple Gallery) https://github.com/FossifyOrg/Gallery/issues/29
- **Moving photos resets file create/modify dates to "now"** even with "keep old change date" enabled, so files look newly created in other apps. (Fossify Gallery) https://github.com/FossifyOrg/Gallery/issues/140
- EXIF **location metadata stripped when another app picks an image** via ACTION_PICK. (Fossify Gallery) https://github.com/FossifyOrg/Gallery/issues/218
- Samsung sharing **re-compresses / reduces quality**; users ask whether shared photos are downscaled. (Samsung Gallery) https://r2.community.samsung.com/t5/Galaxy-Store-Apps-more/Are-Photos-Compressed-or-Quality-Reduced-when-shared-in-Galaxy/td-p/2358888

## 10. Editing frustrations (destructive edits, quality loss)

- **Samsung Gallery overwrites the original** instead of saving a copy — after updates the original disappears unless users find the hidden "Save copy" option. (Samsung Gallery) https://us.community.samsung.com/t5/Galaxy-S21/Gallery-overwrites-my-image-when-saving/td-p/1838707 , https://eu.community.samsung.com/t5/other-galaxy-s-series/original-photos-disappear-after-saving-an-edited-version/td-p/2496875
- Confusion over where originals go after a Samsung Gallery edit; hard to find/restore. (Samsung Gallery) https://us.community.samsung.com/t5/Galaxy-S23/Where-are-originals-stored-after-an-image-is-edited-in-the/td-p/2899344
- **Samsung's built-in video editor loses quality / drops frames** — trimming re-encodes and visibly degrades video. (Samsung Gallery) https://us.community.samsung.com/t5/Galaxy-S24/Samsung-built-in-video-editor-losing-quality-frames-when/td-p/2873420
- HyperOS Gallery AI editing plug-ins (AI Expansion, AI Erase, AI Sky) are buggy — saving an expanded image can error out and edits don't reflect in the saved file; many gated to the Chinese ROM. (MIUI/Xiaomi Gallery) https://techpipino.com/hyperos-2-global-the-good-the-bad-and-the-ugly/

## 11. Auto-generated Stories / Memories

- Samsung Gallery **keeps auto-creating unwanted Stories** from old photos; users must repeatedly delete them and want a real off switch. (Samsung Gallery) https://us.community.samsung.com/t5/Galaxy-S23/The-gallery-app-keeps-creating-stories-from-my-pictures-without/td-p/2883271
- Stories **still generate even with "Customization Service" off** — the documented fix doesn't work because Stories run as a separate hidden component. (Samsung Gallery) https://eu.community.samsung.com/t5/galaxy-s24-series/disable-gallery-stories-if-not-who-has-access/td-p/9125712

## 12. RAW support

- Samsung **Expert RAW "DNGs" are effectively processed JPEGs** — no highlight/shadow recovery, JPEG-like artifacts; users dub it "ExpertJPG." (Samsung Gallery / Expert RAW) https://eu.community.samsung.com/t5/galaxy-s22-series/official-response-needed-expertraw-app-is-not-creating-real-raw/td-p/5120501
- Samsung **RAW/DNG files not recognized or vanish** from Gallery; new DNG 1.7 output breaks RawTherapee/Darktable. (Samsung Gallery) https://eu.community.samsung.com/t5/galaxy-s24-series/raw-format-not-recognised/td-p/9214621 , https://us.community.samsung.com/t5/Galaxy-S24/Raw-DNG-Files-Vanished/td-p/3379313
- OnePlus users note limited native RAW handling and ask where RAW files go / how to view them. (OnePlus Gallery) https://community.oneplus.com/threads/dng-raw-files.574425/
- Request to **stack RAW + paired JPEG** (same basename) as one item instead of showing twice, plus a global toggle to hide RAW clutter (marked wont-fix). (Aves) https://github.com/deckerst/aves/issues/1045 , https://github.com/deckerst/aves/issues/810
- DNG/HEIC metadata editing unsupported because underlying libraries don't handle those formats, despite phones producing them. (Aves) https://github.com/deckerst/aves/discussions/952

## 13. Hidden folders / privacy vaults that don't truly hide

- Google Photos **Locked Folder originally had no cloud backup** — a factory reset, forgotten lock, or lost device meant permanent loss. (Google Photos / Pixel) https://www.androidauthority.com/google-gallery-app-existence-surprise-3555934/
- Locked Folder is heavily restricted: **can't move photos into albums, can't share, originally no web access** — a one-way vault, not an organization tool. (Google Photos) https://support.google.com/photos/thread/792656?hl=en
- Samsung **"Hidden" albums aren't truly private** — hidden photos still surface elsewhere (e.g., the picker). (Samsung Gallery) https://us.community.samsung.com/t5/Galaxy-Note-Phones/Hidden-albums-in-my-Gallery-are-showing-as-an-option-in-picture/td-p/2799418
- Samsung requires the heavyweight **Secure Folder just to hide a few photos**; a proper "Private Album" only arrives in One UI 8.5. (Samsung Gallery) https://www.androidcentral.com/apps-software/samsung-phone-users-may-finally-get-a-proper-locked-folder-in-the-gallery-app
- Users question whether Fossify's "hidden folder" truly hides media from other apps or just within Fossify (.nomedia concern). (Fossify Gallery) https://discuss.grapheneos.org/d/13501-hidden-folder-fossify-gallery-app
- Request for separate exclude lists so hiding an album from the collection doesn't also hide it when browsing files. (Aves) https://github.com/deckerst/aves/issues/1216

## 14. Archive & delete-from-device vs cloud confusion

- Google Photos: with backup ON, deleting a photo removes it from **both device and cloud** — a frequent, damaging surprise; the safer "Delete from device" is buried in overflow. (Google Photos) https://9to5google.com/2025/12/19/google-photos-delete-content-guide/
- **Cannot archive an entire album in one action** — must archive photos individually. (Google Photos) https://support.google.com/photos/thread/325245735/
- Users want archived photos to stop appearing across other views / a proper private area — Archive doesn't cleanly hide content. (Google Photos) https://support.google.com/photos/thread/792656?hl=en

## 15. Performance (large libraries, thumbnails, re-scanning)

- MIUI Gallery **hoards hidden thumbnail copies of every photo/video ever taken — including deleted ones** — wasting gigabytes with no in-app way to clear it. (MIUI/Xiaomi Gallery) https://xiaomi.eu/community/threads/miui-gallery-storage-leak-mi-11-ultra-miui13.64737/
- Under **HyperOS 2.0.x the Gallery no longer auto-regenerates thumbnails**, leaving broken/grey thumbnails; users resort to uninstalling updates or wiping cache. (MIUI/Xiaomi Gallery) https://tehnoblog.org/xiaomi-app-how-to-re-install-stock-factory-version-how-to-fix-corrupted-gallery-app-no-root/
- Samsung: **slow/missing thumbnails after One UI updates** unless images are opened manually. (Samsung Gallery) https://eu.community.samsung.com/t5/other-galaxy-s-series/samsung-gallery-app-missing-thumbnails-slow-to-load-after-one-ui/td-p/12781036
- Samsung: **laggy scrolling on large libraries** — Gallery tries to load everything before scrolling instead of progressively. (Samsung Gallery) https://us.community.samsung.com/t5/Samsung-Apps-and-Services/Phone-gallery-is-laggy-when-scrolling/td-p/2484266
- **OnePlus thumbnails load slowly / re-load every time the app opens**, and the app freezes when sharing; OnePlus publicly committed to fixing the freezes. (OnePlus Gallery) https://www.androidpolice.com/oneplus-commits-to-fixing-annoying-gallery-app-freezes/
- Google Photos stutters/lags on large high-res libraries; reviewers note local-first alternatives scroll instantly by comparison. (Google Photos) https://www.androidpolice.com/found-google-photos-alternative-on-android/
- Fossify: freezing/jank when scrolling large libraries (relevant to fast-scroll/timeline scrubber quality). (Fossify Gallery) https://github.com/FossifyOrg/Gallery/issues/301

## 16. UI / UX regressions

- Google Photos **removed the bottom navigation bar (2026)** for a floating "pill" toolbar; an earlier 2020 redesign hid Search in a bottom tab and removed the hamburger menu — repeated forced relearning. (Google Photos) https://9to5google.com/2026/02/12/google-photos-floating-toolbar/ , https://www.androidpolice.com/2020/05/21/big-google-photos-redesign-hides-search-in-bottom-tab-and-removes-hamburger-menu/
- **One UI 7 Gallery fast-scroll scrollbar made nearly invisible** — too thin to grab. (Samsung Gallery) https://forums.androidcentral.com/threads/one-ui-7-and-just-updates-in-general-why-consistently-remove-useful-features.1078941/
- Broad "I hate the One UI 7 Gallery changes" — search bar relocated to the bottom feels unnatural; a user who hand-tagged 15,000 photos upset by the reorg. (Samsung Gallery) https://eu.community.samsung.com/t5/galaxy-a-series/i-hate-the-changes-by-the-ui-7-update-especially-to-the-gallery/td-p/12877591
- **Samsung grid layout resets/changes on its own**, with pinch-to-zoom the only control. (Samsung Gallery) https://eu.community.samsung.com/t5/galaxy-s23-series/gallery-layout-changed/td-p/8245379
- MIUI thumbnails described as "really ugly"/pixelated vs alternatives; navigation becomes unresponsive and the app force-closes. (MIUI/Xiaomi Gallery) https://xiaomi.eu/community/threads/problems-with-miui-gallery.4853/

## 17. Privacy / telemetry / desire to replace stock

- MIUI's data-collection reputation drives users to abandon the stock Gallery; guides explicitly recommend replacing it to reduce tracking. (MIUI/Xiaomi Gallery) https://geekflare.com/protection/improve-miui-privacy/
- Users seek a private, local-first gallery to escape Google Photos' cloud-tied model, migrating to Fossify/Simple Gallery for zero-cloud, no-stutter, off-Google-servers use. (Google Photos / alternatives) https://www.androidpolice.com/found-google-photos-alternative-on-android/
- OnePlus users find the stock Gallery limited and debate switching but resist Google Photos as "intrusive," wanting a simple ROM-supported local viewer. (OnePlus Gallery) https://community.oneplus.com/threads/better-gallery-app.569955/

---

## Highest-signal opportunities for an open-source local-first gallery

Recurring across multiple apps and communities:

1. **Zero ads / recommendations / telemetry and no cloud-upsell nagging** (Xiaomi, OnePlus, Samsung).
2. **Proper recursive / nested folder browsing** — the single most cross-cutting request (Samsung, OnePlus, MIUI, Aves, Fossify, Immich).
3. **On-device face/people grouping and local content search** with no cloud — universally described as the thing missing from every offline gallery.
4. **Non-destructive editing** — never overwrite, save-copy by default; **no re-compression** on edit/move/share; **preserve all metadata**.
5. **Flexible, persistent sort** including "date taken" and manual/custom per-album order.
6. **Tags/keywords written to file metadata (XMP/EXIF)**, not a private DB.
7. **Fast, stable progressive thumbnails** that don't bloat storage or need constant regeneration.
8. **Power-user local tools stock apps lack** — EXIF/date/GPS editing, EXIF-based bulk rename, duplicate detection, real RAW support (with JPEG+RAW stacking), and a hidden folder that truly hides from other apps.
