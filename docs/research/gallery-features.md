# Android Gallery / Photos Apps — Master Feature Catalog

Reference for building an **open-source, local-first Android gallery**. This is the consolidated "what to build" list: it separates the **common baseline** (features nearly every gallery has — table stakes) from **standout/unique features** (niche differentiators worth stealing), and does the same for the **built-in photo editor**.

**Companion files in this folder:**
- **`gallery-features-and-implementation.md`** — per-app deep dives (Xiaomi, OnePlus, F-Stop, 1Gallery, Aves, Fossify, Google Photos, Samsung, self-hosted) **+ PART B code-side implementation notes** (MediaStore, ExifInterface, ML Kit, GPUImage, motion photos, trash, dedupe, etc.). Referenced inline below as → *impl §Bx*.
- **`gallery-user-frustrations.md`** — what users complain about and repeatedly ask for but don't get. The "opportunities" section there maps directly onto the ⭐ priorities flagged below.

> **Scope note:** Premium/generative AI editing (Magic Editor, Magic Eraser, AI Expansion, generative fill, Sky Replacement, cloud-only semantic search) is **deliberately excluded** from the "default" baseline per the brief. Where a feature *can* be done on-device without paid cloud (e.g. ML Kit labeling, on-device face clustering), it's included and flagged as **[on-device-doable]**.

---

# 1. COMMON FEATURES — the baseline (table stakes)

These appear in virtually every serious gallery (Google Photos, Samsung Gallery, MIUI, OnePlus, Fossify/Simple, Aves, Piktures, F-Stop). If your app lacks one of these, users will notice. Grouped by area.

### 1.1 Browsing & viewing
- [ ] **Photo/video grid** with adjustable column count (pinch-to-zoom to change density). *impl §B1–B2*
- [ ] **Timeline / chronological stream** grouped by Day / Month / Year, with sticky date headers.
- [ ] **Fast-scroll scrubber** on the edge to jump by month/year (users specifically hate when this is missing or too thin — see frustrations §16).
- [ ] **Fullscreen viewer** with swipe between items, pinch/double-tap zoom, **deep zoom** for high-res images. *impl §A6 (Fossify deep-zoom)*
- [ ] **Video playback** inline, with scrubber, mute, loop; PiP (picture-in-picture) is common. *impl §B8 (Media3/ExoPlayer)*
- [ ] **Motion/Live photo playback** (Pixel/Samsung/Apple motion photos play the embedded clip). *impl §B9*
- [ ] **GIF playback** (animated, autoplay in grid or on tap).
- [ ] **Landscape / rotation support**; **immersive mode** (hide system bars in viewer).

### 1.2 Organization
- [ ] **Albums** (user-created) and **folders** (filesystem-backed, one per directory — `BUCKET_ID`). *impl §B3*
- [ ] **Auto-albums**: Camera, Screenshots, Downloads, Videos, Favorites auto-populated.
- [ ] **Move / copy** items between folders (with SD-card support).
- [ ] **Create / rename / delete** albums; **pin favorite folders** to top.
- [ ] **Hide / exclude folders** from the main view (`.nomedia` for system-wide, or an app-level exclude list). *impl §B3, §B5*
- [ ] **Favorites** (star → auto-Favorites album).
- [ ] **Manual reorder** of folders/albums.

### 1.3 Search & filter
- [ ] **Search** by filename, date, folder/album, file type.
- [ ] **Search by location / places** (from EXIF GPS). *impl §B4*
- [ ] **[on-device-doable]** Search by content/objects and text-in-image — via ML Kit Image Labeling + Text Recognition (OCR) at ingest, indexed in SQLite FTS. *impl §B7*
- [ ] **Sort** by name / date taken / date modified / size / random, ascending or descending. **Persistent per-album sort** and a real **"date taken"** option are the two most-requested sort features (frustrations §5).
- [ ] **Filter** by type (photos / videos / GIFs / RAW), favorites, location-tagged, etc.

### 1.4 Deletion & recovery
- [ ] **Trash / Recycle Bin** with a retention window (~30 days), restore, empty-now. *impl §B12 (`createTrashRequest`, API 30+)*
- [ ] **Batch selection** → delete / move / share / favorite / hide.
- [ ] Clear "delete from device" vs "delete everywhere" semantics (a top Google Photos confusion — frustrations §14).

### 1.5 Privacy / vault
- [ ] **App lock** (PIN / pattern / fingerprint / biometric). *impl §A6*
- [ ] **Hidden / private album / vault** for sensitive media.
- [ ] **Granular lock checkpoints** (Fossify model): separate locks for *opening the app*, *viewing hidden items*, and *modifying files*. *impl §A6*
- [ ] A vault that **truly hides from other apps / the picker** — a recurring complaint that most don't (frustrations §13). Best done with encryption (see §2, 1Gallery) or a real `.nomedia`+MediaStore exclusion.

