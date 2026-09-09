# Native core workbench

`WorkbenchApplication` retains one connected core and passes it to native screens
through `ClerkProvider`. UI activities supply the current platform presentation
context and route callbacks into that owner. Authentication completion includes
pending session tasks and the native presentation steps reported by the provider.

Configure a publishable key in Settings. The generated-core prerelease currently
does not support the old proxy URL option: a stored nonempty proxy produces an
explicit connection error. Clear that field to connect directly. Saving or clearing
configuration restarts the process, preventing reuse of an owner for an old instance.

The sample compiles against the new major. Live credentials, browser/passkey prompts,
and interactions still require device verification.
