enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    // Protect is developed in its private monorepo and currently publishes only
    // to Maven local. Opt in explicitly while integrating it so a developer's
    // local artifact can never shadow a released dependency by default.
    if (providers.gradleProperty("clerkProtectMavenLocal").orNull == "true") {
      mavenLocal { content { includeGroup("com.clerk.protect") } }
    }
    google()
    mavenCentral()
  }
}

rootProject.name = "Clerk"

include(
  ":e2e",
  ":samples:quickstart",
  ":samples:custom-flows",
  ":samples:linear-clone",
  ":source:api",
  ":source:telemetry",
  ":source:ui",
  ":workbench",
)

include(":samples:prebuilt-ui")
