# Standalone telemetry environment

The 2.0 prerelease removes `ClerkTelemetryEnvironment()` because the new SDK has
an explicitly retained core owner and no process-wide Clerk singleton. Standalone
telemetry callers must pass the existing five providers: SDK version, instance
type, telemetry consent, debug mode, and publishable key. Provider functions are
evaluated on each read so changes to consent and configuration take effect.

The core-backed SDK and native UI use the generated owner's `clerk.telemetry`.
They do not require this standalone module or a second telemetry collector.

The old no-argument constructor's binary-compatibility assertion is intentionally
retired for this major release. Tests now cover values from the explicitly supplied
owner, changing consent/configuration, and absent publishable keys. The provider
constructor remains available.
