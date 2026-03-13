# Android MVP bootstrap

This repo is intentionally isolated from the current web app production setup.

## What is included
- Kotlin + Jetpack Compose app shell
- Core navigation (Login, Dashboard, Start Job, Flight Log, Reports)
- API layer scaffolding (Retrofit + OkHttp)
- Session handling scaffolding (DataStore)
- CI workflow scaffold

## Next setup steps
1. Open in Android Studio (latest stable).
2. Let Android Studio create/update Gradle wrapper files if prompted.
3. Create `local.properties` with SDK path.
4. Build and run.

## Environment config
Default API base URL is set in `BuildConfig.API_BASE_URL` and can be overridden per build type.

