# Ktor is a compileOnly dependency that only the deprecated TelemetryCollector and TelemetryModule
# overloads use. R8 removes those overloads when nothing calls them, but an app whose keep rules
# retain them would otherwise fail with missing Ktor classes.
-dontwarn io.ktor.**
