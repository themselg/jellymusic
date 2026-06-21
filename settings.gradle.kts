pluginManagement {
    includeBuild("build-logic")
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
        google()
        mavenCentral()
        // Jellyfin SDK snapshots / releases live on Maven Central; this is here only
        // in case you want to use unstable SDK builds.
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "JellyMusic"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:player")
include(":core:ui")
include(":feature:login")
include(":feature:home")
include(":feature:search")
include(":feature:profile")
include(":feature:detail")
include(":feature:player")
include(":feature:library")
