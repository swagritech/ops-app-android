# Android MVP bootstrap

This repo is intentionally isolated from the current web app production setup.

## What is included
- Kotlin + Jetpack Compose app shell
- Core navigation (Login, Dashboard, Start Job, Flight Log, Reports)
- API layer scaffolding (Retrofit + OkHttp)
- Session handling scaffolding (DataStore)
- CI workflow scaffold
- Initial endpoint wiring:
  - `get_auth_identity`
  - `start_job`
  - `get_active_job`

## Next setup steps
1. Open in Android Studio (latest stable).
2. Let Android Studio create/update Gradle wrapper files if prompted.
3. Create `local.properties` with SDK path.
4. Build and run.

## Environment config
Default API base URL is set in `BuildConfig.API_BASE_URL` and can be overridden per build type.

## Current auth note
The backend uses Easy Auth session/cookies. Native token flow is not implemented yet in this commit, so identity verification depends on backend auth session behavior for mobile clients.

