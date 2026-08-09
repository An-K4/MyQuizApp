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
