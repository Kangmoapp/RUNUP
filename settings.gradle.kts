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
        maven {
            url = uri("https://artifacts.objectbox.io/maven")
            content {
                includeGroup("io.objectbox") // ObjectBox 것만 허용
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://artifacts.objectbox.io/maven")
            content {
                includeGroup("io.objectbox") // ObjectBox 것만 허용
            }
        }
    }
}


rootProject.name = "RunUp"
include(":app")
 