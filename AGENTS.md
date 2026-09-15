# Working with coding agents in this repo

Rules for any coding agent used in this repo and for the person directing it.

1. **One block per branch.** Branch names like `block/2-network` or `chore/ci`. `main` changes only
   through pull requests with CI green (once CI exists).
2. **Stay inside the block.** Each block has a scope and a done-when in its task. Anything outside it
   is reported, not built. The "not built" list in the README is deliberate; do not add those
   features.
3. **Verify library APIs against current documentation; don't guess.** Especially AGP, the Kotlin
   and KSP versions, the Compose BOM, Hilt, Room, Navigation, and Retrofit's kotlinx.serialization
   converter. All versions live in `gradle/libs.versions.toml`.
4. **Architecture is fixed:** single activity, Compose + Material 3, UI → ViewModel (`StateFlow`) →
   repository → Retrofit + Room. The Room database is the single source of truth; the network only
   refreshes it; the UI observes `Flow` from the DAO. Kotlin `Flow` and coroutines only, no RxJava.
5. **No secrets anywhere.** Keystores, passwords, and `keystore.properties` never enter git, prompts,
   or logs. CI signing reads GitHub Actions secrets.
6. **Tests and lint pass before a PR opens:** `./gradlew ktlintCheck test assembleDebug`.
   Instrumented tests (`connectedAndroidTest`) run locally on an emulator and are reported in the PR.
7. **Every generated diff is read before merge.** Review happens in git, not in chat.
8. **Keep the review log honest.** `docs/review-log.md` records per block what was asked, what was
   produced, what was changed or rejected, and how it was verified. "Accepted as-is after review" is a
   valid entry; invented corrections are not.
9. **No overstatement in docs.** Describe what the code does and what it does not do. No claims of
   users, production readiness, or scale.
