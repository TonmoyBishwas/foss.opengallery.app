# Philosophy

What OpenGallery is, and — just as importantly — what it refuses to become. Every
feature decision should be checkable against this page.

## 1. Local-first, forever

Photos never leave the device. No account, no cloud, no telemetry, no ads, no API
keys, no network permission beyond what the OS map tiles need. Features that would
require a server (shared albums, cloud sync, generative AI edits) are **out of scope
by design**, not "coming later". If a proposed feature needs a privacy policy
paragraph to explain, it doesn't ship.

This is also the app's single biggest selling point on the Play Store: the Data
safety form can truthfully say *no data collected, no data shared*.

## 2. Flagship polish for every phone

Most people's mental model of "a gallery" is their OEM gallery. OpenGallery
recreates the layout, spacing, and interaction patterns of a modern flagship gallery
(One UI-style, dark-first) so it feels instantly familiar — on any Android 8.0+
device, including ones whose stock gallery is ad-riddled or abandoned.

## 3. Legally clean recreation

We recreate *patterns*, never *assets*. Every icon is original hand-drawn Canvas
code (`ui/components/OgIcons.kt`); fonts are system Roboto; no Samsung or One UI
name, icon, or artwork appears anywhere in the app or its store listing. This is
what makes the look-alike approach Play-Store-safe and lawsuit-proof — protect it
in every PR.

## 4. Never destroy user data

- Edits always save a copy; EXIF (including GPS and date taken) is preserved.
- Deletes go to a recycle bin with 30-day retention, on every supported API level.
- The Locked Folder hides files but the user can always move them back out.
- Destructive MediaStore operations always go through the system consent dialog.

A gallery holds people's most irreplaceable data. One bug that eats a photo costs
more trust than every feature combined.

## 5. Lightweight is a feature

Single module, hand-rolled DI (no Hilt), no Firebase, no analytics SDK, R8 full
shrink, ~13 MB arm64 APK. Every new dependency must justify its bytes. Heavy
ML runs on-device, incrementally, via WorkManager — never blocking the UI and never
in the cloud.

## 6. Free as in freedom

GPL-3.0: anyone can fork it, but forks must stay open. The research that drove the
feature set (`docs/research/`) came from real user complaints about stock galleries —
the roadmap should keep answering those, not chase trends.
