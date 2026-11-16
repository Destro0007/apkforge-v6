# APKForge V6

Android app (Kotlin + Jetpack Compose) to generate apps with AI, edit code, build APKs in the cloud with GitHub Actions, and install them, all from the phone.

## Status
Fresh start. This repo contains only a minimal app shell that builds. Features are added one at a time; anything not built yet is labeled as unavailable in the app.

## Planned
- AI code generation/rewrite (OpenRouter, Gemini, Groq free models)
- Code editor
- Cloud build via GitHub Actions (GitHub OAuth Device Flow)
- APK signature verification and install

## Build
Push to `main`. The workflow in `.github/workflows/build-apk.yml` builds a debug APK and uploads it as an artifact.