### 1.6 Metadata / EXIF
- [ ] **View EXIF/metadata**: camera model, lens, aperture, shutter, ISO, dimensions, file size, date taken, GPS. *impl §B4*
- [ ] **Map / Places view**: geotagged photos on an interactive map. *impl §B4 (ACCESS_MEDIA_LOCATION + setRequireOriginal)*
- [ ] **Edit basic metadata** (at minimum date/time; ideally GPS, orientation, title/description).

### 1.7 Sharing & output
- [ ] **Share** to other apps (single + batch) via the system share sheet.
- [ ] **"Set as" wallpaper / contact photo**; respond to **Edit / View / Set-As intents** from other apps. *impl §A6*
- [ ] **Usable as the system image picker** (respond to `ACTION_PICK` / `GET_CONTENT` / `ACTION_PICK_IMAGES`). *impl §B5*
- [ ] **Slideshow** with configurable interval, transition/animation, shuffle, loop. *impl §A6*
- [ ] **Optionally strip GPS/EXIF on share** (privacy toggle — Fossify, Piktures, MIUI do this). *impl §A6, §A1*

### 1.8 Backup / sync (optional but expected)
- [ ] Local-first design that **works fully offline, no account required** (the whole point vs Google Photos — frustrations §2, §17).
- [ ] Optional sync to a user-chosen backend (Nextcloud/WebDAV/S3) rather than a forced proprietary cloud.

### 1.9 Customization & theming
- [ ] **Material You / dynamic color**, light/dark/auto themes, custom accent. *impl §A6*
- [ ] Configurable grid density, toolbar actions, what appears on the main screen.
- [ ] **Widgets** (photo/album shortcut), app shortcuts.

---

# 2. STANDOUT / UNIQUE FEATURES — niche differentiators worth stealing

Features that only *some* apps have. These are how you stand out. Grouped by theme; the app that does it best is named. Full detail in *impl §A1–A9*.

### 2.1 Format & media breadth (Aves leads)
- **Exotic format support** — multi-page TIFF, SVG, old AVI, HEIC/HEIF, AVIF, **DNG RAW (with embedded-JPEG preview)**, JPEG XL. (Aves, Fossify) → a genuine differentiator; most galleries choke on these. *impl §B11 (RAW/LibRaw)*
- **Auto-detect & categorize special media**: motion photos, **panoramas / photo spheres** (with gyroscope 360° viewing), **360° videos**, **GeoTIFF**, screenshots. (Aves)
- **RAW + JPEG stacking** — show a RAW and its paired JPEG as one item, with a toggle to hide RAW clutter. (Requested everywhere, done almost nowhere — frustrations §12.)

### 2.2 Metadata-as-navigation (Aves + F-Stop)
- **Statistics / insights page** — counts by file type, storage used, per-country geotag counts, "most-used camera," "% shot in the evening." (Aves) — a delightful, cheap-to-build niche feature.
- **Countries / Places as browsable collections** via reverse geocoding. (Aves, Google Photos, Samsung)
- **Tag-centric browsing** — one photo carries many tags and appears in many tag "folders" without duplication. (F-Stop, Aves)
- **XMP / IPTC / XMP-sidecar read+write** — tags/ratings written to file metadata or a `.xmp` sidecar so they survive transfer to Lightroom/desktop (F-Stop Pro). The #1 metadata-portability request (frustrations §7).
- **Star ratings** (1–5) with a dedicated ratings view. (F-Stop, Aves)
- **Nested / hierarchical folders & albums** — navigate deep folder trees, nested smart albums. (F-Stop) — **the single most cross-cutting missing-feature request** (frustrations §4).
- **Smart albums (rule-based)** — auto-populating albums from criteria ("rating > 4 AND tag=Family AND date in 2024"). (F-Stop)

### 2.3 Browsing modes & UX gimmicks (Piktures)
- **Multiple view modes**: standard grid, **mosaic** (varied tile sizes), list, **calendar view**. (Piktures)
- **Unified sources view** — internal + SD + USB-OTG + Drive/Photos/OneDrive shown together in one timeline. (Piktures)
- **"On this day" / Rewind** — resurface past photos on their anniversary. (MIUI, Google, Nextcloud Memories)

