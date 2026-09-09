# Returned session state after expiry

The shared bridge now reconciles root ownership before encoding a returned resource. Previously a successful session reload that removed the selected session encoded its old handle and then invalidated that same handle in the completion snapshot. Generated Kotlin failed to decode the return value with `CoreException`.

An active/pending reload retains the canonical wrapper. If the session becomes expired or disappears, the owner clears its selected session and invalidates the original handle; the returned detached resource remains readable through its new handle. A newly listed different session is not silently adopted. This is shared runtime infrastructure, not a Kotlin authentication transition.

The real-core suite includes twelve initial-selection and generated-reload cases, and the full embedded suite passes 165 tests. The pre-fix packaged QuickJS test reproduces the exception; its post-fix contract checks the active, pending, and expired cases through generated APIs. The full Android packaged target passes six tests; the iOS contract target passes 49 using the same bundle and outcomes. Other response-ordering and signed-in upgrade audits remain separate.
