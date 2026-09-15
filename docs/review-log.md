# Review log

One entry per block of work done with a coding agent. Four fields each: what was asked, what the
agent produced, what was changed or rejected (and why), and how it was verified. Entries are written
by the reviewer, not the agent, and "accepted as-is after review" is a valid outcome.

## Block 0: environment and repository (2026-09-15)
- **Asked:** install the Android toolchain on Apple Silicon, create the public repo, and write the agent rules.
- **Produced:** Android Studio and command-line tools via Homebrew; SDK platform 36, build-tools 36.0.0, platform-tools, emulator, and an arm64 API 36 system image; AVD `marque_api36`; `AGENTS.md`, README stub, this log; public repo with secret scanning and push protection on.
- **Changed/rejected:** `avdmanager` from the Homebrew cask could not see the SDK's system images ("Package path is not valid"). Installed `cmdline-tools;latest` into the SDK and used that `avdmanager` instead.
- **Verified:** `emulator -list-avds` shows `marque_api36`; the emulator booted in about 50 s; `gh repo view` reports `PUBLIC`; the secret scanning API reports both features enabled.

## Block 1: skeleton (2026-09-15, PR #1)
- **Asked:** single-activity Compose + Material 3 app, Hilt injecting a placeholder repository into a ViewModel, ktlint, a version catalog with every version checked against its source, and CI on pull requests.
- **Produced:** AGP 9.4.0 (built-in Kotlin 2.4.20), Gradle 9.7.0, KSP, Compose BOM 2026.06.01, Hilt 2.60.1, ktlint-gradle 14.2.0; `HelloRepository` → `HelloViewModel` (StateFlow) → `HelloScreen`; `ci.yml` running ktlint, unit tests, and a debug build.
- **Changed/rejected:** The newest Compose, core, lifecycle, androidx.hilt, and navigation releases declare `minCompileSdk=37` and failed `checkDebugAarMetadata`. Kept compileSdk 36 and pinned each to its newest release that allows 36, confirmed by reading each AAR's metadata. The deprecated `hilt-navigation-compose` `hiltViewModel` was replaced with `hilt-lifecycle-viewmodel-compose`. Otherwise accepted after review.
- **Verified:** `ktlintCheck testDebugUnitTest assembleDebug` green locally and in CI; APK launched on the emulator showing the injected greeting.

## Blocks 2–3: network layer and offline-first cache (2026-09-15, PR #2)
- **Asked:** Retrofit + kotlinx.serialization against vPIC with timeouts, one retry for transient failures only, and HTTP/IO/serialization failures mapped to a sealed error; Room as the single source of truth observed through `Flow` into `stateIn`; a 24-hour cache policy; tests against MockWebServer and fakes; one DAO test on device.
- **Produced:** `DataError`, `Result`, `RetryPolicy`, `VpicRemoteDataSource`, `RoomVehicleLocalDataSource` (rows and refresh timestamp in one transaction), `OfflineFirstVehicleRepository`, Hilt modules, `MakesViewModel`/`MakesUiState`, and a plain makes screen. 29 JVM tests and 4 instrumented DAO tests.
- **Changed/rejected:** OkHttp held at 5.4.0 because 5.5.0 requires compileSdk 37. `kotlinx-coroutines-android` was added explicitly after the instrumented tests crashed with `NoSuchMethodError` on a mismatched coroutines version. The repository also turns cache-write exceptions into `DataError.Unknown`, so nothing escapes it. The reviewer kept the non-forced refresh on screen open: with a fresh cache and no connection the app correctly makes no request and shows no error, and that trade-off is documented in the README.
- **Verified:** Unit and instrumented tests green; CI green. On the emulator: 195 makes loaded online; airplane mode with the cache aged past 24 hours showed cached makes plus the error and exactly one retry in logcat; cleared data offline showed the error state; Retry after reconnecting loaded the list.

## Blocks 4–6: screens, tests, release workflow (2026-09-15, PR #3)
- **Asked:** list → detail with type-safe Compose Navigation; pull-to-refresh, search, and loading/empty/error/cached-with-error states; state that survives rotation and process death; ViewModel, mapping, and Compose UI tests; a release signing config fed only by environment variables and a tag-triggered workflow that publishes a verified, signed APK. Keystore, secrets, and tags were kept out of the agent's scope.
- **Produced:** `MarqueNavHost`, makes and models screens split into stateful route and stateless screen composables, a shared `RefreshTracker`, the search query in `SavedStateHandle`, `DataError` → string mapping, `release.yml`. 41 JVM tests and 9 instrumented tests (4 DAO, 5 Compose UI).
- **Changed/rejected:** `SavedStateHandle.toRoute()` needs an Android `Bundle`, which broke JVM tests. Instead of adding Robolectric, `RouteModule` decodes the route once in the ViewModel component and the ViewModel takes `ModelsRoute` as a constructor argument. Accepted after review.
- **Verified:** Tests green locally and in CI. On the emulator: navigation there and back; rotation kept the make, query, and scroll position; with "Don't keep activities" and with `am kill` in the background, the app came back to the same make; a never-cached make offline showed the error, and Retry after reconnecting loaded it. Locally, a throwaway keystore produced an APK that passed `apksigner verify` and installed; without the environment variables the release build stayed unsigned.

## Block 7: README (2026-09-15)
- **Asked:** an engineering README with an architecture diagram, three decisions with reasons, what was deliberately not built, and how to run the tests.
- **Produced:** Written by the reviewer. Screenshots taken on the API 36 emulator, including the offline banner in airplane mode.
- **Verified:** Each technical claim checked against the merged code (package layout, error mapping, retry rules, cache policy, saved state, test counts).
