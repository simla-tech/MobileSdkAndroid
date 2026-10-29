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

val isolatedBuildDirectory = providers.gradleProperty("isolatedBuildDirectory")
gradle.beforeProject {
    isolatedBuildDirectory.orNull?.let { directory ->
        val projectDirectoryName = path.removePrefix(":").replace(':', '/').ifEmpty { "root" }
        layout.buildDirectory.set(file(directory).resolve(projectDirectoryName))
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SimlaConsultantAndroidSDK"

include(":sdk:core")
include(":sdk:core-okhttp")
include(":sdk:consultant")
include(":sdk:tracker")
include(":sdk:recommendations")
include(":sdk:push-firebase")
include(":sdk:bom")
