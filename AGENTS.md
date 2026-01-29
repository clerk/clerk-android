# Repository Guidelines

## Project Structure & Module Organization
- Core SDK lives under `source/`: `api` (network/auth logic), `ui` (Jetpack Compose widgets bundling the API), and `telemetry` (shared analytics). Each module keeps the standard Gradle tree with `src/main`, `src/test`, and optional `src/androidTest`.
- Example integrations reside in `samples/*` (`quickstart`, `custom-flows`, `linear-clone`, `prebuilt-ui`) and can be installed independently to verify flows before release.
- Tooling and playground code sit in `workbench`, while repo-wide lint/static-analysis settings live in `config/`.

## Build, Test, and Development Commands
```bash
./gradlew build                     # Full build with unit tests for every module
./gradlew :source:api:publishToMavenLocal  # Verify Maven publishing metadata
./gradlew :samples:quickstart:installDebug # Deploy sample to a device/emulator
./gradlew spotlessCheck detekt lint # Formatting (ktfmt), static analysis, Android lint
./gradlew dokkaGenerateHtml         # Refresh docs/ from shared source sets
```
Keep `gradle.properties` publishable keys current before launching samples or snapshotting UI flows.

## Coding Style & Naming Conventions
- Kotlin + Compose everywhere: stick to ktfmt’s Google style (4-space indent, trailing commas). Run `./gradlew spotlessApply` whenever formatting drifts.
- Static checks rely on Detekt (`config/detekt/detekt.yml`) with all rules enabled, so avoid wildcard imports, prefer explicit visibility, and route errors through `ClerkResult` instead of exceptions.
- Package names mirror feature scopes (e.g., `com.clerk.api.sso`, `com.clerk.ui.signup`). Class names should describe intent (`SessionTokenFetcher`, `AuthStartViewModel`).

## Testing Guidelines
- Default unit tests run on the JVM with JUnit4, Robolectric, MockK, and kotlinx-coroutines-test—see `source/api/src/test/java/...`. Name files `*Test.kt` and mirror the package of the class under test.
- Compose snapshot tests rely on Paparazzi: `./gradlew :source:ui:testDebug` for verification, `recordPaparazziDebug` when updating golden images.
- Instrumented tests exist for modules that touch Android services; execute `./gradlew connectedAndroidTest` before publishing.
- Target ≥80% coverage on touched packages and add regression tests whenever altering auth flows, attestation, or persistence logic.

## Commit & Pull Request Guidelines
- Follow the existing Conventional Commits style (`chore(deps): …`, `fix: …`). Scope optional but encouraged (`feature(scope): summary`). Write bodies describing rationale and mention affected modules.
- Pull requests should state the problem, link GitHub issues, and attach emulator screenshots or Paparazzi diffs for UI tweaks. Mention migrations or new Gradle knobs explicitly.
- Before requesting review, run `spotlessCheck`, `detekt`, `lint`, relevant `test` tasks, and at least one sample install so reviewers can focus on logic instead of build failures.
