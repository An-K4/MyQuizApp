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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyQuizzApp"
include(":app")
include(":core:network")
include(":core:database")
include(":core:common")
include(":core:datastore")
include(":core:ui")
include(":feature:auth")
include(":feature:home")
include(":feature:lobby")
include(":feature:game-player")
include(":feature:game-host")
include(":feature:leaderboard")
include(":feature:quiz-manage")