### 2.4 Transfer & utility gimmicks
- **Built-in QR-code scanner** with a dedicated scanned-codes folder. (Piktures)
- **Wi-Fi Direct device-to-device transfer** between nearby users without mobile data. (Piktures)
- **Animated GIF maker** from a photo/video burst; **GIF player**. (Piktures, MIUI)
- **USB-OTG drive browsing** directly in-gallery. (Piktures)

### 2.5 Encryption-grade vault (1Gallery)
- **AES-encrypted vault** (not just move/hide) — files physically encrypted in a `.1Gallery`-style folder, optionally backed up encrypted. (1Gallery) *impl §A4*
- **Disguise / decoy lock modes** — Calculator disguise, PIN/pattern/fingerprint. (1Gallery)

### 2.6 On-device intelligence (no paid cloud) **[on-device-doable]**
- **Local face/people grouping** — detect faces (ML Kit) → embed (FaceNet/MobileFaceNet TFLite) → cluster (DBSCAN). Runs entirely offline. **Universally described as the one thing missing from every offline gallery** (frustrations §6). *impl §B6*
- **Natural-language / smart search offline** — "beach photos from July" via on-device labels + optional CLIP-style embeddings. (Focus Go demonstrates it; users beg for it in Fossify §6.) *impl §B7*
- **On-device OCR** — search text inside photos (receipts, screenshots, signs). *impl §B7*
- **Duplicate finder** — exact (hash) + near-dup (perceptual dHash/pHash) across storages. Requested, absent from all stock galleries (frustrations §8). *impl §B14*

### 2.7 Non-AI "everyday" features from the big OEMs worth matching
- **Locked Folder / Secure Folder / Private Album** (Google, Samsung) — a real vault excluded from grid, search, memories, and other apps.
- **Archive** — hide from main grid without deleting (Google).
- **Partner / shared albums** — collaborative albums shareable by link to non-users (Google, Samsung).
- **Auto-updating "person" albums**, near-duplicate grouping (Samsung).
- **Collage maker**, **Stories/Memories** auto-slideshows with music (all OEMs — algorithmic, not generative-AI).
- **Cinematic / 3D-motion stills** (Google) — algorithmic depth effect.

---

# 3. PHOTO EDITOR — common editing features (baseline)

Nearly every built-in editor (Google Photos, Samsung, MIUI, OnePlus, Piktures, Fossify, 1Gallery) ships these. Table stakes for your editor. Implementation for filters/adjustments → *impl §B10 (GPU), §B13 (non-destructive save)*.

### 3.1 Geometry / transform
- [ ] **Crop** (free + fixed aspect ratios: 1:1, 4:3, 16:9, etc.)
- [ ] **Rotate** (90° steps) and **Straighten** (fine angle dial)
- [ ] **Flip / Mirror** (horizontal + vertical)
- [ ] **Perspective correction** (keystone / horizontal + vertical skew)
- [ ] **Resize** (change output resolution)

### 3.2 Light & color adjustment sliders
- [ ] **Brightness**
- [ ] **Contrast**
- [ ] **Saturation**
- [ ] **Exposure**
- [ ] **Warmth / temperature** (and often **tint**)
- [ ] **Highlights / Shadows / White point / Black point**
- [ ] **Sharpness**
- [ ] **Vignette**
- [ ] **One-tap Auto-enhance** (magic-wand) — auto brightness/contrast/color.

### 3.3 Filters / presets
- [ ] A set of **preset filters / LUTs** (B&W, vintage, vivid, etc.), applied live on the GPU. *impl §B10 (android-gpuimage ships 120+)*
- [ ] Adjustable filter **intensity**.

### 3.4 Markup / annotation
- [ ] **Draw / pen** (freehand, color + brush size)
- [ ] **Highlighter**
- [ ] **Text** overlay (color; font choice is a differentiator — Google Photos lacks it, Samsung has it)
- [ ] **Eraser / undo / redo**

