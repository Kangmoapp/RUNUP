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

        // --- 네이버 지도 저장소 추가 ---
        maven {
            url = uri("https://repository.map.naver.com/archive/maven")
        }
    }
}


rootProject.name = "RunUp"
include(":app")
 