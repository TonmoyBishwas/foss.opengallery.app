# Contributing to OpenGallery

Thanks for your interest!

## Ground rules

- **No ads, no telemetry, no cloud dependencies, no API keys.** PRs introducing any of these will be declined.
- Keep it lightweight: think twice before adding a dependency; prefer platform APIs.
- The visual language is the app's identity — new screens should reuse the components in `ui/components/` and tokens in `ui/theme/`.
- All assets must be original or appropriately licensed (no assets ripped from other apps).

## Workflow

1. Open an issue describing the bug/feature first for anything non-trivial.
2. Fork, branch from `main`, keep PRs focused.
3. `./gradlew assembleDebug test` must pass.
4. By contributing you agree your work is licensed under GPL-3.0.

## Code style

Standard Kotlin style (`kotlin.code.style=official`). Compose-first UI, MVVM with Flow, no reflection-based DI.
