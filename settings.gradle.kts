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
        google()
        mavenCentral()
    }
}

rootProject.name = "bianwanlu2_0"
include(":app")
include(":core:common")
include(":core:model")
include(":core:data")
include(":core:ui")
include(":feature:notes")
include(":feature:todo")
include(":feature:category")
include(":feature:calendar")
include(":feature:timeline")
 

