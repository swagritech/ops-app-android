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
- Native Microsoft sign-in scaffold using OAuth2 (AppAuth)

## Next setup steps
1. Open in Android Studio (latest stable).
2. Let Android Studio create/update Gradle wrapper files if prompted.
3. Create `local.properties` with SDK path.
4. Build and run.

## Environment config
Default API base URL is set in `BuildConfig.API_BASE_URL` and can be overridden per build type.

## Current auth note
- Native OAuth sign-in flow is implemented in-app and stores access token in memory.
- Session persistence is implemented (access token + refresh token saved locally).
- App startup attempts token refresh and restores signed-in state.
- Dashboard includes explicit Sign Out action that clears local auth state.
- Flight logging screen now supports:
  - create flight submission to `create_flight`
  - offline queue persistence
  - manual queue sync
  - GPS capture (`Use Current GPS`) and Latitude/Longitude payload fields
- Redirect URI currently configured as `swatops://auth/callback`.
- You must register this redirect URI in Entra app registration for Android client testing.

