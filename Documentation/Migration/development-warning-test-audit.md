# Development warning assertion audit

The three declarations in `ClerkDevelopmentModeWarningTest.kt` were checked against baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`, and the retained file's bytes matched that baseline before retirement. The old singleton property required `showDevModeWarning` and an instance type other than production. Its replacement belongs to native presentation, reading the generated environment fields. [The proof](evidence/development-warning/proof.json) preserves the original file hash, exact declarations and replacement results.

## Reproduced and corrected behavior

The migrated Android warning components checked the warning flag but omitted the production guard. The normal Clerk branding also hid whenever that flag was true. Two rendered regression cases failed: production on initial connection, and development changing to production through `environment.reload()`.

The test mounts the actual `DevelopmentModeWarningBox`, standalone `DevelopmentModeWarning`, and `SecuredByClerkView` in one test screen, backed by the packaged QuickJS core and fixture HTTP. Before the fix, both warning components display their labels in production and the normal branding is absent. The shared presentation check now requires a loaded owner, the warning flag, and an instance type other than `production`. All three components use that check.

Four rendered cases pass after the fix. They verify warning visibility, continued account-content visibility and normal branding. The reload case switches from development with warnings enabled to production with the flag still enabled, then development with warnings disabled; it checks the UI after each generated environment reload. Six screenshots record the resulting states. The iOS presentation extension already has the production guard and needs no change.

## Every legacy declaration

| Old declaration | Current owner and evidence |
| --- | --- |
| `shouldShowDevelopmentModeWarning returns true for development with warning enabled` | `developmentShowsRequestedWarning` requires both warning components to render and normal branding to remain hidden. |
| `shouldShowDevelopmentModeWarning returns false for production` | `productionNeverShowsDevelopmentWarning` requires no warning labels and visible normal branding, despite the flag being true. The reload case verifies the same result after a state update. |
| `shouldShowDevelopmentModeWarning returns false when warning disabled` | `disabledWarningPreservesNormalBranding` requires no warning labels and visible normal branding in development with the flag false. |

The obsolete singleton setter, DTO constructors and computed property are removed rather than restored as a domain facade. TypeScript continues to own environment hydration. This correction changes only Compose presentation and its tests.

## Test execution

`scripts/run-development-warning-journey.sh` executes the four exact named cases and preserves their XML and screenshots before the separate authentication journeys run. The CI artifact includes both sets. A combined class selector initially ran only the authentication class; the existing result verifier rejected that incomplete run rather than treating Gradle success as complete evidence.

The first test fixture did not compile because it called an API-internal JSON helper; using the JSON primitive accessor fixed compilation. Its first runtime response then omitted the client authorization credential and failed startup with `missing_client_credential`. The corrected fixture preserves that response credential in memory. Neither setup failure is counted as the reproduced presentation regression.

These rendered fixture checks do not establish live-service behavior, physical-device performance, or released-app upgrade continuity. Those broader migration gates remain open.

## CI evidence

Commit `e0b283d329664d659c2ef0901154e5b045312a50` passes all four jobs in [Android test CI](https://github.com/clerk/clerk-android/actions/runs/34544765216). Downloaded XML verifies 566 unit cases (39 API and 527 UI), two API 24 emulator cases, and 144 API 36 emulator cases without failures or skips. Those results include all four footer cases and all three authentication journeys. [The CI proof](evidence/development-warning/ci/proof.json) records 26 canonical instrumentation reports, unit-report hashes and six footer screenshots. The production and reload screenshots show normal branding without development labels; the enabled development case retains both tested warning components.

All three jobs in [build CI](https://github.com/clerk/clerk-android/actions/runs/34544766538) passed. Independent inspection of the two downloaded release AARs confirms identical bytes, SHA-256 `833e066b3c76432e180621bff174241bee0d98caf23e6e6e3749df42f184623e`, and the unchanged core bundle `406af90fdb32effc6fb3c1b35b52ceea639a68ceb3874b4d1390fd958a339f7d`. The UI build spent roughly ten minutes in JDK setup, then completed successfully without a retry.
