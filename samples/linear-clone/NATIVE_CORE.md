# Generated-core authentication sample

The application retains one connected `Clerk` and injects it into navigation-owned
view models and `ClerkProvider`. Google SSO uses the core's transferable flow;
email verification and discoverable passkeys use generated future-style methods.
Completed attempts explicitly call `finalize()`. Remaining requirements and pending
session tasks open the native authentication UI against the same owner.

Failures appear in a shared dialog and preserve the form for retry. Platform
presenters use the active activity; cold/warm callbacks route into the retained core.
Screen previews render their stateless content without connecting to an account.

The sample requires compile SDK 37 for its navigation/lifecycle dependencies.
Its target and minimum SDK settings follow the repository defaults. Building the
sample does not establish that live OAuth/passkey flows have passed device testing.
