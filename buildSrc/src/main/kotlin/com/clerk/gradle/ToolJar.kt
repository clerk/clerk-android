package com.clerk.gradle

/** A fat jar on Maven Central that a `config/bin` wrapper runs. */
data class ToolJar(val module: String, val classifier: String, val versionKey: String)

private val PRE_RELEASE =
  Regex(
    """(?i)[.-](alpha|beta|rc|cr|m|milestone|pre|preview|eap|dev|snapshot)([.-]?\d+)*([.-]|$)"""
  )

/** Whether [version] is an alpha, beta, release candidate, milestone or snapshot. */
fun isPreRelease(version: String): Boolean = PRE_RELEASE.containsMatchIn(version)

/**
 * The directory `config/bin/lib/tool-jar.sh` caches tool jars in. Like the shell's `${VAR:-…}`, it
 * treats a variable set to an empty string as unset. Where `HOME` is unset, as under `gradlew.bat`
 * from a Windows command prompt, it uses [userHome], which is where Git for Windows points `HOME`
 * for the hook.
 */
fun toolJarCacheDirectory(env: (String) -> String?, userHome: String): String =
  env("CLERK_TOOLS_CACHE").orEmpty().ifEmpty {
    val home = env("HOME").orEmpty().ifEmpty { userHome }
    val cacheHome = env("XDG_CACHE_HOME").orEmpty().ifEmpty { "$home/.cache" }
    "$cacheHome/clerk-android/tools"
  }