### 3.5 Output
- [ ] **Save as copy** (non-destructive default — never overwrite the original; the #1 editing complaint against Samsung — frustrations §10). *impl §B13*
- [ ] **Preserve EXIF/metadata** on save (decode→edit→re-encode strips it unless you copy tags over — Fossify's biggest editing complaint, frustrations §9). *impl §B13*
- [ ] **Video trimming** (cut start/end) at minimum.

---

# 4. PHOTO EDITOR — unique / advanced editing tools

Beyond the baseline. These distinguish a power-user editor. Most are **doable without paid AI** — the open-source **Litrato** and **Fossify** prove it (*impl §B10*).

### 4.1 Advanced non-AI tools (open-source-attainable)
- **Selective / masking brush** — paint a mask (adjustable brush size + opacity) to apply a filter/adjustment only to part of the image. (Litrato) *impl §B10*
- **Live RGB histogram** (log-scaled Y-axis) updating as you edit. (Litrato)
- **Color picker** — sample a hue directly from the image to drive an adjustment. (Litrato)
- **Curves** (tone curve) and **HSL** (per-color hue/saturation/luminance) — the pro color tools; not in most stock editors.
- **Edit-history slider** — non-destructive undo stack you can scrub back to any prior state. (Litrato)
- **Contour / stylize filters** — Laplacian, Sobel edge-detect, Sketch, Cartoon (pure shader/convolution, no AI). (Litrato) *impl §B10*
- **Spot / blemish healing** and **red-eye removal** (classic, non-generative).
- **Blur tools** — Gaussian/box/lens blur, tilt-shift, background blur (bokeh via depth or manual mask). *impl §B10*
- **Custom GLSL/AGSL shader filters** — let the app (or advanced users) define fragment-shader effects. *impl §B10 (AGSL, API 33+)*

### 4.2 Creative extras seen in OEM editors
- **Stickers / emoji** overlay (Samsung, MIUI — Google Photos notably lacks stickers).
- **Doodle** (Samsung).
- **Object cut-out / lasso** — isolate a subject to make a sticker or composite (Samsung lasso; MIUI "Photo Cutout"). *Note: some OEM cut-outs are AI-backed — a simple manual/GrabCut version is non-AI.*
- **Collage maker** (grid layouts, per-cell edit, shuffle).
- **Frames / borders**, **art filters** (Low Poly, Etching, Frosted Glass — MIUI).
- **Pro video editor** — multi-clip stitch, speed ramp, music, text overlays, per-clip brightness/effects (MIUI, OnePlus). *Warning: re-encoding degrades quality — a real Samsung complaint (frustrations §10); preserve quality / offer lossless trim.*
- **Markup font selection** and **shape tools** (arrows, rectangles) for annotation.

### 4.3 Deliberately EXCLUDED (paid / generative AI — out of scope)
Magic Editor / Magic Eraser (Google), AI Eraser / AI Expansion / AI Sky Replacement / AI Unblur / Reflection Removal (MIUI, OnePlus), generative fill/outpaint. Listed only so you know what the commercial galleries market — **not part of the open-source baseline**, and several are buggy and cloud-gated anyway (frustrations §10).

---

# 5. Recommended build priorities for an open-source gallery

Synthesized from the **frustrations** file (what people beg for) crossed with feasibility. ⭐ = high user demand + differentiates from stock apps.

1. ⭐ **Zero ads, zero telemetry, no cloud-upsell** — the entire reason people flee MIUI/Samsung/Google (frustrations §1, §2, §17). This is a positioning win, essentially free.
2. ⭐ **Recursive / nested folder browsing** — the single most cross-cutting request across Samsung, OnePlus, MIUI, Aves, Fossify, Immich (frustrations §4).
3. ⭐ **Non-destructive editing** — never overwrite; save-copy by default; **no re-compression** on edit/move/share; **preserve all metadata** (frustrations §9, §10). *impl §B13*
4. ⭐ **On-device face/people grouping + local content/OCR search**, no cloud (frustrations §6). *impl §B6, §B7*
5. ⭐ **Flexible, persistent sort** incl. true "date taken" and manual per-album order (frustrations §5).
6. **Tags/keywords written to file XMP/EXIF** (or sidecar), not a private DB (frustrations §7). *impl §B4*
7. **Fast, stable, progressive thumbnails** that don't bloat storage or need constant regeneration (frustrations §15). *impl §B1, §B2*
8. **Power-user tools stock apps lack** — EXIF/date/GPS editing, EXIF-based bulk rename, duplicate detection, real RAW with JPEG+RAW stacking, and a vault that truly hides from other apps (frustrations §8, §12, §13). *impl §B11, §B14*
9. **Niche delight features** cheap to build: statistics page, calendar/mosaic view modes, QR scanner, GIF maker, map view (§2).

---

## Source basis

The common/unique feature and editor claims were gathered and **adversarially fact-checked** (3-vote verification, 25/25 confirmed) by the deep-research workflow, and expanded by two focused research passes. Primary sources include the official app repos/help (Aves `deckerst/aves`, Fossify `FossifyOrg/Gallery`, Litrato `DrMint/Litrato`, `help.piktures.app`, Google Photos Help, Samsung support, F-Stop `fstopapp.com`), `developer.android.com` for all code-side claims, and community threads for the frustrations. Full per-claim sources are inline in the two companion files. Vendor feature sets change across OS/app versions; verify current behavior before implementing.
